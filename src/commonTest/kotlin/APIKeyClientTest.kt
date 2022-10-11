package me.atpstorages.tumblr_api

import io.ktor.http.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ClientTest {
    private val clientNoKey = Client("")
    private val clientWithKey = Client("")
    val clientWithOAuth = Unit

    @Test
    fun checkBlogAvatar() = runTest {
        assertIs<Url>(clientNoKey.blogAvatar("tumblr"), "Client w/o API Key returned non-url result (default)")
        assertIs<Url>(clientWithKey.blogAvatar("tumblr"), "Client w/ API Key returned non-url result (default)")
        assertIs<Url>(clientNoKey.blogAvatar("tumblr", BlogAvatarSize.SQUARE_128), "Client w/o API Key returned non-url result (128px)")
        assertIs<Url>(clientWithKey.blogAvatar("tumblr", BlogAvatarSize.SQUARE_128), "Client w/ API Key returned non-url result (px)")
    }

    @Test
    fun checkTagged() = runTest {
        // TODO
    }
}