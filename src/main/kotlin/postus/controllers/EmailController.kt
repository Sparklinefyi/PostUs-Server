package postus.controllers

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

@Serializable
data class EmailRequest(
    val from: String,
    val to: List<String>,
    val subject: String,
    val html: String, // Changed to html
    val headers: Map<String, String>,
    val attachments: List<Attachment>
)

@Serializable
data class Attachment(
    val filename: String,
    val content: String
)

class EmailController {

    private val client = OkHttpClient()

    fun sendVerificationEmail(apiKey: String, email: String, token: String) {
        val confirmLink = "http://localhost:3000/new-verification?token=$token"
        val htmlContent = """
<!DOCTYPE html>
<html dir="ltr" lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="x-apple-disable-message-reformatting">
</head>
<body style="background-color:#ffffff; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;">

<table align="center" width="100%" border="0" cellpadding="0" cellspacing="0" role="presentation" style="max-width:37.5em; padding-left:12px; padding-right:12px; margin:0 auto;">
    <tbody>
        <tr style="width:100%;">
            <td>
                <h1 style="color:#333; font-size:24px; font-weight:bold; margin:40px 0; padding:0;">Login</h1>
                <a href="$confirmLink" style="color:#2754C5; text-decoration:underline; font-size:14px; display:block; margin-bottom:16px;" target="_blank">Click here to verify your email</a>
                <p style="font-size:14px; line-height:24px; margin:24px 0; color:#ababab;">If you didn't try to login, you can safely ignore this email.</p>
                <p style="font-size:14px; line-height:24px; margin:24px 0; color:#333;">LOGO</p>
                <p style="font-size:12px; line-height:22px; margin:16px 0; color:#898989;"><a href="https://sparkline.fyi" style="color:#898989; text-decoration:underline; font-size:14px;" target="_blank">sparkline.fyi</a>, expand your reach<br />with analytics and scheduling.</p>
            </td>
        </tr>
    </tbody>
</table>

</body>
</html>
        """.trimIndent()

        val emailRequest = EmailRequest(
            from = "Jacob <noreply@sparkline.fyi>",
            to = listOf(email),
            subject = "Confirm your email",
            html = htmlContent, // Changed to html
            headers = mapOf("X-Entity-Ref-ID" to "123"),
            attachments = emptyList()
        )

        val json = Json.encodeToString(emailRequest)

        val request = Request.Builder()
            .url("https://api.resend.com/emails")
            .post(json.toRequestBody("application/json".toMediaTypeOrNull()))
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    println("Email sent successfully")
                } else {
                    println("Failed to send email: ${response.body?.string()}")
                }
            }
        })
    }
}
