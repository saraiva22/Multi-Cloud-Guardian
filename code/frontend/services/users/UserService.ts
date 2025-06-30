import httpServiceInit, { apiRoutes, PREFIX_API } from "../utils/HttpService";
import { LoginOutput } from "./models/LoginOutputModel";
import { RegisterOutput } from "./models/RegisterOutputModel";
import { LogoutOutput } from "./models/LogoutOutput";
import { CostType } from "@/domain/preferences/CostType";
import { LocationType } from "@/domain/preferences/LocationType";
import { CreateUserOutputModel } from "./models/CredentialOutputMode";
import { UserInfoOutputModel } from "./models/UserInfoOutuputModel";
import { StorageDetailsOutputModel } from "./models/StorageDetailsOutputModel";

const httpService = httpServiceInit();

export async function register(
  username: string,
  email: string,
  password: string,
  salt: string,
  iterations: number,
  cost: CostType,
  location: LocationType
): Promise<RegisterOutput> {
  const path = PREFIX_API + apiRoutes.REGISTER_USER;
  return await httpService.post<RegisterOutput>(
    path,
    JSON.stringify({
      username,
      email,
      password,
      salt,
      iterations,
      costType: cost,
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

export async function logout(token: string): Promise<LogoutOutput> {
  const path = PREFIX_API + apiRoutes.LOGOUT;
  return await httpService.post<LogoutOutput>(path, undefined, token);
}

export async function getCredentials(
  token: string
): Promise<CreateUserOutputModel> {
  const path = PREFIX_API + apiRoutes.GET_USER_CREDENTIALS;
  return await httpService.get<CreateUserOutputModel>(path, token);
}

export async function getUserByUsername(
  username: string,
  token: string
): Promise<UserInfoOutputModel> {
  const params = new URLSearchParams({ username }).toString();
  const path = `${PREFIX_API}${apiRoutes.GET_USER_BY_USERNAME}?${params}`;
  return await httpService.get<UserInfoOutputModel>(path, token);
}

export async function getStorageDetails(
  token: string
): Promise<StorageDetailsOutputModel> {
  const path = PREFIX_API + apiRoutes.GET_STORAGE_DETAILS;
  return await httpService.get<StorageDetailsOutputModel>(path, token);
}
