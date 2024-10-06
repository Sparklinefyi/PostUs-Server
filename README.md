# PostUs-Server - Kotlin Backend for Sparkline Social Media Scheduling Tool

PostUs-Server is the Kotlin-based backend for the Sparkline social media scheduling tool. It provides authentication endpoints for the iOS app and handles media uploads via AWS S3 buckets. The backend supports posting content to multiple social media platforms, scheduling posts, and managing user data.

## Features

- **User Authentication**: Manages user registration, login, and email verification, integrated with the iOS app.
- **Media Upload**: Uses AWS S3 to upload and store images and videos.
- **Social Media Integration**: Supports posting content to TikTok, Instagram, YouTube, Twitter, and LinkedIn.
- **Post Scheduling**: Allows users to schedule posts across multiple platforms.
- **Analytics**: Retrieves analytics data from platforms like Instagram, YouTube, and TikTok.
  
## Prerequisites

- Kotlin 1.6+
- AWS S3 for media storage
- MongoDB (or any other database compatible with Prisma)
- Auth0 for authentication
- Docker (optional, for containerized development)

## Installation

1. Clone the repository:

    ```bash
    git clone https://github.com/Sparklinefyi/PostUs-Server.git
    ```

2. Navigate to the project directory:

    ```bash
    cd PostUs-Server
    ```

3. Set up environment variables. Create a `.env` file in the project root with the following:

    ```bash
    DATABASE_URL=<your-database-url>
    AUTH0_DOMAIN=<your-auth0-domain>
    AUTH0_CLIENT_ID=<your-auth0-client-id>
    AUTH0_CLIENT_SECRET=<your-auth0-client-secret>
    AWS_ACCESS_KEY_ID=<your-aws-access-key-id>
    AWS_SECRET_ACCESS_KEY=<your-aws-secret-access-key>
    S3_BUCKET_NAME=<your-s3-bucket-name>
    ```

4. Build and run the server:

    ```bash
    ./gradlew build
    ./gradlew run
    ```

5. The server will start at `http://localhost:8080`.

## API Endpoints

### Authentication

- **POST /auth/register**: Registers a new user and sends a verification email.
- **POST /auth/signin**: Logs in a user with email and password.
- **POST /auth/signout**: Logs out the user.
- **POST /auth/test-email**: Sends a test email for verification.

### User Management

- **POST /user/info**: Retrieves user information based on the provided token.
- **POST /user/update**: Updates user profile information, including passwords.

### Media Management (AWS S3)

- **POST /socials/upload/image**: Uploads an image to AWS S3.
- **POST /socials/upload/video**: Uploads a video to AWS S3.
- **GET /socials/list/images**: Lists all images uploaded by the user.
- **GET /socials/list/videos**: Lists all videos uploaded by the user.

### Social Media Posting

- **POST /socials/publish/image/instagram**: Publishes an image to Instagram.
- **POST /socials/publish/video/tiktok**: Publishes a video to TikTok.
- **POST /socials/publish/video/instagram**: Publishes a video to Instagram.
- **POST /socials/publish/video/youtube**: Publishes a video to YouTube.
- **POST /socials/publish/twitter**: Posts content to Twitter.
- **POST /socials/publish/linkedin**: Publishes a post to LinkedIn.

### Analytics

- **POST /socials/analyze/youtube/page**: Retrieves YouTube page analytics.
- **POST /socials/analyze/youtube/post**: Retrieves YouTube post analytics.
- **GET /socials/analyze/instagram/page**: Retrieves Instagram page analytics.
- **GET /socials/analyze/instagram/post**: Retrieves Instagram post analytics.
- **GET /socials/analyze/tiktok/page**: Retrieves TikTok page analytics.
- **GET /socials/analyze/tiktok/post**: Retrieves TikTok post analytics.

## Usage

- You can test the API endpoints using tools like [Postman](https://www.postman.com/) or [cURL](https://curl.se/).
- Media uploads (image/video) are stored on AWS S3, and the corresponding URLs are returned for further use.

## AWS S3 Integration

To configure AWS S3 for media uploads, ensure the following:

1. Set your `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, and `S3_BUCKET_NAME` in your `.env` file.
2. Ensure your S3 bucket has the correct permissions to allow uploads from the backend.
3. Use the provided `/upload` endpoints to store media on S3.

## Contributing

Contributions are welcome! To contribute:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature/your-feature`).
3. Make your changes.
4. Push to the branch (`git push origin feature/your-feature`).
5. Open a Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contact

If you have any questions, feel free to reach out:

- Email: j.kennedy092420@gmail.com
- GitHub: [ibrood-a](https://github.com/ibrood-a)
