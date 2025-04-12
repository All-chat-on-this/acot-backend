# acot-backend
后端

# JWT Authentication with Social Login

This project implements JWT-based authentication with support for social login using QQ and WeChat platforms.

## Features

- JWT-based authentication
- Username/password login
- Social login with QQ and WeChat
- Binding social accounts to existing users
- Token validation

## Authentication Flow

### Regular Login (Username/Password)

1. Client sends username and password to the server
2. Server validates credentials and generates a JWT token
3. Server returns token and user information
4. Client uses the token for subsequent API requests

### Social Login (QQ and WeChat)

1. Client requests an authorization URL for the desired platform
2. User is redirected to the platform's OAuth page
3. After user authorization, the platform redirects back to the application with an authorization code
4. Server exchanges the code for user information
5. If user exists, server returns a JWT token
6. If user doesn't exist, server creates a new user and returns a JWT token

## API Endpoints

### Login with Username and Password

```
POST /api/auth/login
```

Parameters:
- `username`: User's username
- `password`: User's password

### Get Social Authorization URL

```
GET /api/social/authorize
```

Parameters:
- `type`: Social platform type (32 for WeChat, 36 for QQ)
- `redirect_uri`: URL to redirect after successful authorization

### Social Login Callback

```
GET /api/auth/social/callback
```

Parameters:
- `type`: Social platform type (32 for WeChat, 36 for QQ)
- `code`: Authorization code from the social platform
- `state`: State parameter for verification

### Bind Social Account

```
POST /api/auth/social/bind
```

Parameters:
- `userId`: ID of the user to bind the social account to
- `type`: Social platform type (32 for WeChat, 36 for QQ)
- `code`: Authorization code from the social platform
- `state`: State parameter for verification

### Validate Token

```
GET /api/auth/validate
```

Parameters:
- `token`: JWT token to validate

## Response Format

All API responses follow this format:

```json
{
  "code": 0,       // 0 for success, other values for errors
  "message": "success",
  "data": {        // Response data
    // Varies depending on the endpoint
  }
}
```

## Login Response Example

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": 1,
    "username": "user1",
    "nickname": "User One",
    "loginType": 0,
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "expiresIn": 86400000,
    "isNewUser": false
  }
}
```

## Configuration

Configuration for JWT and social login can be found in `application.yml`:

```yaml
# JWT Configuration
jwt:
  secret: your-jwt-secret
  expiration: 86400000  # 24 hours in milliseconds

# Social Login Configuration
justauth:
  enabled: true
  # QQ configuration
  type:
    QQ:
      client-id: your-qq-client-id
      client-secret: your-qq-client-secret
      redirect-uri: http://your-domain/api/auth/social/callback?type=36
  # WeChat configuration
    WECHAT_OPEN:
      client-id: your-wechat-client-id
      client-secret: your-wechat-client-secret
      redirect-uri: http://your-domain/api/auth/social/callback?type=32
  cache:
    type: default
```

## Security

- JWT tokens are signed with a secure key
- Passwords are encrypted using BCrypt
- Token expiration is configurable
- Stateless authentication (no session)

## Usage in Frontend

Add the JWT token to the Authorization header for protected API requests:

```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```
