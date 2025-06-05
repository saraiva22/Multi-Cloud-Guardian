import { UserInfo } from "@/domain/user/UserInfo";

export type FileOutputModel = {
  fileId: number;
  user: UserInfo;
  folderId: number | null;
  name: string;
  path: string;
  size: number;
  contentType: string;
  createdAt: number;
  encryption: boolean;
  url: string | null;
};
