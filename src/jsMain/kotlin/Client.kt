package me.atpstorages.tumblr_api

import io.ktor.client.engine.js.*

// Use Js
actual class Client actual constructor(consumerKey: String) : BaseClient(Js, consumerKey)
actual class OAuthClient actual constructor(
    consumerKey: String,
    consumerSecret: String,
    authorizationCode: String,
    redirectUri: String?,
) :
    OAuthClientBase(Js, consumerKey, consumerSecret, authorizationCode, redirectUri)