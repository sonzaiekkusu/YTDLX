package com.sonzaiekkusu.ytdlx

import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class YtdlpBridge {
    suspend fun metadata(url: String): VideoMetadata = withContext(Dispatchers.IO) {
        val result = module().callAttr("extract_metadata", url).toJava(String::class.java)
        val json = JSONObject(result)
        if (json.optBoolean("playlist")) {
            val entries = json.optJSONArray("entries")
            if (entries == null || entries.length() == 0) {
                error("Playlist tidak memiliki item yang bisa diproses")
            }
            return@withContext parseVideo(entries.getJSONObject(0))
        }
        parseVideo(json)
    }

    suspend fun download(url: String, quality: QualityOption, stagingDirectory: String): String =
        withContext(Dispatchers.IO) {
            module().callAttr(
                "download",
                url,
                quality.formatSelector(),
                stagingDirectory,
            ).toJava(String::class.java)
        }

    private fun module() = Python.getInstance().getModule("ytdlx_engine")

    private fun parseVideo(json: JSONObject): VideoMetadata = VideoMetadata(
        id = json.optString("id").takeIf { it.isNotBlank() },
        title = json.optString("title", "Tanpa judul"),
        channel = json.optString("channel").takeIf { it.isNotBlank() },
        uploader = json.optString("uploader").takeIf { it.isNotBlank() },
        duration = json.optInt("duration").takeIf { json.has("duration") && !json.isNull("duration") },
        view_count = json.optLong("view_count").takeIf { json.has("view_count") && !json.isNull("view_count") },
        upload_date = json.optString("upload_date").takeIf { it.isNotBlank() },
        thumbnail = json.optString("thumbnail").takeIf { it.isNotBlank() },
        webpage_url = json.optString("webpage_url").takeIf { it.isNotBlank() },
        description = json.optString("description").takeIf { it.isNotBlank() },
    )
}
