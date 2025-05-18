import { LocationType } from "@/domain/preferences/LocationType";
import { PerformanceType } from "@/domain/preferences/PerformanceType";

export type UserInfoOutputModel = {
  userId: number;
  username: string;
  email: string;
  locationType: LocationType;
  performanceType: PerformanceType;
};
