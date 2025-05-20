import { UserInfo } from "../user/UserInfo";

export type FolderType = {
  folderId: number;
  user: UserInfo;
  parentId: number | null;
  folderName: string;
  size: number;
  numberFile: number;
  path: string;
  createdAt: number;
  updatedAt: number;
};
