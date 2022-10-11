package me.atpstorages.tumblr_api

import io.ktor.client.engine.curl.*

// Use cURL

actual class Client actual constructor(consumerKey: String): BaseClient(Curl, consumerKey)