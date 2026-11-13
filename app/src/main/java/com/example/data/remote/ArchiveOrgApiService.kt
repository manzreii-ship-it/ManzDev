package com.example.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class ArchiveSearchResponse(
    val response: ArchiveDocsContainer? = null
)

@JsonClass(generateAdapter = true)
data class ArchiveDocsContainer(
    val docs: List<ArchiveDocDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ArchiveDocDto(
    val identifier: String = "",
    val title: String? = null
)

@JsonClass(generateAdapter = true)
data class ArchiveMetadataResponse(
    val files: List<ArchiveFileDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ArchiveFileDto(
    val name: String = "",
    val format: String? = null,
    val size: String? = null
)

interface ArchiveOrgApiService {
    @GET("advancedsearch.php")
    suspend fun searchAnimeVideos(
        @Query("q") query: String,
        @Query("fl[]") fields: List<String> = listOf("identifier", "title"),
        @Query("rows") rows: Int = 10,
        @Query("page") page: Int = 1,
        @Query("output") output: String = "json"
    ): ArchiveSearchResponse

    @GET("metadata/{identifier}")
    suspend fun getItemMetadata(
        @Path("identifier") identifier: String
    ): ArchiveMetadataResponse
}
