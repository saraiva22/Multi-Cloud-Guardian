import { InviteStatusType } from "./InviteStatusType";
import { UserInfo } from "../user/UserInfo";

export type Invite = {
  inviteId: number;
  folderId: number;
  folderName: number;
  user: UserInfo;
  status: InviteStatusType;
};
