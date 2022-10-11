package me.atpstorages.tumblr_api

fun Set<Post>.filterContent(strict: Boolean = false, vararg filters: PostContentType) = buildSet {
    this@filterContent
        .filter { (strict && it.content.all { content -> filters.contains(content.type) }) || !strict }
        .forEach { add(it.filterContent(*filters)) }
}

fun Post.filterContent(vararg types: PostContentType) = Post(
    this.id,
    this.timestamp,
    this.content.filter { types.contains(it.type) }
)
