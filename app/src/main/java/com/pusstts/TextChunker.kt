package com.pusstts

/**
 * Podcast 文本分塊器
 *
 * 支援角色標記語法：
 *   [女聲]：大家好，歡迎收聽。[男聲]：今天來聊 ...
 *
 * 自動識別 [女聲]/[男聲] 前綴，維護角色 → Speaker ID 映射
 */
class TextChunker(private val maxChars: Int = 80) {

    data class ScriptChunk(
        val text: String,
        val speakerId: Int,
        val speakerName: String,
    )

    // 音色映射（對應 sherpa-onnx kokoro-multi-lang voices.bin）
    private val speakerMap = mapOf(
        "女聲" to 0,    // af_xiaoni (預設女聲)
        "男聲" to 10,   // am_adam (預設男聲)
    )

    // 解析角色標籤的正則
    private val rolePattern = "\\[(女聲|男聲)]：?".toRegex()

    /**
     * 解析完整 Podcast 文本為 ScriptChunk 列表
     */
    fun split(script: String): List<ScriptChunk> {
        val result = mutableListOf<ScriptChunk>()
        val lines = script.split("\n").filter { it.isNotBlank() }

        for (line in lines) {
            val trimmed = line.trim()
            val roleMatch = rolePattern.find(trimmed)

            if (roleMatch != null) {
                val roleName = roleMatch.groupValues[1]
                val speakerId = speakerMap[roleName] ?: 0
                // 移除 [角色]：前綴
                val text = trimmed.substring(roleMatch.range.last + 1).trim()
                if (text.isNotEmpty()) {
                    // 過長則進一步分句
                    result.addAll(splitLongText(text, speakerId, roleName))
                }
            } else {
                // 無角色標記的文字，當作旁白用女聲
                if (trimmed.isNotEmpty()) {
                    result.addAll(splitLongText(trimmed, 0, "女聲"))
                }
            }
        }
        return result
    }

    /**
     * 將過長的文字塊細分為句子
     */
    private fun splitLongText(text: String, speakerId: Int, speakerName: String): List<ScriptChunk> {
        if (text.length <= maxChars) return listOf(ScriptChunk(text, speakerId, speakerName))

        val result = mutableListOf<ScriptChunk>()
        // 句號、問號、驚嘆號切分
        val sentenceSplit = "(?<=[。！？!?\n])".toRegex()
        val parts = text.split(sentenceSplit).filter { it.isNotBlank() }

        val current = StringBuilder()
        for (part in parts) {
            if (current.length + part.length > maxChars && current.isNotEmpty()) {
                result.add(ScriptChunk(current.toString().trim(), speakerId, speakerName))
                current.clear()
            }
            current.append(part)
        }
        if (current.isNotEmpty()) {
            result.add(ScriptChunk(current.toString().trim(), speakerId, speakerName))
        }
        return result
    }
}
