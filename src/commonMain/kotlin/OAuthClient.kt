package me.atpstorages.tumblr_api

import com.javiersc.kotlinx.coroutines.run.blocking.runBlocking
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// * // * // * // * // * // * // * // * // * // * //
class OAuthInitializationException(message: String) : Exception(message)
class OAuthRefreshException(message: String) : Exception(message)

// * // * // * // * // * // * // * // * // * // * //
abstract class OAuthClientBase(
    engine: HttpClientEngineFactory<HttpClientEngineConfig>,
    private val consumerKey: String,
    private val consumerSecret: String,
    authorizationCode: String,
    redirectUri: String?,
) : BaseClient(engine, consumerKey) {
    init {
        oauth = runBlocking {
            try {
                client.post("https://api.tumblr.com/v2/oauth2/token/") {
                    setBody(buildString {
                        append(
                            "grant_type=authorization_code&" +
                                    "code=$authorizationCode&" +
                                    "client_id=$consumerKey&" +
                                    "client_secret=$consumerSecret"
                        )
                        if (!redirectUri.isNullOrEmpty()) append("&redirect_uri=$redirectUri")
                    })

                    contentType(ContentType.Application.FormUrlEncoded)
                }.let {
                    if (it.status != HttpStatusCode.OK) {
                        throw Exception(
                            "Returned not OK result. ${it.status.value}: ${it.status.description} \"${it.bodyAsText()}\""
                        )
                    } else it.body<CodeGrant>().withTimestamp()
                }
            } catch (e: Exception) {
                throw OAuthInitializationException("Failed to get required credentials from code grant. ${e.message}")
            }
        }
    }

    // X-Ratelimit-Perday-Limit=[5000]
    // X-Ratelimit-Perday-Remaining=[4906]
    // X-Ratelimit-Perday-Reset=[76560]
    // X-Ratelimit-Perhour-Limit=[1000]
    // X-Ratelimit-Perhour-Remaining=[917]
    // X-Ratelimit-Perhour-Reset=[3263]

    @Serializable
    class RefreshTokenBody(
        @SerialName("client_id") val consumerKey: String,
        @SerialName("client_secret") val consumerSecret: String,
        @SerialName("refresh_token") val refreshToken: String,
        val grant_type: String,
    )

    private suspend inline fun HttpClient.requestAndCheck(builder: HttpRequestBuilder): HttpResponse {
        oauth.let {
            if (it != null && epoch() > it.expiresAt) {
                if (it.scope.contains("offline_access")) {
                    println("refreshing!")
                    oauth = try {
                        client.post("https://api.tumblr.com/v2/oauth2/token") {
                            contentType(ContentType.Application.Json)
                            setBody(RefreshTokenBody(consumerKey, consumerSecret, it.refreshToken!!, "refresh_token"))
                        }.let { res ->
                            if (res.status != HttpStatusCode.OK) {
                                throw Exception(
                                    "Returned not OK result. ${res.status.value}: ${res.status.description} \"${res.bodyAsText()}\""
                                )
                            } else res.body<CodeGrant>().withTimestamp()
                        }
                    } catch (e: Exception) {
                        throw OAuthRefreshException("Failed to get refresh credentials. ${e.message}")
                    }
                    println("refreshed: \n new key: ${it.accessToken}")
                } else throw OAuthRefreshException("/oath2/authorize scopes must include offline_access to refresh tokens")
            }
        }

        return HttpStatement(builder, this).execute()
    }

    // * // * // * // * // * // * // * // * // * // * //

    private suspend inline fun HttpClient.getAndCheck(block: HttpRequestBuilder.() -> Unit): HttpResponse {
        val builder = HttpRequestBuilder().apply(block)
        builder.method = HttpMethod.Get
        return this.requestAndCheck(builder)
    }

    private suspend inline fun HttpClient.postAndCheck(block: HttpRequestBuilder.() -> Unit): HttpResponse {
        val builder = HttpRequestBuilder().apply(block)
        builder.method = HttpMethod.Post
        return this.requestAndCheck(builder)
    }

    private suspend inline fun HttpClient.deleteAndCheck(block: HttpRequestBuilder.() -> Unit): HttpResponse {
        val builder = HttpRequestBuilder().apply(block)
        builder.method = HttpMethod.Delete
        return this.requestAndCheck(builder)
    }

    // * // * // * // * // * // * // * // * // * // * //

    @Serializable
    class BlogBlocks(@SerialName("blocked_tumblelogs") val blockedBlogs: Set<Blog>)

    suspend fun blogBlocks(blog: String, offset: Int = 0, limit: Byte = 20) = client.getAndCheck {
        url {
            appendPathSegments(version, "blog", blog, "blocks")
            parameters.append("offset", offset.toString())
            parameters.append("limit", limit.toString())
        }
    }.body<Response<BlogBlocks>>().contents.blockedBlogs

    suspend fun blogBlocks(blog: Blog, offset: Int = 0, limit: Byte = 20) = blogBlocks(blog.uuid, offset, limit)

    // * // * // * // * // * // * // * // * // * // * //

    suspend fun addBlogBlock(blog: String, blockBlog: String) {
        client.postAndCheck {
            url {
                appendPathSegments(version, "blog", blog, "blocks")
                parameters.append("blocked_tumblelog", blockBlog)
            }
        }
    }

    suspend fun addBlogBlock(blog: Blog, blockBlog: Blog) = addBlogBlock(blog.uuid, blockBlog.uuid)

    suspend fun addBlogBlock(blog: String, postId: Long) {
        client.postAndCheck {
            url {
                appendPathSegments(version, "blog", blog, "blocks")
                parameters.append("post_id", postId.toString())
            }
        }
    }

    suspend fun addBlogBlock(blog: Blog, postId: Long) = addBlogBlock(blog.uuid, postId)
    suspend fun addBlogBlock(blog: String, postId: Post) = addBlogBlock(blog, postId.id)
    suspend fun addBlogBlock(blog: Blog, postId: Post) = addBlogBlock(blog.uuid, postId.id)

    // * // * // * // * // * // * // * // * // * // * //

    suspend fun addBlogBlocks(blog: String, force: Boolean = false, vararg blogs: Blog) {
        client.postAndCheck {
            url {
                appendPathSegments(version, "blog", blog, "blocks", "bulk")
                parameters.append("force", force.toString())
                parameters.append("blocked_tumblelogs", blogs.joinToString(",") { it.uuid })
            }
        }
    }

    suspend fun addBlogBlocks(blog: String, vararg blogs: Blog) = addBlogBlocks(blog, false, *blogs)

    // * // * // * // * // * // * // * // * // * // * //

    private suspend fun removeBlogBlock(blog: String, unblockBlog: String?, anonymousOnly: Boolean) {
        client.deleteAndCheck {
            url {
                appendPathSegments(version, "blog", blog, "blocks")
                parameters.append("anonymous_only", anonymousOnly.toString())
                if (unblockBlog != null) parameters.append("blocked_tumblelog", unblockBlog)
            }
        }
    }

    suspend fun removeBlogBlock(blog: String, unblockBlog: String) = removeBlogBlock(blog, unblockBlog, false)
    suspend fun removeBlogBlock(blog: String, unblockBlog: Blog) = removeBlogBlock(blog, unblockBlog.uuid, false)
    suspend fun removeBlogBlock(blog: String) = removeBlogBlock(blog, null, true)

    // * // * // * // * // * // * // * // * // * // * //

    @Serializable
    class BlogFollowing(val blogs: Set<Blog>, @SerialName("total_blogs") val totalBlogs: Int)

    suspend fun blogFollowing(blog: String, limit: Byte = 20, offset: Int = 0) = client.getAndCheck {
        url {
            appendPathSegments(version, "blog", blog, "following")
            parameters.append("offset", offset.toString())
            parameters.append("limit", limit.toString())
        }
    }.body<Response<BlogFollowing>>().contents

    suspend fun blogFollowing(blog: Blog, limit: Byte = 20, offset: Int = 0) = blogFollowing(blog.uuid, limit, offset)

    // * // * // * // * // * // * // * // * // * // * //

    @Serializable
    class BlogFollowers(val users: Set<Blog>, @SerialName("total_users") val totalUsers: Int)

    suspend fun blogFollowers(blog: String, limit: Byte = 20, offset: Int = 0) = client.getAndCheck {
        url {
            appendPathSegments(version, "blog", blog, "followers")
            parameters.append("offset", offset.toString())
            parameters.append("limit", limit.toString())
        }
    }.body<Response<BlogFollowers>>().contents

    suspend fun blogFollowers(blog: Blog, limit: Byte = 20, offset: Int = 0) = blogFollowers(blog.uuid, limit, offset)

    // * // * // * // * // * // * // * // * // * // * //

    @Serializable
    class BlogFollowedBy(@SerialName("followed_by") val followedBy: Boolean)

    suspend fun followedBy(blog: String, who: String) = client.getAndCheck {
        url {
            appendPathSegments(version, "blog", blog, "followers")
            parameters.append("query", who)
        }
    }.body<Response<BlogFollowedBy>>().contents.followedBy

    suspend fun followedBy(blog: Blog, who: String) = followedBy(blog.uuid, who)
    suspend fun followedBy(blog: String, who: Blog) = followedBy(blog, who.name)
    suspend fun followedBy(blog: Blog, who: Blog) = followedBy(blog.uuid, who.name)

    // * // * // * // * // * // * // * // * // * // * //

    // state isn't described in API docs. what does it mean, engineers???
    @Serializable
    class BlogQueuedPosts(private val state: JsonElement, val posts: Set<Post>)

    suspend fun blogQueuedPosts(
        blog: String,
        offset: Int = 0,
        limit: Byte = 20,
        textFilter: PostTextReturnFilter = PostTextReturnFilter.HTML,
    ) = client.getAndCheck {
        url {
            appendPathSegments(version, "blog", blog, "posts", "queue")
            parameters.append("offset", offset.toString())
            parameters.append("limit", limit.toString())
            if (textFilter != PostTextReturnFilter.HTML) parameters.append("filter", textFilter.name.lowercase())
        }
    }.body<Response<BlogQueuedPosts>>().contents.posts

    suspend fun blogQueuedPosts(
        blog: Blog,
        offset: Int = 0,
        limit: Byte = 20,
        textFilter: PostTextReturnFilter = PostTextReturnFilter.HTML,
    ) = blogQueuedPosts(blog.uuid, offset, limit, textFilter)

    // * // * // * // * // * // * // * // * // * // * //

    suspend fun blogReorderQueuedPost(
        blog: String,
        postID: Long,
        insertAfterPostID: Long,
    ) = client.postAndCheck {
        url {
            appendPathSegments(version, "blog", blog, "posts", "queue")
            parameters.append("insert_after", insertAfterPostID.toString())
            parameters.append("post_id", postID.toString())
        }
    }.bodyAsText()

    suspend fun blogReorderQueuedPost(
        blog: Blog,
        postID: Long,
        insertAfterPostID: Long,
    ) = blogReorderQueuedPost(blog.uuid, postID, insertAfterPostID)

    suspend fun blogReorderQueuedPost(
        blog: Blog,
        postID: Post,
        insertAfterPostID: Long,
    ) = blogReorderQueuedPost(blog.uuid, postID.id, insertAfterPostID)

    suspend fun blogReorderQueuedPost(
        blog: Blog,
        postID: Post,
        insertAfterPostID: Post,
    ) = blogReorderQueuedPost(blog.uuid, postID.id, insertAfterPostID.id)

    suspend fun blogReorderQueuedPost(
        blog: String,
        postID: Post,
        insertAfterPostID: Long,
    ) = blogReorderQueuedPost(blog, postID.id, insertAfterPostID)

    suspend fun blogReorderQueuedPost(
        blog: String,
        postID: Post,
        insertAfterPostID: Post,
    ) = blogReorderQueuedPost(blog, postID.id, insertAfterPostID.id)

    suspend fun blogReorderQueuedPost(
        blog: Blog,
        postID: Long,
        insertAfterPostID: Post,
    ) = blogReorderQueuedPost(blog.uuid, postID, insertAfterPostID.id)

    suspend fun blogReorderQueuedPost(
        blog: String,
        postID: Long,
        insertAfterPostID: Post,
    ) = blogReorderQueuedPost(blog, postID, insertAfterPostID.id)

    // * // * // * // * // * // * // * // * // * // * //

    suspend fun blogShuffleQueuedPosts(blog: String) = client.postAndCheck {
        url { appendPathSegments(version, "blog", blog, "posts", "queue", "shuffle") }
    }.bodyAsText()

    suspend fun blogShuffleQueuedPosts(blog: Blog) = blogShuffleQueuedPosts(blog.uuid)

    // * // * // * // * // * // * // * // * // * // * //

    suspend fun blogDraftPosts(
        blog: String,
        before: Long? = null,
        textFilter: PostTextReturnFilter = PostTextReturnFilter.HTML,
    ) = client.getAndCheck {
        url {
            appendPathSegments(version, "blog", blog, "posts", "draft")
            if (before != null) parameters.append("before_id", before.toString())
            parameters.append("filter", textFilter.toString())
        }
    }.body<Response<Set<Post>>>()

    suspend fun blogDraftPosts(
        blog: Blog,
        before: Long? = null,
        textFilter: PostTextReturnFilter = PostTextReturnFilter.HTML,
    ) = blogDraftPosts(blog.uuid, before, textFilter)

    suspend fun blogDraftPosts(blog: Blog, before: Post, textFilter: PostTextReturnFilter = PostTextReturnFilter.HTML) =
        blogDraftPosts(blog.uuid, before.id, textFilter)

    suspend fun blogDraftPosts(
        blog: String,
        before: Post,
        textFilter: PostTextReturnFilter = PostTextReturnFilter.HTML,
    ) = blogDraftPosts(blog, before.id, textFilter)

    // * // * // * // * // * // * // * // * // * // * //

    suspend fun blogSubmissionPosts(
        blog: String,
        offset: Int? = 0,
        textFilter: PostTextReturnFilter = PostTextReturnFilter.HTML,
    ) = client.getAndCheck {
        url {
            appendPathSegments(version, "blog", blog, "posts", "submission")
            parameters.append("offset", offset.toString())
            if (textFilter != PostTextReturnFilter.HTML) parameters.append("filter", textFilter.toString())
        }
    }.body<Response<JsonElement>>()

    suspend fun blogSubmissionPosts(
        blog: Blog,
        offset: Int? = 0,
        textFilter: PostTextReturnFilter = PostTextReturnFilter.HTML,
    ) = blogSubmissionPosts(blog.uuid, offset, textFilter)

    // * // * // * // * // * // * // * // * // * // * //
}

@Serializable
class CodeGrant(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String,
    private val scope: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
) {
    fun withTimestamp() = CodeGrantWithTimestamp(
        accessToken,
        (epoch() + expiresIn * 1000) - 5000, tokenType,
        scope.split(" ").toSet(),
        refreshToken
    )
}

class CodeGrantWithTimestamp(
    val accessToken: String,
    val expiresAt: Long,
    val tokenType: String,
    val scope: Set<String>,
    val refreshToken: String?,
)

fun epoch() = Clock.System.now().toEpochMilliseconds()
expect class OAuthClient(
    consumerKey: String,
    consumerSecret: String,
    authorizationCode: String,
    redirectUri: String? = null,
) : OAuthClientBase