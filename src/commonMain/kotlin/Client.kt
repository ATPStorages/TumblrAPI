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
    val response: T,
    val errors: Set<ResponseError>? = null
)
// * // * // * // * // * // * // * // * // * // * //
abstract class BaseClient(
    engine: HttpClientEngineFactory<HttpClientEngineConfig>,
    consumerKey: String,
    //secretKey: String? = null,
    oauthToken: String? = null
) {
    private val client = HttpClient(engine) {
        install(ContentNegotiation) { json(json) }
        install(HttpRequestRetry) {
            retryIf(maxRetries = 5) { _, httpResponse ->
                when(httpResponse.status.value) {
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
                    // should we have an oauth client that inherits from base or have a standalone that does the job?
                    if(oauthToken != null) append(HttpHeaders.Authorization, "Bearer $oauthToken")
                }
            }
        }
    }

    // * // * // * // * // * // * // * // * // * // * //

    suspend fun blogAvatar(blog: String, size: BlogAvatarSize = BlogAvatarSize.SQUARE_512) =
        Url(client.get { url { appendPathSegments(version, "blog", blog, "avatar", size.px.toString()) } }.let {
            if(it.headers[HttpHeaders.Location] != null) it.headers[HttpHeaders.Location]!!
            else it.body<Response<AvatarResponseObject>>().response.avatar_url
        })

    suspend fun blogInfo(blog: String) = // TODO optional stuff
        client.get { url { appendPathSegments(version, "blog", blog, "info") } }.body<Blog>()

    // * // * // * // * // * // * // * // * // * // * //

    // filter: Filter what types to include.
    // filterStrict: Posts MUST have only the types specified in filter.
    // TODO: javadoc
    
    suspend fun blogPosts(
        blog: String,
        vararg tags: String,
        
    )
    
    //suspend fun blogPost()

    private suspend fun blogLikes(
        blog: String,
        before: Long? = null,
        offset: Int? = null,
        after: Long? = null,
        limit: Byte = 20,
        vararg filters: PostContentType,
        filterStrict: Boolean = false
    ) = this.client.get { url {
        appendPathSegments(version, "blog", blog, "likes")
        if(offset != null) parameters.append("offset", offset.toString())
        if(before != null) parameters.append("before", before.toString())
        if(after != null) parameters.append("after", after.toString())
        parameters.append("limit", limit.toString())
    } }.body<Response<LikedBlogPostsResponse>>().let { api ->
        Response(
            api.status,
            LikedBlogPostsResponse(
                api.response.posts.let { if(filters.isNotEmpty()) it.filterContent(filterStrict, *filters) else it },
                api.response.totalLiked
            ),
            api.errors
        )
    }

    suspend fun blogLikesBefore(
        blog: String,
        before: Long,
        limit: Byte = 20,
        vararg filters: PostContentType,
        filterStrict: Boolean = false
    ) = blogLikes(blog, before, null, null, limit, filterStrict, *filters)

    suspend fun blogLikesAfter(
        blog: String,
        after: Long,
        limit: Byte = 20,
        vararg filters: PostContentType,
        filterStrict: Boolean = false
    ) = blogLikes(blog, null, null, after, limit, filterStrict, *filters)

    suspend fun blogLikes(
        blog: String,
        limit: Byte = 20,
        offset: Int? = null,
        vararg filters: PostContentType,
        filterStrict: Boolean = false
    ) = blogLikes(blog, null, offset, null, limit, filterStrict, *filters)

    suspend fun readTag(
        tag: String,
        before: Long? = null,
        after: Long? = null,
        limit: Byte = 20,
        vararg filters: PostContentType,
        filterStrict: Boolean = false
    ) = this.client.get { url {
        appendPathSegments(version, "tagged")
        parameters.append("tag", tag)
        parameters.append("limit", limit.toString())
        if(before != null) parameters.append("before", before.toString())
    } }.body<Response<Set<Post>>>().let { postSet ->
        Response(
            postSet.status,
            postSet.response
                .let { if(filters.isNotEmpty()) it.filterContent(filterStrict, *filters) else it }
                .let { if(after != null) it.filter { it.timestamp > after }.toSet() else it },
            postSet.errors
        )
    }
}

// TODO: class OAuthClient(...) refer to ln 73

expect class Client(consumerKey: String): BaseClient
