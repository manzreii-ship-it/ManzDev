package com.example.data.repository

import android.net.Uri
import com.example.data.model.StreamSource
import com.example.data.remote.ArchiveOrgApiService
import com.example.data.remote.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StreamResolverRepository(
    private val archiveApi: ArchiveOrgApiService = NetworkModule.archiveOrgApi
) {
    private val streamCache = mutableMapOf<String, List<StreamSource>>()

    suspend fun resolveStreamsForEpisode(
        animeId: Long,
        animeTitle: String,
        episodeNumber: Int
    ): List<StreamSource> = withContext(Dispatchers.IO) {
        val cacheKey = "${animeId}_$episodeNumber"
        streamCache[cacheKey]?.let { return@withContext it }

        val dynamicArchiveSources = fetchArchiveOrgAnimeStreams(animeTitle, episodeNumber)
        val combined = (dynamicArchiveSources + curatedOpenStreams(episodeNumber))
            .distinctBy { it.url }

        streamCache[cacheKey] = combined
        combined
    }

    private suspend fun fetchArchiveOrgAnimeStreams(
        animeTitle: String,
        episodeNumber: Int
    ): List<StreamSource> {
        return try {
            val cleanKeyword = animeTitle
                .replace(Regex("[^a-zA-Z0-9 ]"), " ")
                .trim()
                .split(Regex("\\s+"))
                .take(2)
                .joinToString(" ")
                .ifBlank { "anime" }

            val query = "mediatype:movies AND (subject:anime OR subject:animation) AND ($cleanKeyword OR anime)"
            val searchResp = archiveApi.searchAnimeVideos(query = query, rows = 5)
            val docs = searchResp.response?.docs.orEmpty()
            if (docs.isEmpty()) return emptyList()

            val targetDoc = docs[(episodeNumber - 1) % docs.size]
            val metadata = archiveApi.getItemMetadata(targetDoc.identifier)
            val mp4File = metadata.files.firstOrNull { file ->
                file.name.endsWith(".mp4", ignoreCase = true) &&
                    !file.name.contains("ia_thumb", ignoreCase = true)
            } ?: return emptyList()

            val encodedName = Uri.encode(mp4File.name, "/")
            val directUrl = "https://archive.org/download/${targetDoc.identifier}/$encodedName"
            listOf(
                StreamSource(
                    id = "archive_${targetDoc.identifier}",
                    label = "Server 1 • Archive.org",
                    provider = targetDoc.title?.take(28) ?: "Internet Archive",
                    quality = "HD MP4",
                    url = directUrl,
                    isHls = false
                )
            )
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun curatedOpenStreams(episodeNumber: Int): List<StreamSource> {
        val pool = listOf(
            StreamSource(
                id = "open_sintel",
                label = "Server 2 • Open Cinema HD",
                provider = "Blender Open Movie",
                quality = "1080p MP4",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                isHls = false
            ),
            StreamSource(
                id = "open_tears",
                label = "Server 3 • Fast CDN",
                provider = "Open Sci-Fi Stream",
                quality = "1080p MP4",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                isHls = false
            ),
            StreamSource(
                id = "hls_mux",
                label = "Server 4 • Adaptive HLS",
                provider = "Mux Adaptive Stream",
                quality = "Auto HLS",
                url = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                isHls = true
            ),
            StreamSource(
                id = "open_elephants",
                label = "Server 5 • Classic Animation",
                provider = "Open Studio MP4",
                quality = "720p MP4",
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                isHls = false
            )
        )
        val rotateOffset = (episodeNumber - 1).coerceAtLeast(0) % pool.size
        return pool.drop(rotateOffset) + pool.take(rotateOffset)
    }
}
