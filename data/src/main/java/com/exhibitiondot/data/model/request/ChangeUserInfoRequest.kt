package com.exhibitiondot.data.model.request

import kotlinx.serialization.Serializable

@Serializable
data class ChangeUserInfoRequest(
    val nickname: String,
    val region: String,
    val categoryList: List<String>,
    val eventTypeList: List<String>
)
