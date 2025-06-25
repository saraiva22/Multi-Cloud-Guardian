import { LocationType } from "@/domain/preferences/LocationType";
import { CostType } from "@/domain/preferences/CostType";

export type UserInfoOutputModel = {
  userId: number;
  username: string;
  email: string;
  locationType: LocationType;
  costType: CostType;
};
