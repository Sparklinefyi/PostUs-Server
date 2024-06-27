package postus.controllers

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.Base64

@Serializable
data class EmailRequest(
    val from: String,
    val to: List<String>,
    val subject: String,
    val text: String,
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
            <html>
                <body>
                    <p>Please confirm your email by clicking the link: <a href="$confirmLink">Confirm Email</a></p>
                </body>
            </html>
        """.trimIndent()

        val emailRequest = EmailRequest(
            from = "Nizar <noreply@resend.dev>",
            to = listOf(email),
            subject = "Confirm your email",
            text = htmlContent,
            headers = mapOf("X-Entity-Ref-ID" to "123"),
            attachments = emptyList()
        )

        val json = Json.encodeToString(emailRequest)

        val request = Request.Builder()
            .url("https://api.resend.com/emails")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), json))
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
