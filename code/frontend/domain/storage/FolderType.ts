import { UserInfo } from "../user/UserInfo";

export interface FolderType {
  folderId: number;
  user: UserInfo;
  parentFolderId: number | null;
  folderName: string;
  size: number;
  numberFile: number;
  path: string;
  createdAt: number;
  updatedAt: number;
}
