package com.exhibitiondot.data.network.retrofit

import com.exhibitiondot.data.network.api.ApiConst
import com.exhibitiondot.domain.repository.PreferenceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val preferenceRepository: PreferenceRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return runBlocking {
            val userId = preferenceRepository.userId.first()
            val newRequest = chain.request().newBuilder().apply {
                userId?.let { addHeader(ApiConst.AUTH_HEADER, it) }
            }.build()
            chain.proceed(newRequest)
        }
    }
}