
### Multi-Cloud Guardian API



* The API provides endpoints for managing **users**, **files**, and **folders**.  
  * All `GET` endpoints that return collections support **pagination** (using `page`, `size`, and optionally `sort` query parameters).  

For the complete list of API endpoint paths, see the [**Uris**](../multicloud-guardian/http/src/main/kotlin/pt/isel/leic/multicloudguardian/http/Uris.kt) file, which centralizes all route constants used throughout the backend.



**API endpoints** are organized as follows:

## Users 

#### [*UsersController*](../multicloud-guardian/http/src/main/kotlin/pt/isel/leic/multicloudguardian/http/controllers/UsersController.kt)

| Endpoint                                      | HTTP Methods                                                                                  |
|-----------------------------------------------|-----------------------------------------------------------------------------------------------|
| `/api/users`                                  | [POST](#post-apiusers), [GET](#get-apiusers)                                                  |
| `/api/users/{id}`                             | [GET](#get-apiusersid)                                                                        |
| `/api/users/info`                             | [GET](#get-apiusersinfo)                                                                      |
| `/api/users/credentials`                      | [GET](#get-apiuserscredentials)                                                               |
| `/api/users/storage`                          | [GET](#get-apiusersstorage)                                                                   |
| `/api/me`                                     | [GET](#get-apime)                                                                             |
| `/api/users/notifications`                    | [GET](#get-apiusersnotifications)                                                             |
| `/api/users/token`                            | [POST](#post-apiuserstoken)                                                                   |
| `/api/logout`                                 | [POST](#post-apilogout)                                                                       |


## Files

#### [*FilesController*](../multicloud-guardian/http/src/main/kotlin/pt/isel/leic/multicloudguardian/http/controllers/FilesController.kt)


| Endpoint                                      | HTTP Methods                                                                                  |
|-----------------------------------------------|-----------------------------------------------------------------------------------------------|
| `/api/files`                                  | [GET](#get-apifiles), [POST](#post-apifiles)                                                  |
| `/api/files/{fileId}`                         | [GET](#get-apifilesfileid), [DELETE](#delete-apifilesfileid)                                  |
| `/api/files/{fileId}/temp-url`                | [POST](#post-apifilesfileidtemp-url)                                                          |
| `/api/files/{fileId}/download`                | [GET](#get-apifilesfileiddownload)                                                            |
| `/api/files/{fileId}/move`                    | [PATCH](#post-apifilesfileidmove)                                                             |


## Folders


#### [*FoldersController*](../multicloud-guardian/http/src/main/kotlin/pt/isel/leic/multicloudguardian/http/controllers/FoldersController.kt)


| Endpoint                                      | HTTP Methods                                                                                  |
|-----------------------------------------------|-----------------------------------------------------------------------------------------------|
| `/api/folders`                                | [GET](#get-apifolders), [POST](#post-apifolders)                                              |
| `/api/folders/{folderId}`                     | [GET](#get-apifoldersfolderid), [POST](#post-apifoldersfolderid), [DELETE](#delete-apifoldersfolderid) |
| `/api/folders/{folderId}/folders`             | [GET](#get-apifoldersfolderidfolders)                                                         |
| `/api/folders/{folderId}/files`               | [GET](#get-apifoldersfolderidfiles), [POST](#post-apifoldersfolderidfiles)                   |
| `/api/folders/{folderId}/files/{fileId}`      | [GET](#get-apifoldersfolderidfilesfileid), [DELETE](#delete-apifoldersfolderidfilesfileid)   |
| `/api/folders/{folderId}/files/{fileId}/download` | [GET](#get-apifoldersfolderidfilesfileiddownload)                                         |
| `/api/folders/{folderId}/invites`             | [POST](#post-apifoldersfolderidinvites)                                                       |
| `/api/folders/{folderId}/invites/{inviteId}`  | [GET](#get-apifoldersfolderidinvitesinviteid)                                                 |
| `/api/folders/invites/received`               | [GET](#get-apifoldersinvitesreceived)                                                         |
| `/api/folders/invites/sent`                   | [GET](#get-apifoldersinvitessent)                                                             |
| `/api/folders/{folderId}/leave`               | [POST](#post-apifoldersfolderidleave)                                                         |

# Users 

### POST /api/users

**Description:** Create a new user account.

**Request:**

- **Content:** An object with the information of the player
    - **Content Type:** application/json
    - **Schema:**


````
{
  "username": String,
  "email": String,
  "password": String,
  "salt": String,
  "iterations": Integer,
  "costType": "LOW" | "MEDIUM" | "HIGH",
  "locationType": "NORTH_AMERICA" | "SOUTH_AMERICA" | "EUROPE" | "OTHERS"
}
````
**Example:**

```shell
  curl --location --request GET 'http://localhost:8080/api/users' --header 'Content-Type: application/json' 
--data-raw '{ "username": "user1", "email": "user1@example.com", "password": "SecureP_123", "salt": "15435435435329",
"iterations": 15100, "costType": "HIGH","locationType": "EUROPE"}'
```

**Success Response:**

- **Status Code:** 201 Created
    - **Content:** An object with the information of the player
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
