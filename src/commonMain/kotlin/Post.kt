package me.atpstorages.tumblr_api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.JsonNames
import kotlinx.serialization.json.JsonElement

@Serializable
class Post(
    val id: Long,
    val timestamp: Long,
    val content: List<Content>
)

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
        val media: List<Media>
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
        //override val attribution: Content? = null
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
        //override val attribution: Content? = null
    ): Content(PostContentType.VIDEO), AudioVideoContent
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
): BaseMedia()

@Serializable
class Media(
    val type: String? = null,
    override val url: String,
    override val width: Int? = null,
    override val height: Int? = null,
    val cropped: Boolean? = null,
    @SerialName("has_original_dimensions") val originalSize: Boolean? = null,
    @SerialName("original_dimensions_missing") val dimensionsDefault: Boolean? = null
): BaseMedia()

interface AudioVideoContent {
    val media: Media?
    val url: String?
    val provider: String?
    val poster: List<Media>?
    val metadata: JsonElement?
    //val attribution: Content?
}
