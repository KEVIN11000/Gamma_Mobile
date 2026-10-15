package com.gamma.mobile.data.remote

import com.gamma.mobile.domain.model.GitHubReleaseDto
import retrofit2.Response
import retrofit2.http.GET

interface GitHubReleaseApi {
    @GET("repos/KEVIN11000/Gamma_Mobile/releases/latest")
    suspend fun getLatestRelease(): Response<GitHubReleaseDto>
}
