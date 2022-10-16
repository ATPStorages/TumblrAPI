package me.atpstorages.tumblr_api

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
class Blog(
    val uuid: String,
    val name: String? = null,
    val url: String? = null,
    val title: String? = null,
    val description: String? = null,
    @SerialName("followed") val following: Boolean? = null,
    @SerialName("is_blocked_from_primary") val blocked: Boolean? = null,
    @SerialName("ask") val canAsk: Boolean? = null,
    @SerialName("ask_anon") val canAnonymousAsk: Boolean? = null,
    @SerialName("updated") val lastPost: Long? = null,
    @SerialName("posts") val totalPosts: Int? = null,
    @SerialName("likes") val totalLikes: Int? = null,
    val avatar: List<Media>? = null,
    val theme: BlogTheme? = null
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
enum class BlogAvatarShape {
    @JsonNames("circle") CIRCLE,
    @JsonNames("square") SQUARE
}

@Serializable
class BlogTheme(
    @SerialName("avatar_shape") val avatarShape: BlogAvatarShape? = null,
    @SerialName("background_color") val backgroundColorHex: String? = null,
    @SerialName("body_font") val bodyFont: String? = null,
    @SerialName("header_bounds") val headerBounds: String? = null,
    @SerialName("header_image") val headerImageUrl: String? = null,
    @SerialName("header_image_npf") val headerImageContent: Content? = null,
    @SerialName("header_image_focused") val preferredHeaderImage: String? = null,
    @SerialName("header_image_poster") val headerImagePoster: String? = null,
    @SerialName("header_image_scaled") val headerImageScaled: String? = null,
    @SerialName("header_stretch") val headerStretched: Boolean? = null,
    @SerialName("link_color") val linkColorHex: String? = null,
    @SerialName("show_avatar") val showAvatar: Boolean? = null,
    @SerialName("show_description") val showDescription: Boolean? = null,
    @SerialName("show_header_image") val showHeaderImage: Boolean? = null,
    @SerialName("show_title") val showTitle: Boolean? = null,
    @SerialName("title_color") val titleColorHex: String? = null,
    @SerialName("title_font") val titleFont: String? = null,
    @SerialName("title_font_weight") val titleFontWeight: String? = null
)

@Serializable
class BlogPostsResponse(
    val blog: Blog,
    val posts: Set<Post>,
    @SerialName("total_posts") val totalPosts: Int
)

enum class BlogAvatarSize(val px: Short) {
    SQUARE_16  (16),
    SQUARE_24  (24),
    SQUARE_30  (30),
    SQUARE_40  (40),
    SQUARE_48  (48),
    SQUARE_64  (64),
    SQUARE_96  (96),
    SQUARE_128(128),
    SQUARE_512(512)
}

// blogAvatar
@Serializable
class BlogAvatarResponseObject(val avatar_url: String)

// blogLikes
@Serializable
class LikedBlogPosts(
    @SerialName("liked_posts") val posts: Set<Post>,
    @SerialName("liked_count") val totalLiked: Int,
)

interface BlogPostNotesResponse {
    val notes: List<PostNote>
}

@Serializable
@OptIn(ExperimentalSerializationApi::class)
enum class PostLikeType {
    @JsonNames("like")
    LIKE,

    @JsonNames("reply")
    REPLY,

    @JsonNames("posted")
    POSTED,

    @JsonNames("reblog")
    REBLOG,
}

@Serializable
class BlogPostNotesResponseWithTags(
    override val notes: List<PostNote>,
) : BlogPostNotesResponse

@Serializable
class BlogPostNotesResponseNormal(
    override val notes: List<PostNote>,
    @SerialName("total_notes") val totalNotes: Int,
) : BlogPostNotesResponse

@Serializable
class BlogPostNotesResponseConversation(
    override val notes: List<PostNote>,
    @SerialName("total_notes") val totalNotes: Int,
    @SerialName("total_likes") val totalLikes: Int,
    @SerialName("total_reblogs") val totalReblogs: Int,
    @SerialName("rollup_notes") val rollupNotes: List<PostNote>,
) : BlogPostNotesResponse

interface BasePostNote {
    val timestamp: Long
    val blogName: String
    val blogUUID: String
    val blogUrl: String
    val followed: Boolean
    val avatarShape: BlogAvatarShape
}

@Serializable
sealed class PostNote(val type: PostLikeType) {
    @Serializable
    @SerialName("like")
    class Like(
        override val timestamp: Long,
        @SerialName("blog_name") override val blogName: String,
        @SerialName("blog_uuid") override val blogUUID: String,
        @SerialName("blog_url") override val blogUrl: String,
        override val followed: Boolean,
        @SerialName("avatar_shape") override val avatarShape: BlogAvatarShape,
    ) : PostNote(PostLikeType.LIKE), BasePostNote

    @Serializable
    @SerialName("reply")
    class Reply(
        override val timestamp: Long,
        @SerialName("blog_name") override val blogName: String,
        @SerialName("blog_uuid") override val blogUUID: String,
        @SerialName("blog_url") override val blogUrl: String,
        override val followed: Boolean,
        @SerialName("avatar_shape") override val avatarShape: BlogAvatarShape,
        @SerialName("reply_text") val replyText: String,
        // val formatting: List<>
        @SerialName("is_original_poster") val isOriginalPoster: Boolean,
        @SerialName("can_block") val canBlock: Boolean,
    ) : PostNote(PostLikeType.REPLY), BasePostNote

    @Serializable
    @SerialName("posted")
    class Posted(
        override val timestamp: Long,
        @SerialName("blog_name") override val blogName: String,
        @SerialName("blog_uuid") override val blogUUID: String,
        @SerialName("blog_url") override val blogUrl: String,
        override val followed: Boolean,
        @SerialName("avatar_shape") override val avatarShape: BlogAvatarShape,
    ) : PostNote(PostLikeType.POSTED), BasePostNote

    @Serializable
    @SerialName("reblog")
    class Reblog(
        override val timestamp: Long,
        @SerialName("blog_name") override val blogName: String,
        @SerialName("blog_uuid") override val blogUUID: String,
        @SerialName("blog_url") override val blogUrl: String,
        override val followed: Boolean,
        @SerialName("avatar_shape") override val avatarShape: BlogAvatarShape,
        val tags: List<String>? = null,
        @SerialName("post_id") val postID: Long,
        @SerialName("reblog_parent_blog_name") val reblogParentBlogName: String,
    ) : PostNote(PostLikeType.REBLOG), BasePostNote
}