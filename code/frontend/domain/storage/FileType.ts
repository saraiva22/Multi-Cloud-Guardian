import { UserInfo } from "../user/UserInfo";

/**
 * FileType
 * This type represents a file in the storage system.
 * @property {number} fileId - The unique identifier for the file.
 * @property {UserInfo} userInfo - The user information associated with the file.
 * @property {number | null} folderId - The unique identifier for the folder the file is in, or null if it is not in a folder (e.g., root directory).
 * @property {string} name - The name of the file.
 * @property {string} path - The path to the file in the storage system.
 * @property {number} size - The size of the file in bytes.
 * @property {boolean} encryption - Indicates whether the file is encrypted or not.
 * @property {string} url - The URL to access the file (temporarily)
 */
export interface FileType {
  fileId: number;
  userInfo: UserInfo;
  folderId: number | null;
  name: string;
  path: string;
  size: number;
  contentType: string;
  createdAt: number;
  encryption: boolean;
  url: string;
}
