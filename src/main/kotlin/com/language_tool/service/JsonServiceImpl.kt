package com.language_tool.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.language_tool.dto.SummaryChangeResponse
import com.language_tool.dto.UpdateRequest
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.springframework.stereotype.Service
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

//const val NEW_FILES_ROOT_DIRECTORY = "R:\\Reist\\localizationSand"
//const val INCLUDE_NEW = true
//val APP_LIST = listOf("credential-reset", "vault", "selfservice", "enrollment")

//const val ORIGIN_DIRECTORY: String =
//    "R:\\Reist\\frontend\\apps\\{subfolder}\\src\\assets\\locales"
//const val ORIGIN_DIRECTORY: String =
//    "R:\\Reist\\localizationSand\\localizationSand_2023-05-28-202052\\{subfolder}"
//
//const val UPDATE_DIRECTORY: String =
//    "R:\\Reist\\localization\\localization-2023-05-25\\{subfolder}"

//const val UPDATE_DIRECTORY: String =
//    "R:\\Reist\\localizationSand\\localizationSand_2023-05-28-150311\\{subfolder}"
@Service
class JsonServiceImpl : JsonService {
    override fun updateJson(updateRequest: UpdateRequest): List<SummaryChangeResponse> {

        // make root directory for new files
        val destinationParentPath = updateRequest.resultDirectory
        val sourceDirectory = File(destinationParentPath)
        val timestamp = SimpleDateFormat("yyyy-MM-dd-HHMMss").format(Date())
        val destinationPath = "$destinationParentPath/${sourceDirectory.name}_$timestamp"
        val destinationDirectory = File(destinationPath)
        destinationDirectory.mkdirs()

        // read original files
        val jsonFilesOrigin = mutableListOf<File>()
        updateRequest.appNameList.forEach { app ->
            this.getJsonFilesList(File(updateRequest.originDirectory.replace("{subfolder}", app)), jsonFilesOrigin)
        }
        val jsonUpdatedFiles = mutableListOf<File>()
        updateRequest.appNameList.forEach { app ->
            this.getJsonFilesList(File(updateRequest.updateDirectory.replace("{subfolder}", app)), jsonUpdatedFiles)
        }


        // Create map with values
        val originMap = this.mapFilesContent(jsonFilesOrigin, updateRequest.appNameList)
        val updatedMap = this.mapFilesContent(jsonUpdatedFiles, updateRequest.appNameList)

        // Map new value from update
        val changeLog = mutableListOf<String>()
        val newMap =
            this.createUpdateMap(
                original = originMap,
                updatedMap = updatedMap,
                changeLog = changeLog,
                includeNew = updateRequest.addNewKeys
            )
        saveChangeLogToFile(changeLog, "$destinationPath\\changelog.txt")

        // create subfolders and files with new values
        for (file in newMap) {
            val fileName = file.key.substringAfterLast("/")
            //create app folder
            val appSubfolder = file.key.substringBefore("/")
            if (!File("$destinationPath\\$appSubfolder").exists()) {
                File("$destinationPath\\$appSubfolder").mkdirs()
            }
            //create language folder
            val languageSubfolder = file.key.substringBeforeLast("/").substringAfterLast("/")
            if (!File("$destinationPath\\$appSubfolder\\$languageSubfolder").exists()) {
                File("$destinationPath\\$appSubfolder\\$languageSubfolder").mkdirs()
            }
            // validate JSON and save file
            if (isValidJson(file.value)) {
                (this.convertValueToJSONAndSave(
                    data = file.value,
                    filePath = "$destinationPath\\$appSubfolder\\$languageSubfolder\\$fileName"
                ))
            }
        }
        return createChangeLogResponse(changeLog, destinationPath)
    }

