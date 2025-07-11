import { UserInfo } from "../user/UserInfo";
import { FolderInfo } from "./FolderInfo";

/**
 * File
 * This type represents a file in the storage system.
 * @property {number} fileId - The unique identifier for the file.
 * @property {UserInfo} user - The user information associated with the file.
 * @property {number | null} folderInfo - The unique identifier for the folder the file is in, or null if it is not in a folder (e.g., root directory).
 * @property {string} name - The name of the file.
 * @property {string} path - The path to the file in the storage system.
 * @property {number} size - The size of the file in bytes.
 * @property {boolean} encryption - Indicates whether the file is encrypted or not.
 * @property {string} url - The URL to access the file (temporarily)
 */
export interface File {
  fileId: number;
  user: UserInfo;
  folderInfo: FolderInfo | null;
  name: string;
  path: string;
  size: number;
  contentType: string;
  createdAt: number;
  encryption: boolean;
  url: string | null;
}
