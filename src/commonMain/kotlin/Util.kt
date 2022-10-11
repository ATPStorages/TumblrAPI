package me.atpstorages.tumblr_api

fun Set<Post>.filterContent(vararg filters: PostContentType, strict: Boolean = false) = buildSet {
    this@filterContent
        .filter { (strict && it.content.none { content -> !filter.contains(content.type) }) || !strict }
        .forEach { add(it.filterContent(filter)) }
}

fun Post.filterContent(vararg types: PostContentType) = Post(
    this.id,
    this.timestamp,
    this.content.filter { types.contains(it.type) }
)