    private fun createChangeLogResponse(
        changeLog: MutableList<String>,
        resultDestination: String,
    ): List<SummaryChangeResponse> {
        val changeTable = changeLog.map { it.substringBefore("json").split(";") }
        val fileCountMap = mutableMapOf<String, Int>()
        for (item in changeTable) {
            if (fileCountMap.containsKey(item[0])) {
                fileCountMap.put(key = item[0], value = fileCountMap.getValue(item[0]) + 1)
            } else {
                fileCountMap.put(item[0], 1)
            }
        }
        return fileCountMap.mapNotNull {
            SummaryChangeResponse(
                it.key, it.value,
                "$resultDestination//changelog.txt"
            )
        }
    }

    private fun isValidJson(json: Any): Boolean {
        try {
            // Try parsing the JSON string to a JSON object
            JSONObject(json)
        } catch (e: JSONException) {
            try {
                // Try parsing the JSON string to a JSON array
                JSONArray(json)
            } catch (e: JSONException) {
                return false
            }
        }
        return true
    }

    private fun saveChangeLogToFile(changeLog: MutableList<String>, filePath: String) {
        val file = File(filePath)
        val writer = BufferedWriter(FileWriter(file))
        try {
            val update = changeLog.filter { it.startsWith("U") }
            val new = changeLog.filter { it.startsWith("N") }
            this.writeToFile(update, writer)
            this.writeToFile(new, writer)
        } finally {
            writer.close()
        }
    }

    private fun writeToFile(file: List<String>, writer: BufferedWriter) {
        for (item in file) {
            writer.write(item)
            writer.newLine()
        }
    }

    private fun mapFilesContent(files: MutableList<File>, appName: List<String>): MutableMap<String, Map<String, Any>> {
        val objectMapper = ObjectMapper()
        val map: MutableMap<String, Map<String, Any>> = mutableMapOf()
        for (file in files) {
            try {
                val jsonMap: Map<String, Any> = objectMapper.readValue(file, Map::class.java) as Map<String, Any>
                val languageMutation = file.toPath().parent.toString().substringAfterLast("\\")
                val subfolder = appName.first { element ->
                    file.parent.toString().contains(element)
                }
                map["$subfolder/$languageMutation/${file.name}"] = jsonMap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return map
    }

    private fun getJsonFilesList(directory: File, kotlinFiles: MutableList<File>) {
        val files = directory.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isDirectory) {
                    getJsonFilesList(file, kotlinFiles)
                } else if (file.isFile && file.extension.equals("json", ignoreCase = true)) {
                    kotlinFiles.add(file)
                }
            }
        }
    }

    private fun convertValueToJSONAndSave(data: Any, filePath: String) {
        val objectMapper = ObjectMapper()
        val json = objectMapper.writeValueAsString(data)
        val file = File(filePath)
        file.writeText(json)
    }

    private fun createUpdateMap(
        original: Map<String, Any>,
        updatedMap: Map<String, Any>,
        changeLog: MutableList<String>,
        currentPath: String = "",
        includeNew: Boolean? = false,
    ): Map<String, Any> {
        val newMap = mutableMapOf<String, Any>()
        for ((key, valueOriginal) in original) {
            val valueUpdated = updatedMap[key]
            val path = if (currentPath.isEmpty()) key else "$currentPath.$key"
            if (valueUpdated is String && updatedMap[key] != original[key]) {
                changeLog.add("Update;$path: ${original[key]} -> ${updatedMap[key].toString()}")
            }

            if (valueUpdated is Map<*, *> && valueOriginal is Map<*, *>) {
                val nestedMapOriginal = valueOriginal as Map<String, Any>
                val nestedMapUpdate = valueUpdated as Map<String, Any>
                newMap[key] = createUpdateMap(nestedMapOriginal, nestedMapUpdate, changeLog, path, includeNew)
            } else {
                newMap[key] = valueUpdated ?: valueOriginal
            }
        }
        // Check for new keys in the updatedMap and add them to the change log
        for ((key, valueUpdated) in updatedMap) {
            if (!original.containsKey(key)) {
                val path = if (currentPath.isEmpty()) key else "$currentPath.$key"
                changeLog.add("New;$path <null> -> value $valueUpdated")
                if (includeNew == true) {
                    newMap.put(key, valueUpdated)
                }
            }
        }
        return newMap
    }
}