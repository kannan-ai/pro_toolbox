package com.example.utilityhub.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET

interface UpdateApi {
    @GET("repos/KANNAN/pro_toolbox/releases/latest")
    suspend fun getLatestRelease(): GitHubRelease
}

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("name") val name: String,
    @SerializedName("body") val body: String,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("assets") val assets: List<GitHubAsset>
)

data class GitHubAsset(
    @SerializedName("browser_download_url") val downloadUrl: String,
    @SerializedName("name") val name: String,
    @SerializedName("size") val size: Long
)
