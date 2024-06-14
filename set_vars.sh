#!/bin/bash

# Ktor Deployment
export PORT=8080

# Database Configuration
export DB_URL=jdbc:postgresql://sparkline-db.c1y0c8y882lf.us-east-1.rds.amazonaws.com:5432/postgres
export DB_USER=sparklinefyi
export DB_PASSWORD=SuperSecure052624!
export DB_DRIVER=org.postgresql.Driver
export DB_MAX_POOL_SIZE=10

# Google Configuration
export GOOGLE_CLIENT_ID=846736523978-qmnn9ntfm1ccakv76778hsu9a4i8kb36.apps.googleusercontent.com
export GOOGLE_CLIENT_SECRET=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
export GOOGLE_API_KEY=AIzaSyC8Zj_Eh55VZOno2WuegS2QI_prHHfDi3A
export GOOGLE_REDIRECT_URI=http://localhost:8080/socials/auth/youtube
export GOOGLE_TOKEN_URL=https://oauth2.googleapis.com/token
export GOOGLE_USER_INFO_URL=https://openidconnect.googleapis.com/v1/userinfo

# Facebook Configuration
export FACEBOOK_CLIENT_ID=your-facebook-client-id
export FACEBOOK_CLIENT_SECRET=your-facebook-client-secret
export FACEBOOK_REDIRECT_URI=http://127.0.0.1:8080/auth/facebook/callback
export FACEBOOK_TOKEN_URL=https://graph.facebook.com/v9.0/oauth/access_token
export FACEBOOK_USER_INFO_URL=https://graph.facebook.com/v9.0/me

# Instagram Configuration
export INSTAGRAM_CLIENT_ID=486594670554364
export INSTAGRAM_CLIENT_SECRET=c4953d7d0d6771d0bace9d4d715647f2
export INSTAGRAM_REDIRECT_URI=https://sparkline.fyi/login
export INSTAGRAM_TOKEN_URL=https://api.instagram.com/oauth/access_token
export INSTAGRAM_USER_INFO_URL=https://graph.instagram.com/me

# Twitter Configuration
export TWITTER_CLIENT_ID=eW5iVmVDRFF2cmN2ZURvMjRheVQ6MTpjaQ
export TWITTER_CLIENT_SECRET=mVB7W0FbFBtmx2q1sw6MHrBnDlbxCKaPqbm7JYFPBEA7yHSgPu
export TWITTER_API_KEY=4q4TF56TtKWcS8FKSLBBhmi7A
export TWITTER_API_SECRET=hX3a80BHdlIQE9MP1v5F3O1S65njSr2eXjxORmQ1fRfjCa2Kx3
export TWITTER_ACCESS_TOKEN=1798206823297630208-goEa6g9lOrBR9Vk8yVgE5kO3rPSq4I
export TWITTER_ACCESS_TOKEN_SECRET=aj0mLmsjqDD86QJBIz9d3AHxlJta5jDsXK3Y1YsjEtnek
export TWITTER_REDIRECT_URL=https://sparkline.fyi/login

# LinkedIn Configuration
export LINKEDIN_CLIENT_ID=78d2xh3vvntptl
export LINKEDIN_CLIENT_SECRET=WPL_AP1.mzK0jqJavk31aWkc.WsD5Jg==
export LINKEDIN_REDIRECT_URI=http://sparkline.fyi/login
export LINKEDIN_TOKEN_URL=https://www.linkedin.com/oauth/v2/accessToken
export LINKEDIN_POST_URL=https://api.linkedin.com/v2/ugcPosts
export LINKEDIN_ANALYTICS_URL=https://api.linkedin.com/v2/organizationalEntityShareStatistics

# JWT Configuration
export JWT_SECRET=supersecret
export JWT_EXPIRATION=86400
export JWT_ISSUER=sparkline

echo "Environment variables have been set."
