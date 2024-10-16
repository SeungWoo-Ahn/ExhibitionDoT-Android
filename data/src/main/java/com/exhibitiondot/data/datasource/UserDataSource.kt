package com.exhibitiondot.data.datasource

import com.exhibitiondot.data.model.request.ChangeUserInfoRequest
import com.exhibitiondot.data.model.request.SignInRequest
import com.exhibitiondot.data.model.request.SignUpRequest
import com.exhibitiondot.data.model.response.SignInResponse
import com.exhibitiondot.data.network.NetworkState

interface UserDataSource {
    suspend fun sigIn(signInRequest: SignInRequest): NetworkState<SignInResponse>

    suspend fun signUp(signUpRequest: SignUpRequest): NetworkState<Void>

    suspend fun changeUserInfo(changeUserInfoRequest: ChangeUserInfoRequest): NetworkState<Void>
}