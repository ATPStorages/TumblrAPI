package me.atpstorages.tumblr_api

import io.ktor.client.engine.apache.*

// Use Apache
actual class Client actual constructor(consumerKey: String) : BaseClient(Apache, consumerKey)
actual class OAuthClient actual constructor(
    consumerKey: String,
    consumerSecret: String,
    authorizationCode: String,
    redirectUri: String?,
) :
    OAuthClientBase(Apache, consumerKey, consumerSecret, authorizationCode, redirectUri)