package me.atpstorages.tumblr_api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Blog(
    val title: String,
    @SerialName("posts") val totalPosts: Int,
)

@Serializable
class BlogPostsResponse(
    val blog: Blog,
    val posts: Set<Post>
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
class AvatarResponseObject(val avatar_url: String)

// blogLikes
@Serializable
class LikedBlogPostsResponse(
    @SerialName("liked_posts") val posts: Set<Post>,
    @SerialName("liked_count") val totalLiked: Int
)
