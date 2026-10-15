package com.gamma.mobile.domain.model

import com.google.gson.annotations.SerializedName

data class GitHubReleaseDto(
    @SerializedName("tag_name") val tagName: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("body") val body: String = "",
    @SerializedName("assets") val assets: List<GitHubAssetDto> = emptyList()
)

data class GitHubAssetDto(
    @SerializedName("name") val name: String = "",
    @SerializedName("size") val size: Long = 0L,
    @SerializedName("browser_download_url") val browserDownloadUrl: String = ""
)

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    data class UpToDate(val version: String) : UpdateStatus()
    data class UpdateAvailable(
        val version: String,
        val downloadUrl: String,
        val assetName: String,
        val sizeMb: Double
    ) : UpdateStatus()
    data class CheckFailed(val reason: String?) : UpdateStatus()
}
