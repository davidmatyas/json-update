package com.language_tool.dto

data class UpdateRequest(
    // In request path need to be replaced part with name of app with string {subfolder}
    var originDirectory: String,
    var updateDirectory: String,
    var resultDirectory: String,
    var addNewKeys: Boolean? = false,
    var appNameList: List<String>,
)