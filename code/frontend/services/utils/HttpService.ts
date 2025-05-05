const LOCAL_IP = "192.168.1.72";
const PORT = "8080";

export const PREFIX_API = `http://${LOCAL_IP}:${PORT}/api`;

export const apiRoutes = {
  // Users
  REGISTER_USER: "/users",
  LOGIN: "/users/token",
  LOGOUT: "/logout",
  GET_USER_BY_ID: "/users/:id",

  // Files
  UPLOAD_FILE: "/files",
  GET_FILES: "/files",
  GET_FILE_BY_ID: "/files/:id",
  DOWNLOAD_FILE: "/files/:id/download",
  DELETE_FILE: "/files/:id",
};
