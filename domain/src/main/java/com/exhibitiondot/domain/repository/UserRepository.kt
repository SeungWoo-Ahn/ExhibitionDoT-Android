package com.exhibitiondot.domain.repository

import com.exhibitiondot.domain.model.UpdateUserInfo
import com.exhibitiondot.domain.model.User

interface UserRepository {
    suspend fun getUser(): Result<User>

    suspend fun signIn(email: String): Result<Long>

    suspend fun signUp(user: User): Result<Unit>

    suspend fun updateUserInfo(updateUserInfo: UpdateUserInfo): Result<Unit>
}