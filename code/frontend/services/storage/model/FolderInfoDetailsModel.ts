import { FolderType } from "@/domain/storage/FolderType";

export type FolderInfoDetailsModel = {
  folderId: number;
  folderName: string;
  folderType: FolderType;
};
