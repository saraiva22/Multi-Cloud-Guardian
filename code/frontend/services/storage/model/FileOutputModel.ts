import { UserInfo } from "@/domain/user/UserInfo";
import { FolderInfoDetailsModel } from "./FolderInfoDetailsModel";

export type FileOutputModel = {
  fileId: number;
  user: UserInfo;
  folderInfo: FolderInfoDetailsModel | null;
  name: string;
  path: string;
  size: number;
  contentType: string;
  createdAt: number;
  encryption: boolean;
  url: string | null;
};
