package me.atpstorages.tumblr_api

fun Set<Post>.filterContent(strict: Boolean = false, vararg filters: PostContentType) = buildSet {
    this@filterContent
        .filter { (strict && (it.content?.all { content -> filters.contains(content.type) } == true)) || !strict }
        .forEach { add(it.filterContent(*filters)) }
}

fun Post.filterContent(vararg types: PostContentType) = Post(
    this.id,
    this.timestamp,
    this.content?.filter { types.contains(it.type) },
    this.tags,
    this.blog,
    this.slug,
    this.summary,
    this.shouldOpenInLegacy,
    this.canLike,
    this.canReblog,
    this.canSendInMessage,
    this.canReply,
    this.displayAvatar,
    this.reblogKey,
    this.shortUrl,
    this.genesisId,
    this.blogName,
    this.url
)
