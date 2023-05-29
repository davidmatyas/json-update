package com.language_tool.service

import com.language_tool.dto.SummaryChangeResponse
import com.language_tool.dto.UpdateRequest

interface JsonService {
    fun updateJson(updateRequest: UpdateRequest): List<SummaryChangeResponse>
}