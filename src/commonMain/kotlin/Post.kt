package me.atpstorages.tumblr_api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames

@Serializable
class Post(
    val id: Long,
    val timestamp: Long,
    val content: List<Content>? = null,
    val tags: List<String>? = null,
    val blog: Blog? = null,
    val slug: String? = null,
    val summary: String? = null,
    @SerialName("should_open_in_legacy") val shouldOpenInLegacy: Boolean? = null,
    // layout, trail, interactability_reblog
    @SerialName("can_like") val canLike: Boolean? = null,
    @SerialName("can_reblog") val canReblog: Boolean? = null,
    @SerialName("can_send_in_message") val canSendInMessage: Boolean? = null,
    @SerialName("can_reply") val canReply: Boolean? = null,
    @SerialName("display_avatar") val displayAvatar: Boolean? = null,
    @SerialName("reblog_key") val reblogKey: String,
    @SerialName("short_url") val shortUrl: String? = null,
    @SerialName("genesis_post_id") val genesisId: String? = null,
    @SerialName("blog_name") val blogName: String? = null,
    @SerialName("post_url") val url: String
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
enum class PostContentType {
    @JsonNames("text") TEXT,
    @JsonNames("link") LINK,
    @JsonNames("audio") AUDIO,
    @JsonNames("video") VIDEO,
    @JsonNames("image") IMAGE;
}

@Serializable
sealed class Content(
    val type: PostContentType
) {
    @Serializable
    @SerialName("text")
    class Text(
        val text: String
    ): Content(PostContentType.TEXT)

    @Serializable
    @SerialName("image")
    class Image(
        val media: List<Media>,
        val colors: Map<String, String>? = null,
        @SerialName("feedback_token") val feedbackToken: String? = null,
        val poster: List<Media>? = null,
        val attribution: Attribution? = null,
        @SerialName("alt_text") val alternativeText: String? = null,
        val caption: String? = null
    ): Content(PostContentType.IMAGE)

    @Serializable
    @SerialName("link")
    class Link(
        val url: String,
        val title: String? = null,
        val description: String? = null,
        val author: String? = null,
        @SerialName("site_name") val name: String? = null,
        @SerialName("display_url") val displayUrl: String? = null,
        val poster: List<Media>? = null
    ): Content(PostContentType.LINK)

    @Serializable
    @SerialName("audio")
    class Audio(
        override val media: Media? = null,
        override val url: String? = null,
        override val provider: String? = null,
        val title: String? = null,
        val artist: String? = null,
        @SerialName("album") val albumName: String? = null,
        override val poster: List<Media>? = null,
        @SerialName("embed_html") val embedHtml: String? = null,
        @SerialName("embed_url") val embedUrl: String? = null,
        override val metadata: JsonElement? = null,
        val attribution: Attribution? = null
    ): Content(PostContentType.AUDIO), AudioVideoContent

    @Serializable
    @SerialName("video")
    class Video(
        override val media: Media? = null,
        override val url: String? = null,
        override val provider: String? = null,
        override val poster: List<Media>? = null,
        @SerialName("embed_html") val embedHtml: String? = null,
        @SerialName("embed_iframe") val embedIFrame: EmbedIFrame? = null,
        @SerialName("embed_url") val embedUrl: String? = null,
        override val metadata: JsonElement? = null,
        @SerialName("can_autoplay_on_cellular") val autoplayOnCellular: Boolean? = null,
        val attribution: Attribution? = null
    ): Content(PostContentType.VIDEO), AudioVideoContent

    // TODO: Paywall
}

interface BaseMedia {
    val url: String
    val width: Int?
    val height: Int?
}

@Serializable
class EmbedIFrame(
    override val url: String,
    override val width: Int? = null,
    override val height: Int? = null
): BaseMedia

@Serializable
class Media(
    val type: String? = null,
    override val url: String,
    override val width: Int? = null,
    override val height: Int? = null,
    val cropped: Boolean? = null,
    @SerialName("has_original_dimensions") val originalSize: Boolean? = null,
    @SerialName("original_dimensions_missing") val dimensionsDefault: Boolean? = null
): BaseMedia



interface AudioVideoContent {
    val media: Media?
    val url: String?
    val provider: String?
    val poster: List<Media>?
    val metadata: JsonElement?
    //val attribution: Content?
}

@Serializable
@OptIn(ExperimentalSerializationApi::class)
enum class ContentAttributionType {
    @JsonNames("link") LINK,
    @JsonNames("blog") BLOG,
    @JsonNames("post") POST,
    @JsonNames("app") APP;
}

@Serializable
sealed class Attribution(
    val type: ContentAttributionType
) {
    @Serializable
    @SerialName("post")
    class Post(
        val url: String,
        val post: me.atpstorages.tumblr_api.Post,
        val blog: Blog
    ): Attribution(ContentAttributionType.POST)

    @Serializable
    @SerialName("link")
    class Link(
        val url: String
    ): Attribution(ContentAttributionType.LINK)

    @Serializable
    @SerialName("blog")
    class Blog(
        val blog: me.atpstorages.tumblr_api.Blog
    ): Attribution(ContentAttributionType.BLOG)

    @Serializable
    @SerialName("app")
    class App(
        val url: String,
        @SerialName("app_name") val appName: String? = null,
        @SerialName("display_text") val displayText: String? = null,
        val logo: Media? = null
    ): Attribution(ContentAttributionType.APP)
}