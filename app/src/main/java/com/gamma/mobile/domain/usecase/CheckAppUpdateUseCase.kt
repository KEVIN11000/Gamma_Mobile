package com.gamma.mobile.domain.usecase

import com.gamma.mobile.BuildConfig
import com.gamma.mobile.data.remote.GitHubReleaseApi
import com.gamma.mobile.domain.model.UpdateStatus
import com.gamma.mobile.domain.util.SemverUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

class CheckAppUpdateUseCase @Inject constructor(
    private val gitHubApi: GitHubReleaseApi,
    @Named("NetworkDispatcher") private val dispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(installedVersion: String = BuildConfig.VERSION_NAME): UpdateStatus {
        return withContext(dispatcher) {
            try {
                val response = gitHubApi.getLatestRelease()
                if (response.isSuccessful && response.body() != null) {
                    val release = response.body()!!
                    val remoteTag = release.tagName.ifBlank { release.name }

                    if (SemverUtils.isNewer(remoteTag, installedVersion)) {
                        // Buscar el activo APK (.apk)
                        val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                        if (apkAsset != null) {
                            val sizeMb = Math.round(apkAsset.size.toDouble() / (1024.0 * 1024.0) * 100.0) / 100.0
                            UpdateStatus.UpdateAvailable(
                                version = remoteTag,
                                downloadUrl = apkAsset.browserDownloadUrl,
                                assetName = apkAsset.name,
                                sizeMb = sizeMb
                            )
                        } else {
                            UpdateStatus.UpToDate(installedVersion)
                        }
                    } else {
                        UpdateStatus.UpToDate(installedVersion)
                    }
                } else {
                    UpdateStatus.CheckFailed("HTTP ${response.code()}: ${response.message()}")
                }
            } catch (e: Exception) {
                UpdateStatus.CheckFailed(e.localizedMessage)
            }
        }
    }
}
