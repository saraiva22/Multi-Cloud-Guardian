import { MEDIA_TYPE_PROBLEM, Problem } from "../media/Problem";

const LOCAL_IP = "192.168.1.107";
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

  // Files
  UPLOAD_FILE: "/files",
  GET_FILES: "/files",
  GET_FILE_BY_ID: "/files/:id",
  DOWNLOAD_FILE: "/files/:id/download",
  DELETE_FILE: "/files/:id",
  CREATE_TEMP_URL: "/files/:id/temp-url",

  // Folders
  CREATE_FOLDER: "/folders",
  GET_FOLDERS: "/folders",
  CREATE_FOLDER_IN_FOLDER: "/folders/:id",
};

export default function httpService() {
  return {
    get: get,
    post: post,
    put: put,
    delete: del,
    patch: patch,
  };

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

  async function get<T>(path: string): Promise<T> {
    return processRequest<T>(path, "GET", undefined);
  }

  async function post<T>(path: string, body?: string): Promise<T> {
    return processRequest<T>(path, "POST", body);
  }

  async function put<T>(path: string, body?: string): Promise<T> {
    return processRequest<T>(path, "PUT", body);
  }

  async function del<T>(path: string, body?: string): Promise<T> {
    return processRequest<T>(path, "DELETE", body);
  }

  async function patch<T>(path: string, body?: string): Promise<T> {
    return processRequest<T>(path, "PATCH", body);
  }
}
