import { Folder } from "@/domain/storage/Folder";
import { FolderInfo } from "@/domain/storage/FolderInfo";
import { FolderType } from "@/domain/storage/FolderType";
import { UserInfo } from "@/domain/user/UserInfo";

export type FolderOutputModel = {
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
  members: Array<UserInfo>;
};
