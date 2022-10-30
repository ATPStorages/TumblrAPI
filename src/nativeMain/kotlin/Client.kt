package me.atpstorages.tumblr_api

import io.ktor.client.engine.curl.*

// Use cURL
actual class Client actual constructor(consumerKey: String) : BaseClient(Curl, consumerKey)
actual class OAuthClient actual constructor(
    consumerKey: String,
    consumerSecret: String,
    authorizationCode: String,
    redirectUri: String?,
) :
    OAuthClientBase(Curl, consumerKey, consumerSecret, authorizationCode, redirectUri)