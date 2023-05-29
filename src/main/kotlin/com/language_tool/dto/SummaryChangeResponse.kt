package com.language_tool.dto

data class SummaryChangeResponse(
    val type: String,
    val count: Int,
    val changeLogPath: String,
) {
//    companion object(shortType: String) {
//        var type = when (shortType) {
//            "U" -> "Updated"
//            "N"-> "New"
//            else -> "Unknown"
//        }
//    }
}

