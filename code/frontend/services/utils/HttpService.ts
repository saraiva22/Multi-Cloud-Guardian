import { MEDIA_TYPE_PROBLEM, Problem } from "../media/Problem";

const LOCAL_IP = "192.168.1.71";
const PORT = "8088";

export const PREFIX_API = `http://${LOCAL_IP}:${PORT}/api`;

export const apiRoutes = {
  // Users
  REGISTER_USER: "/users",
  LOGIN: "/users/token",
  LOGOUT: "/logout",
  GET_USER_BY_ID: "/users/:id",
  GET_USER_BY_USERNAME: "/users/info",
  GET_USER_CREDENTIALS: "/users/credentials",
  GET_STORAGE_DETAILS: "/users/storage",
  SEARCH_USERS: "/users",
  HOME: "/me",
  NOTIFICATIONS: "/users/notifications",

  // Files
  UPLOAD_FILE: "/files",
  GET_FILES: "/files",
  GET_FILE_BY_ID: "/files/:id",
  CREATE_TEMP_URL: "/files/:id/temp-url",
  DOWNLOAD_FILE: "/files/:id/download",
  DELETE_FILE: "/files/:id",

  // Folders
  CREATE_FOLDER: "/folders",
  CREATE_FOLDER_IN_FOLDER: "/folders/:id",
  GET_FOLDERS: "/folders",
  GET_FOLDER_BY_ID: "/folders/:id",
  GET_FOLDERS_IN_FOLDER: "/folders/:id/folders",
  GET_FILES_IN_FOLDER: "/folders/:id/files",
  GET_FILE_IN_FOLDER: "/folders/:folderId/files/:fileId",
  UPLOAD_FILE_IN_FOLDER: "/folders/:id/files",
  DOWNLOAD_FILE_IN_FOLDER: "/folders/:folderId/files/:fileId/download",
  DELETE_FOLDER: "/folders/:id",
  DELETE_FILE_IN_FOLDER: "/folders/:folderId/files/:fileId",
  CREATE_INVITE_FOLDER: "/folders/{folderId}/invites", // TODO()
  VALIDATE_FOLDER_INVITE: "/folders/:folderId/invites/:inviteId",
  RECEIVED_FOLDER_INVITES: "/folders/invites/received",
  SENT_FOLDER_INVITES: "/folders/invites/sent", // TODO()
  LEAVE_SHARED_FOLDER: "/folders/{folderId}/leave", // TODO()
};

export default function httpService() {
  return {
    get: get,
    post: post,
    put: put,
    delete: del,
    patch: patch,
  };

  function buildUrl(path: string, params?: Record<string, string>): string {
    if (!params || Object.keys(params).length === 0) {
      return path;
    }

    const queryString = new URLSearchParams(params).toString();
    return `${path}?${queryString}`;
  }

  async function processRequest<T>(
    uri: string,
    method: string,
    body?: string
  ): Promise<T> {
    const config: RequestInit = {
      method,
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
      },
      body: body,
    };
    console.log("URI ", uri);
    const response = await fetch(uri, config);

    if (!response.ok) {
      if (response.headers.get("Content-Type")?.includes(MEDIA_TYPE_PROBLEM)) {
        const res = await response.json();
        throw res as Problem;
      } else throw new Error(`HTTP error! Status: ${response.status}`);
    }

    const contentTypes = response.headers.get("Content-Type");
    if (!contentTypes || !contentTypes.includes("application/json")) {
      return {} as T;
    }

    return (await response.json()) as T;
  }
  async function get<T>(
    path: string,
    params?: Record<string, string>
  ): Promise<T> {
    const url = buildUrl(path, params);
    return processRequest<T>(url, "GET");
  }

  async function post<T>(
    path: string,
    body?: string,
    params?: Record<string, string>
  ): Promise<T> {
    const url = buildUrl(path, params);
    return processRequest<T>(url, "POST", body);
  }

  async function put<T>(
    path: string,
    body?: string,
    params?: Record<string, string>
  ): Promise<T> {
    const url = buildUrl(path, params);
    return processRequest<T>(url, "PUT", body);
  }

  async function del<T>(
    path: string,
    body?: string,
    params?: Record<string, string>
  ): Promise<T> {
    const url = buildUrl(path, params);
    return processRequest<T>(url, "DELETE", body);
  }

  async function patch<T>(
    path: string,
    body?: string,
    params?: Record<string, string>
  ): Promise<T> {
    const url = buildUrl(path, params);
    return processRequest<T>(url, "PATCH", body);
  }
}
