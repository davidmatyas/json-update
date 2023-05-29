package com.language_tool.controller

import com.language_tool.dto.SummaryChangeResponse
import com.language_tool.dto.UpdateRequest
import com.language_tool.service.JsonService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/update")
class JsonCompareController(val jsonService: JsonService) {
    @PostMapping
    fun updateJson(@RequestBody request: UpdateRequest): ResponseEntity<List<SummaryChangeResponse>> {
        return ResponseEntity.ok(jsonService.updateJson(request))
    }
}