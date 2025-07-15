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
| `/api/files/{fileId}/move`     | [PATCH](#patch-apifilesfileidmove)                            |

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

---

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
  curl --location --request POST 'http://localhost:8080/api/users' --header 'Content-Type: application/json'
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

- **Authorization:**
  - **Bearer {Access Token}**

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
            "id": 3,
            "username": "Userlvn1"
        },
        {
            "id": 4,
            "username": "Userlhlvn1"
        },
        {
            "id": 5,
            "username": "Userjn1"
        },
        {
            "id": 6,
            "username": "Userva1"
        },
        {
            "id": 7,
            "username": "Usera123"
        },
        {
            "id": 8,
            "username": "Usera13"
        },
        {
            "id": 9,
            "username": "User143"
        },
        {
            "id": 10,
            "username": "User33433"
        },
        {
            "id": 11,
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

### GET /api/users/{id}

**Description:** Get user information by ID.

**Request:**

- **Path Parameter:** id – The ID of the user.

- **Authorization:**
  - **Bearer {Access Token}**

**Example:**

```shell
curl --location --request GET 'http://localhost:8080/api/users/2' \
--header 'Authorization: Bearer <token>'
```

**Success Response:**

- **Status Code:** 200 OK

  - **Content Type:** application/json
  - **Schema:**

```
  {
    "id": 2,
    "username": "BobTest",
    "email": "Bobtest@gmail.com",
    "locationType": "EUROPE",
    "costType": "MEDIUM"
}

```

**Error Responses:**

- 401 Unauthorized

- 404 Not Found – User not found

### GET /api/users/info

**Description:** Get the full user profile information of the authenticated user.

**Request:**

- **Query String:**
  - username (_String_,_Required_) – The target user's username.

* **Authorization:**
  - **Bearer {Access Token}**

**Example:**

```shell
curl --location --request GET 'http://localhost:8080/api/users/info' \
--header 'Authorization: Bearer <token>'
```

**Success Response:**

- **Status Code:** 200 OK

  - **Content Type:** application/json
  - **Schema:** (example only, depends on implementation)

```
  {
    "id": 6,
    "username": "Saraiva14343",
    "email": "testIs2343l@gmail.com",
    "locationType": "EUROPE",
    "costType": "HIGH"
}
```

**Error Responses:**

- 401 Unauthorized

- 404 Not Found – User not found

### GET /api/users/credentials

**Description:** Get credentials configuration info for the authenticated user.

**Request:**

- **Authorization:**
  - **Bearer {Access Token}**

**Example:**

```shell
curl --location --request GET 'http://localhost:8080/api/users/credentials' \
--header 'Authorization: Bearer <token>'
```

**Success Response:**

- **Status Code:** 201 Created
  - **Content:**: An object with the file ID
  - **Content Type:** application/json
  - **Schema:**

```
{
  "id": Integer
}
```

**Error Responses:**

- 401 Unauthorized

- 404 Not Found – User not found

### GET /api/users/storage

**Description:** Get cloud storage usage and capacity by provider for the authenticated user.

**Request:**

- **Authorization:**
  - **Bearer {Access Token}**

**Example:**

```shell
curl --location --request GET 'http://localhost:8080/api/users/storage' \
--header 'Authorization: Bearer <token>'
```

**Success Response:**

- **Status Code:** 200 OK

  - **Content Type:** application/json
  - **Schema:**

```
  {
    "totalSize": 3825151,
    "images": 3148149,
    "video": 0,
    "documents": 677002,
    "others": 0
}
```

**Error Responses:**

- 401 Unauthorized

- 404 Not Found – User not found

### GET /api/me

**Description:** Get minimal info (ID and username) for the authenticated user.

**Request:**

- **Authorization:**
  - **Bearer {Access Token}**

**Example:**

```shell
curl --location --request GET 'http://localhost:8080/api/me' \
--header 'Authorization: Bearer <token>'
```

**Success Response:**

- **Status Code:** 200 OK

  - **Content Type:** application/json
  - **Schema:**

```
 {
    "id": 2,
    "username": "BobTest"
 }
```

**Error Responses:**

- 401 Unauthorized

- 404 Not Found – User not found

### GET /api/users/notifications

**Description:** Open a Server-Sent Events (SSE) connection to receive real-time notifications.

**Request:**

- **Authorization:**
  - **Bearer {Access Token}**

**Example:**

```shell
curl --location --request GET 'http://localhost:8080/api/users/notifications' \
--header 'Authorization: Bearer <token>'
```

**Success Response:**

- **Status Code:** 200 OK

  - **Content Type:** text/event-stream
  - **Schema:**

```
event: invite
data: {
    "id": 0,
    "inviteId": 13,
    "status": "PENDING",
    "user": {
        "id": 6,
        "username": "Saraiva14343",
        "email": "testIs2343l@gmail.com",
    },
    "folderId": 38,
    "folderName": "ISEL1234"
}
```

**Error Responses:**

- 401 Unauthorized

---

# Files

### POST /api/files

**Description:** Uploads a file for the authenticated user. Encryption is optional.

**Request:**

- **Content Type:** multipart/form-data

- **Authorization:**

  - **Bearer {Access Token}**

- **Form Parameters:**

  - **file:** File to upload (MultipartFile)

  - **mimeType:**: MIME type (e.g., application/pdf)

  - **encryption:**: true or false

  - **encryptedKey:**: Encrypted key (base64)

**Example:**

```shell
curl --location --request POST 'http://localhost:8080/api/files' \
--header 'Authorization: Bearer <token>' \
--form 'file=@"/path/to/file.pdf"' \
--form 'mimeType="application/pdf"' \
--form 'encryption="true"' \
--form 'encryptedKey="base64encodedkey"'

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

- **Headers:**

  - Location: URI of the created file (e.g., /api/files/{fileId})

**Error Responses:**

- 400 Bad Request – Invalid creation storage

- 400 Bad Request – Invalid file name

- 404 Not Found – Parent folder not found

- 400 Bad Request – Encryption not supported in shared folder

- 500 Internal Server Error – Invalid creation global bucket

- 500 Internal Server Error – Invalid create context

- 500 Internal Server Error – Invalid credential

### GET /api/files

**Description:** Retrieves a paginated list of files for
the authenticated user, with optional search, filtering by folder type, and sorting.

**Request:**

- **Query Params**

  - search (optional): String to search files

  - size (optional, default: 10): Number of items per page

  - page (optional, default: 0): Page number

  - type (optional): FolderType (e.g., PRIVATE, SHARED)

  - sort (optional, default: created_asc): Sorting criteria

- **Authorization:**

  - **Bearer {Access Token}**

**Example:**

```shell
curl --location --request GET 'http://localhost:8088/api/files?size=3&page=1' \
--header 'Authorization: Bearer <token>'


```

**Success Response:**

- **Status Code:** 200 OK

  - **Content Type:** application/json (paginated list of file info)

  - **Schema Example:**

  ```
    {
    "content": [
        {
            "fileId": 26,
            "user": {
                "id": 2,
                "username": "BobTest",
                "email": "Bobtest@gmail.com"
            },
            "folderInfo": null,
            "name": "Cat23.jpg",
            "size": 122667,
            "contentType": "image/jpeg",
            "createdAt": 1752485022,
            "encryption": false,
            "url": null
        },
        {
            "fileId": 27,
            "user": {
                "id": 2,
                "username": "BobTest",
                "email": "Bobtest@gmail.com"
            },
            "folderInfo": null,
            "name": "Cat12334.jpg",
            "size": 122667,
            "contentType": "image/jpeg",
            "createdAt": 1752485035,
            "encryption": false,
            "url": null
        },
        {
            "fileId": 28,
            "user": {
                "id": 2,
                "username": "BobTest",
                "email": "Bobtest@gmail.com"
            },
            "folderInfo": null,
            "name": "Cat4.jpg",
            "size": 122667,
            "contentType": "image/jpeg",
            "createdAt": 1752485049,
            "encryption": false,
            "url": null
        }
    ],
    "pageable": {
        "sort": {
            "sorted": true,
            "unsorted": false
        },
        "pageNumber": 1,
        "pageSize": 3,
        "offset": 3
    },
    "totalElements": 21,
    "totalPages": 7,
    "last": false,
    "first": false,
    "size": 3,
    "number": 1
  }
  ```

**Error Responses:**

- 401 Unauthorized – Unauthorized Request

- 400 Bad Request – Invalid request content

### GET /api/files/{fileId}

**Description:** Retrieves detailed information about a specific file by ID.

**Request:**

- **Path Parameter:**: fileId – The ID of the file (must be positive integer)

- **Authorization:**
  - **Bearer {Access Token}**

**Example:**

```shell
curl --location --request GET 'http://localhost:8080/api/files/1' \
--header 'Authorization: Bearer <token>'
```

**Success Response:**

- **Status Code:** 200 OK

  - **Content Type:** application/json
  - **Schema:**

```
{
    "fileId": 1,
    "user": {
        "id": 2,
        "username": "BobTest",
        "email": "Bobtest@gmail.com"
    },
    "folderInfo": null,
    "name": "Cat23.jpg",
    "size": 122667,
    "contentType": "image/jpeg",
    "createdAt": 1752485022,
    "encryption": false,
    "url": null
}
```

**Error Responses:**

- 401 Unauthorized – Unauthorized Request

- 404 Not Found – File not found

- 400 Bad Request – Invalid request content (e.g., invalid file ID)

### POST /api/files/{fileId}/temp-url

**Description:** Generates a temporary URL for accessing a file.

**Request:**

- **Path Parameter:**: fileId – The ID of the file (must be positive integer)

- **Authorization:**

  - **Bearer {Access Token}**

- **Content:** An object with expiration time
  - **Content Type:** application/json
  - **Schema:**

```
{
   "expiresIn": Integer (minutes)
}
```

**Example:**

```shell
curl --location --request POST 'http://localhost:8080/api/files/1/temp-url' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <token>' \
--data-raw '{ "expiresIn": 15 }'
```

**Success Response:**

- **Status Code:** 200 OK

  - **Content:** An object with the token value
  - **Content Type:** application/json

  - **Schema:**

  ```
   {
    "fileId": 1,
    "user": {
        "id": 2,
        "username": "BobTest",
        "email": "Bobtest@gmail.com"
    },
    "folderInfo": null,
    "name": "Cat23.jpg",
    "size": 122667,
    "contentType": "image/jpeg",
    "createdAt": 1752485022,
    "encryption": false,
    "url": "https://storage.googleapis.com/xxxx/xxxxx/Cat23.jpg?X-Goog-Algorithm=xxxxxxxxx"
  }
  ```

**Error Responses:**

- 404 Not Found – File not found

- 400 Bad Request – File is encrypted

- 400 Bad Request – Folder is shared

- 400 Bad Request – Invalid request content (e.g., invalid file ID)

- 500 Internal Server Error – Invalid create context

- 500 Internal Server Error – Invalid creation global bucket

- 500 Internal Server Error – Invalid credential

### PATCH /api/files/{fileId}/move

**Description:** Moves a file to a different folder. If the folderId is not provided in the request body, the file is moved to the root of the authenticated user's storage.

**Request:**

- **Path Parameter:**: fileId – The ID of the file (must be positive integer)

- **Authorization:**

  - **Bearer {Access Token}**

- **Content:** An object with target folder ID
  - **Content Type:** application/json
  - **Schema:**

```
{
  "folderId": Integer (optional – if omitted, moves the file to the user's root storage)
}
```

**Example:**

```shell
curl --location --request PATCH 'http://localhost:8080/api/files/1/move' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <token>' \
--data-raw '{ "folderId": 2 }'
```

**Example (moving to root – omit folderId):**

```shell
curl --location --request PATCH 'http://localhost:8080/api/files/1/move' \
--header 'Content-Type: application/json' \
--header 'Authorization: Bearer <token>' \
--data-raw '{}'
```

**Success Response:**

- **Status Code:** 200 OK

  - **Content:** An object with the token value
  - **Content Type:** application/json

**Error Responses:**

- 404 Not Found – File not found
- 400 Bad Request – Folder is shared

- 400 Bad Request – Invalid request content (e.g., invalid file ID)

- 400 Bad Request – Invalid file name

- 404 Not Found – Folder not found

- 500 Internal Server Error – Invalid create context

- 500 Internal Server Error – Invalid creation global bucket

- 500 Internal Server Error – Invalid credential

- 500 Internal Server Error – Invalid blob creation

### GET /api/files/{fileId}/download

**Description:** Downloads a file by ID. The response includes the file's content and metadata. For encrypted files, an encrypted file key is provided, which can only be decrypted using the client's master key (stored exclusively on the user's device for security).

**Request:**

- **Path Parameter:**: fileId – The ID of the file (must be positive integer)

- **Authorization:**
  - **Bearer {Access Token}**

**Example:**

```shell
curl --location --request GET 'http://localhost:8080/api/files/1/download' \
--header 'Authorization: Bearer <token>'
```

**Success Response:**

- **Status Code:** 200 OK

  - **Content Type:** application/json (with file content and metadata)
  - **Schema (Non-Encrypted File):**

```
{
    "file": {
        "fileContent: "base64encodedfilecontent"
        "fileName": "Cat23.jpg",
        "mimeType": "image/jpeg",
        "encrypted": false
    },
    "fileKeyEncrypted": null
}
```

- **Schema (Encrypted File):**

```
{
    "file": {
        "fileContent: "base64encodedfilecontent"
       "fileName": "Isel12323.pdf",
        "mimeType": "application/pdf",
        "encrypted": true
    },
    "fileKeyEncrypted": "NNW8t04F4Zqg3rAYaYMPXiPDpyvxo+LdViOljtHxpEhpUcwD3uADmvhmLOeoakerVFax+dwLjVlsztWD"
}
```

- **Additional Notes:**

  - **fileContent:** The actual file data encoded in Base64 format, ready for decoding and use on the client side.

  - **fileKeyEncrypted:** (Present only for encrypted files) This is the encrypted representation of the file's encryption key. It can only be decrypted using the client's master key, which is securely stored solely on the user's device and never transmitted to or stored on the server.

**Error Responses:**

- 400 Bad Request – Invalid download file

- 404 Not Found – File not found

- 400 Bad Request – Invalid key

- 404 Not Found – Parent folder not found

- 400 Bad Request – Invalid request content (e.g., invalid file ID)

- 500 Internal Server Error – Invalid create context

- 500 Internal Server Error – Invalid creation global bucket

- 500 Internal Server Error – Invalid credential

### DELETE /api/files/{fileId}

**Description:** Deletes a file by ID.

**Request:**

- **Path Parameter:**: fileId – The ID of the file (must be positive integer)

- **Authorization:**
  - **Bearer {Access Token}**

**Example:**

```shell
curl --location --request DELETE 'http://localhost:8080/api/files/1' \
--header 'Authorization: Bearer <token>'
```

**Success Response:**

- **Status Code:** 200 OK

**Error Responses:**

- 404 Not Found – File not found

- 400 Bad Request – Invalid delete file

- 404 Not Found – Parent folder not found

- 403 Forbidden – User not permissions type

- 400 Bad Request – Invalid request content (e.g., invalid file ID)

- 500 Internal Server Error – Invalid credential

- 500 Internal Server Error – Invalid create context

- 500 Internal Server Error – Invalid creation global bucket
