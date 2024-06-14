package postus.models

import kotlinx.serialization.Serializable
import postus.models.instagram.InstagramPostRequest
import postus.models.youtube.YoutubeUploadRequest

/*This is going to be a monster of a class as we grow,
 we need to ask for all possible data to post to any platform
 we offer, it will all be nullable though as users
 can schedule a post for just 1
*/

@Serializable
data class SchedulePostRequest(
    val providers: List<String>,
    val mediaType: String,
    val instagramPostRequest: InstagramPostRequest?,
    val youtubePostRequest: YoutubeUploadRequest?
)