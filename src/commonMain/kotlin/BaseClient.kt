package me.atpstorages.tumblr_api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// * // * // * // * // * // * // * // * // * // * //
val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

const val version = "v2"
// * // * // * // * // * // * // * // * // * // * //
@Serializable
class ResponseStatus(
    @SerialName("status") val code: Short,
    @SerialName("msg") val message: String
)

@Serializable
class ResponseError(
    @SerialName("title") val name: String,
    @SerialName("code") val subcode: Short,
    @SerialName("detail") val description: String
)

@Serializable
class Response<T>(
    @SerialName("meta") val status: ResponseStatus,
    @SerialName("response") val contents: T,
    private val errors: Set<ResponseError>? = null,
)

// * // * // * // * // * // * // * // * // * // * //
abstract class BaseClient internal constructor(
    engine: HttpClientEngineFactory<HttpClientEngineConfig>,
    consumerKey: String,
) {
    protected var oauth: CodeGrantWithTimestamp? = null
    protected val client = HttpClient(engine) {
        install(ContentNegotiation) { json(json) }
        install(HttpRequestRetry) {
            retryIf(maxRetries = 5) { _, httpResponse ->
                when (httpResponse.status.value) {
                    429, 503, 500 -> true
                    else -> false
                }
            }
            exponentialDelay()
        }

        followRedirects = false

        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
                host = "api.tumblr.com"

                parameters.append("npf", "true")
                parameters.append("api_key", consumerKey)

                headers {
                    append(HttpHeaders.UserAgent, "KTAPI:atp 1.0.0")
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    oauth.let { if (it != null) append(HttpHeaders.Authorization, "Bearer ${it.accessToken}") }
                }
            }
        }
    }

    // * // * // * // * // * // * // * // * // * // * //

    suspend fun blogAvatar(blog: String, size: BlogAvatarSize = BlogAvatarSize.SQUARE_512) =
        Url(client.get { url { appendPathSegments(version, "blog", blog, "avatar", size.px.toString()) } }.let {
            if (it.headers[HttpHeaders.Location] != null) it.headers[HttpHeaders.Location]!!
            else it.body<Response<BlogAvatarObject>>().contents.avatar_url
        })

    suspend fun blogAvatar(blog: Blog, size: BlogAvatarSize = BlogAvatarSize.SQUARE_512) = blogAvatar(blog.name, size)

    suspend fun blogInfo(blog: String) = // TODO optional stuff
        client.get { url { appendPathSegments(version, "blog", blog, "info") } }.body<Blog>()

    suspend fun blogInfo(blog: Blog) = blogInfo(blog.name)

    enum class PostNotesMode {
        ALL,
        LIKES,
        CONVERSATION,
        ROLLUP,
        REBLOGS_WITH_TAGS;
    }

    suspend fun blogPostNotes(
        blog: String,
        id: Long,
        before: Long? = null,
        mode: PostNotesMode = PostNotesMode.ALL,
    ): BlogPostNotes {
        val api = this.client.get {
            url {
                appendPathSegments(version, "blog", blog, "notes")
                if (before != null) parameters.append("before_timestamp", before.toString())

                parameters.append("mode", mode.name.lowercase())
                parameters.append("id", id.toString())
            }
        }

        return when (mode) {
            PostNotesMode.CONVERSATION -> api.body<Response<BlogPostNotesConversation>>()
            PostNotesMode.REBLOGS_WITH_TAGS -> api.body<Response<BlogPostNotesWithTags>>()
            else -> api.body<Response<BlogPostNotesNormal>>()
        }.contents
    }

    suspend fun blogPostNotes(
        blog: Blog,
        id: Long,
        before: Long? = null,
        mode: PostNotesMode = PostNotesMode.ALL,
    ) = blogPostNotes(blog.name, id, before, mode)

    // * // * // * // * // * // * // * // * // * // * //

    // filter: Filter what types to include.
    // filterStrict: Posts MUST have only the types specified in filter.
    // TODO: javadoc

    enum class PostTextReturnFilter {
        RAW,
        TEXT,
        HTML;
    }

    suspend fun blogPosts(
        blog: String,
        limit: Byte = 20,
        before: Long? = null,
        after: Long? = null,
        offset: Int = 0,
        textFilter: PostTextReturnFilter = PostTextReturnFilter.HTML,
        tags: Set<String>? = null,
        vararg filters: PostContentType,
        filterStrict: Boolean = false,
    ) = this.client.get {
        url {
            appendPathSegments(version, "blog", blog, "posts")
            if (textFilter != PostTextReturnFilter.HTML) parameters.append("filter", textFilter.name.lowercase())
            if (before != null) parameters.append("before", before.toString())

            parameters.append("offset", offset.toString())
            parameters.append("limit", limit.toString())
            tags?.forEachIndexed { idx, tag -> parameters.append("tag[$idx]", tag) }
        }
    }.body<Response<BlogPosts>>().contents.posts
        .let { if (filters.isNotEmpty()) it.filterContent(filterStrict, *filters) else it }
        .let { if (after != null) it.filter { post -> post.timestamp > after }.toSet() else it }

    suspend fun blogPosts(
        blog: String,
        limit: Byte = 20,
        before: Long? = null,
        after: Long? = null,
        offset: Int = 0,
        textFilter: PostTextReturnFilter = PostTextReturnFilter.HTML,
        tag: String,
        vararg filters: PostContentType,
        filterStrict: Boolean = false,
    ) = blogPosts(blog, limit, before, after, offset, textFilter, setOf(tag), *filters, filterStrict = filterStrict)

    suspend fun blogPosts(
        blog: Blog,
        limit: Byte = 20,
        before: Long? = null,
        after: Long? = null,
        offset: Int = 0,
        textFilter: PostTextReturnFilter = PostTextReturnFilter.HTML,
        tag: String,
        vararg filters: PostContentType,
        filterStrict: Boolean = false,
    ) = blogPosts(
        blog.name,
        limit,
        before,
        after,
        offset,
        textFilter,
        setOf(tag),
        *filters,
        filterStrict = filterStrict
    )

    suspend fun blogPost(
        blog: String,
        id: Long,
        textFilter: PostTextReturnFilter = PostTextReturnFilter.HTML,
    ) = this.client.get {
        url {
            appendPathSegments(version, "blog", blog, "posts")
            if (textFilter != PostTextReturnFilter.HTML) parameters.append("filter", textFilter.name.lowercase())
            parameters.append("id", id.toString())
        }
    }.body<Response<Set<Post>>>().contents.first()

    suspend fun blogPost(
        blog: Blog,
        id: Long,
        textFilter: PostTextReturnFilter = PostTextReturnFilter.HTML,
    ) = blogPost(blog.name, id, textFilter)

    private suspend fun blogLikes(
        blog: String,
        before: Long? = null,
        offset: Int? = null,
        after: Long? = null,
        limit: Byte = 20,
        vararg filters: PostContentType,
        filterStrict: Boolean = false,
    ) = this.client.get {
        url {
            appendPathSegments(version, "blog", blog, "likes")
            if (offset != null) parameters.append("offset", offset.toString())
            if (before != null) parameters.append("before", before.toString())
            if (after != null) parameters.append("after", after.toString())
            parameters.append("limit", limit.toString())
        }
    }.body<Response<LikedBlogPosts>>().let { response ->
        LikedBlogPosts(
            response.contents.posts.let { if (filters.isNotEmpty()) it.filterContent(filterStrict, *filters) else it },
            response.contents.totalLiked
        )
    }

    suspend fun blogLikesBefore(
        blog: String,
        before: Long,
        limit: Byte = 20,
        vararg filters: PostContentType,
        filterStrict: Boolean = false,
    ) = blogLikes(blog, before, null, null, limit, *filters, filterStrict = filterStrict)

    suspend fun blogLikesBefore(
        blog: Blog,
        before: Long,
        limit: Byte = 20,
        vararg filters: PostContentType,
        filterStrict: Boolean = false,
    ) = blogLikes(blog.name, before, null, null, limit, *filters, filterStrict = filterStrict)

    suspend fun blogLikesAfter(
        blog: String,
        after: Long,
        limit: Byte = 20,
        vararg filters: PostContentType,
        filterStrict: Boolean = false,
    ) = blogLikes(blog, null, null, after, limit, *filters, filterStrict = filterStrict)

    suspend fun blogLikesAfter(
        blog: Blog,
        after: Long,
        limit: Byte = 20,
        vararg filters: PostContentType,
        filterStrict: Boolean = false,
    ) = blogLikes(blog.name, null, null, after, limit, *filters, filterStrict = filterStrict)

    suspend fun blogLikes(
        blog: String,
        limit: Byte = 20,
        offset: Int? = null,
        vararg filters: PostContentType,
        filterStrict: Boolean = false,
    ) = blogLikes(blog, null, offset, null, limit, *filters, filterStrict = filterStrict)

    suspend fun blogLikes(
        blog: Blog,
        limit: Byte = 20,
        offset: Int? = null,
        vararg filters: PostContentType,
        filterStrict: Boolean = false,
    ) = blogLikes(blog.name, limit, offset, *filters, filterStrict = filterStrict)

    suspend fun readTag(
        tag: String,
        before: Long? = null,
        after: Long? = null,
        limit: Byte = 20,
        vararg filters: PostContentType,
        filterStrict: Boolean = false,
    ) = this.client.get {
        url {
            appendPathSegments(version, "tagged")
            parameters.append("tag", tag)
            parameters.append("limit", limit.toString())
            if (before != null) parameters.append("before", before.toString())
        }
    }.body<Response<Set<Post>>>().contents
        .let { if (filters.isNotEmpty()) it.filterContent(filterStrict, *filters) else it }
        .let { if (after != null) it.filter { post -> post.timestamp > after }.toSet() else it }
}

// TODO: handle api errors (probably through a protected function)

expect class Client(consumerKey: String): BaseClient