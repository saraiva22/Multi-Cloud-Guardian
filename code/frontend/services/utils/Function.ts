import { months } from "@/constants/months";
import { FolderType } from "@/domain/storage/FolderType";
import { InviteStatusType } from "@/domain/storage/InviteStatusType";

export const formatSize = (bytes?: number) => {
  if (!bytes) return "0 Kb";
  if (bytes >= 1000 * 1000) return `${(bytes / (1000 * 1000)).toFixed(1)} Mb`;
  if (bytes >= 1000) return `${(bytes / 1000).toFixed(1)} Kb`;
  return `${bytes} B`;
};

export const formatDate = (date: number) => {
  const dateObj = new Date(date * 1000);
  return `${
    months[dateObj.getMonth()]
  } ${dateObj.getDate()}, ${dateObj.getFullYear()}`;
};

export const formatFolderType = (type: FolderType) => {
  return type.charAt(0) + type.slice(1).toLowerCase();
};

export const formatInviteStatus = (status: InviteStatusType) => {
  return status.charAt(0) + status.slice(1).toLowerCase();
};
