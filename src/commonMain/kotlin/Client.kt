package me.atpstorages.tumblr_api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// * // * // * // * // * // * // * // * // * // * //
val json = Json {
    ignoreUnknownKeys = true
}

val version = "v2"
fun createClient(engine: HttpClientEngineFactory<HttpClientEngineConfig>, key: String? = null, oauthToken: String? = null) =
    HttpClient(engine) {
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
                if(key != null) parameters.append("api_key", key)

                headers {
                    append(HttpHeaders.UserAgent, "KTAPI:atp 1.0.0")
                    append(HttpHeaders.ContentType, ContentType.Application.Json)
                    if(oauthToken != null) append(HttpHeaders.Authorization, "Bearer $oauthToken")
                }
            }
        }
    }
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
    suspend fun blogInfo(blog: String) =
        client.get { url { appendPathSegments(version, "blog", blog, "info") } }.body<Blog>()

    // * // * // * // * // * // * // * // * // * // * //

    // filter: Filter what types to include.
    // filterStrict: Posts MUST have only the types specified in filter.

    suspend fun readTag(
        tag: String,
        before: Long = Clock.System.now().epochSeconds,
        after: Long = 0,
        limit: Byte = 20,
        filter: Set<PostContentType>? = null,
        filterStrict: Boolean = false
    ) = this.client.get { url {
        appendPathSegments(version, "tagged")
        parameters.append("tag", tag)
        parameters.append("before", before.toString())
        parameters.append("limit", limit.toString())
    } }.body<Response<Set<Post>>>().let { postSet ->
        Response(
            postSet.status,
            postSet.response
                .filter { it.timestamp > after }.toSet()
                .let { if(filter != null) it.filterContent(filter, filterStrict) else it },
            postSet.errors
        )
    }

    suspend fun readTag(
        tag: String,
        before: Long = Clock.System.now().epochSeconds,
        after: Long = 0,
        limit: Byte = 20,
        filter: PostContentType,
        filterStrict: Boolean = false
    ) = readTag(tag, before, after, limit, setOf(filter), filterStrict)
}

expect class Client(consumerKey: String): BaseClient