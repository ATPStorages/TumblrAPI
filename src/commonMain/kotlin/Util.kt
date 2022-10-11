package me.atpstorages.tumblr_api

fun Set<Post>.filterContent(filter: Set<PostContentType>, strict: Boolean = false) = buildSet {
    this@filterContent
        .filter { (strict && it.content.none { content -> !filter.contains(content.type) }) || !strict }
        .forEach { add(it.filterContent(filter)) }
}

fun Set<Post>.filterContent(filter: PostContentType, strict: Boolean = false) = this.filterContent(setOf(filter), strict)

fun Post.filterContent(types: Set<PostContentType>) = Post(
    this.id,
    this.timestamp,
    this.content.filter { types.contains(it.type) }
)

fun Post.filterContent(type: PostContentType) = this.filterContent(setOf(type))