import httpServiceInit, { apiRoutes, PREFIX_API } from "../utils/HttpService";
import { LoginOutput } from "./models/LoginOutputModel";
import { RegisterOutput } from "./models/RegisterOutputModel";
import { LogoutOutput } from "./models/LogoutOutput";
import { PerformanceType } from "@/domain/preferences/PerformanceType";
import { LocationType } from "@/domain/preferences/LocationType";

const httpService = httpServiceInit();

export async function register(
  username: string,
  email: string,
  password: string,
  performance: PerformanceType,
  location: LocationType
): Promise<RegisterOutput> {
  const path = PREFIX_API + apiRoutes.REGISTER_USER;
  return await httpService.post<RegisterOutput>(
    path,
    JSON.stringify({
      username,
      email,
      password,
      performanceType: performance,
      locationType: location,
    })
  );
}

export async function login(
  username: string,
  password: string
): Promise<LoginOutput> {
  const path = PREFIX_API + apiRoutes.LOGIN;
  return await httpService.post<LoginOutput>(
    path,
    JSON.stringify({
      username,
      password,
    })
  );
}

export async function logout(): Promise<LogoutOutput> {
  const path = PREFIX_API + apiRoutes.LOGOUT;
  return await httpService.post<LogoutOutput>(path);
}
