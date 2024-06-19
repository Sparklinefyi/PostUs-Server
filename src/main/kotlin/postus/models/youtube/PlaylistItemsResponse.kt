package postus.models.youtube

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistItem(
    val kind: String?,
    val etag: String?,
    val id: String?,
    val snippet: Snippet?,
)

@Serializable
data class PlaylistItemsResponse(
    val kind: String?,
    val etag: String?,
    val items: List<PlaylistItem>?,
    val pageInfo: PageInfo?
)

@Serializable
data class VideoItem(
    val kind: String?,
    val etag: String?,
    val id: String?,
    val snippet: Snippet?,
    val statistics: Statistics?,
    val contentDetails: ContentDetails? // Add statistics here
)

@Serializable
data class VideoItemResponse(
    val kind: String?,
    val etag: String?,
    val items: List<VideoItem>?,
    val pageInfo: PageInfo?
)

@Serializable
data class ContentDetails(
    val duration: String?,
    val dimension: String?,
    val definition: String?
)

@Serializable
data class Snippet(
    val publishedAt: String?,
    val channelId: String?,
    val title: String?,
    val description: String?,
    val thumbnails: Thumbnails?,
    val channelTitle: String?,
    val playlistId: String?,
    val position: Int?,
    val resourceId: ResourceId?,
    val videoOwnerChannelTitle: String?,
    val videoOwnerChannelId: String?
)

@Serializable
data class Thumbnail(
    val url: String?,
    val width: Int?,
    val height: Int?
)

@Serializable
data class Thumbnails(
    val default: Thumbnail?,
    val medium: Thumbnail?,
    val high: Thumbnail?,
    val standard: Thumbnail? = null,
    val maxres: Thumbnail? = null
)

@Serializable
data class ResourceId(
    val kind: String?,
    val videoId: String?
)

@Serializable
data class PageInfo(
    val totalResults: Int?,
    val resultsPerPage: Int?
)

@Serializable
data class Statistics(
    val viewCount: String?,
    val likeCount: String?,
    val dislikeCount: String?,
    val favoriteCount: String?,
    val commentCount: String?
)