### Multi-Cloud Guardian API

- The API provides endpoints for managing **users**, **files**, and **folders**.
  - All `GET` endpoints that return collections support **pagination** (using `page`, `size`, and optionally `sort` query parameters).

For the complete list of API endpoint paths, see the [**Uris**](../multicloud-guardian/http/src/main/kotlin/pt/isel/leic/multicloudguardian/http/Uris.kt) file, which centralizes all route constants used throughout the backend.

**API endpoints** are organized as follows:

## Users

#### [_UsersController_](../multicloud-guardian/http/src/main/kotlin/pt/isel/leic/multicloudguardian/http/controllers/UsersController.kt)

| Endpoint                   | HTTP Methods                                 |
| -------------------------- | -------------------------------------------- |
| `/api/users`               | [POST](#post-apiusers), [GET](#get-apiusers) |
| `/api/users/{id}`          | [GET](#get-apiusersid)                       |
| `/api/users/info`          | [GET](#get-apiusersinfo)                     |
| `/api/users/credentials`   | [GET](#get-apiuserscredentials)              |
| `/api/users/storage`       | [GET](#get-apiusersstorage)                  |
| `/api/me`                  | [GET](#get-apime)                            |
| `/api/users/notifications` | [GET](#get-apiusersnotifications)            |
| `/api/users/token`         | [POST](#post-apiuserstoken)                  |
| `/api/logout`              | [POST](#post-apilogout)                      |

## Files

#### [_FilesController_](../multicloud-guardian/http/src/main/kotlin/pt/isel/leic/multicloudguardian/http/controllers/FilesController.kt)

| Endpoint                       | HTTP Methods                                                 |
| ------------------------------ | ------------------------------------------------------------ |
| `/api/files`                   | [GET](#get-apifiles), [POST](#post-apifiles)                 |
| `/api/files/{fileId}`          | [GET](#get-apifilesfileid), [DELETE](#delete-apifilesfileid) |
| `/api/files/{fileId}/temp-url` | [POST](#post-apifilesfileidtemp-url)                         |
| `/api/files/{fileId}/download` | [GET](#get-apifilesfileiddownload)                           |
| `/api/files/{fileId}/move`     | [PATCH](#post-apifilesfileidmove)                            |

## Folders

#### [_FoldersController_](../multicloud-guardian/http/src/main/kotlin/pt/isel/leic/multicloudguardian/http/controllers/FoldersController.kt)

| Endpoint                                          | HTTP Methods                                                                                           |
| ------------------------------------------------- | ------------------------------------------------------------------------------------------------------ |
| `/api/folders`                                    | [GET](#get-apifolders), [POST](#post-apifolders)                                                       |
| `/api/folders/{folderId}`                         | [GET](#get-apifoldersfolderid), [POST](#post-apifoldersfolderid), [DELETE](#delete-apifoldersfolderid) |
| `/api/folders/{folderId}/folders`                 | [GET](#get-apifoldersfolderidfolders)                                                                  |
| `/api/folders/{folderId}/files`                   | [GET](#get-apifoldersfolderidfiles), [POST](#post-apifoldersfolderidfiles)                             |
| `/api/folders/{folderId}/files/{fileId}`          | [GET](#get-apifoldersfolderidfilesfileid), [DELETE](#delete-apifoldersfolderidfilesfileid)             |
| `/api/folders/{folderId}/files/{fileId}/download` | [GET](#get-apifoldersfolderidfilesfileiddownload)                                                      |
| `/api/folders/{folderId}/invites`                 | [POST](#post-apifoldersfolderidinvites)                                                                |
| `/api/folders/{folderId}/invites/{inviteId}`      | [GET](#get-apifoldersfolderidinvitesinviteid)                                                          |
| `/api/folders/invites/received`                   | [GET](#get-apifoldersinvitesreceived)                                                                  |
| `/api/folders/invites/sent`                       | [GET](#get-apifoldersinvitessent)                                                                      |
| `/api/folders/{folderId}/leave`                   | [POST](#post-apifoldersfolderidleave)                                                                  |

# Users

### POST /api/users

**Description:** Create a new user account.

**Request:**

- **Content:** An object with the user information
  - **Content Type:** application/json
  - **Schema:**

```
{
  "username": String,
  "email": String,
  "password": String,
  "salt": String,
  "iterations": Integer,
  "costType": "LOW" | "MEDIUM" | "HIGH",
  "locationType": "NORTH_AMERICA" | "SOUTH_AMERICA" | "EUROPE" | "OTHERS"
}
```

**Example:**

```shell
  curl --location --request GET 'http://localhost:8080/api/users' --header 'Content-Type: application/json'
--data-raw '{ "username": "user1", "email": "user1@example.com", "password": "SecureP_123", "salt": "15435435435329",
"iterations": 15100, "costType": "HIGH","locationType": "EUROPE"}'
```

**Success Response:**

- **Status Code:** 201 Created

  - **Content:** An object with the user ID
  - **Content Type:** application/json

  - **Schema:**

  ```
    {
        "id": Integer,
    }
  ```

**Error Responses:**

- 400 Bad Request – Insecure password

- 400 Bad Request – Username already exists

- 400 Bad Request – Email already exists

### POST /api/users/token

**Description:** Create a new authentication token for a user (login).

**Request:**

- **Content:** An object with login credentials
  - **Content Type:** application/json
  - **Schema:**

```
{
  "username": String,
  "password": String
}
```

**Headers:**

- User-Agent: String (required for token creation)

**Example:**

```shell
  curl --location --request POST 'http://localhost:8080/api/users/token' --header 'Content-Type: application/json' --header 'User-Agent: Mozilla/5.0' \
--data-raw '{ "username": "user1", "password": "SecureP_123" }'

```

**Success Response:**

- **Status Code:** 200 OK

  - **Content:** An object with the token value
  - **Content Type:** application/json

  - **Schema:**

  ```
    {
      "token": String
    }
  ```

**Headers:**

- Set-Cookie: Sets HttpOnly cookies for token and login with SameSite=Strict and Max-Age based on token expiration.

**Error Responses:**

- 400 Bad Request – Username not found

- 400 Bad Request – Invalid token

- 400 Bad Request – Password does not match

- 400 Bad Request – Insecure password

### GET /api/users

**Description:** Search users by username (paginated).

**Request:**

- **Query Params:** username (required), page (optional, default: 0), size (optional, default: 10)

- **Authentication:** Required

**Example:**

```shell
  curl --location --request GET 'http://localhost:8080/api/users?username=User&size=10&page=0' \
--header 'Authorization: Bearer <token>'

```

**Success Response:**

- **Status Code:** 200 OK

  - **Content Type:** application/json (paginated list of users with id and username)
  - **Body Example:**

```
  {
    "content": [
        {
            "id": 2,
            "username": "BobUser"
        },
        {
            "id": 14,
            "username": "Userlvn1"
        },
        {
            "id": 15,
            "username": "Userlhlvn1"
        },
        {
            "id": 16,
            "username": "Userjn1"
        },
        {
            "id": 10,
            "username": "Userva1"
        },
        {
            "id": 4,
            "username": "Usera123"
        },
        {
            "id": 5,
            "username": "Usera13"
        },
        {
            "id": 7,
            "username": "User143"
        },
        {
            "id": 9,
            "username": "User33433"
        },
        {
            "id": 6,
            "username": "User343"
        }
    ],
    "pageable": {
        "sort": {
            "sorted": true,
            "unsorted": false
        },
        "pageNumber": 0,
        "pageSize": 10,
        "offset": 0
    },
    "totalElements": 19,
    "totalPages": 2,
    "last": false,
    "first": true,
    "size": 10,
    "number": 0
  }

```
**Error Responses:**

- 401 Unauthorized

- 400 Bad Request (Invalid parameters)
