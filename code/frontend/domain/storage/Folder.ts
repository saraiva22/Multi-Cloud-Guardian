import { UserInfo } from "../user/UserInfo";
import { FolderInfo } from "./FolderInfo";
import { FolderType } from "./FolderType";

export interface Folder {
  folderId: number;
  user: UserInfo;
  parentFolderId: FolderInfo | null;
  folderName: string;
  size: number;
  numberFile: number;
  path: string;
  type: FolderType;
  createdAt: number;
  updatedAt: number;
}
