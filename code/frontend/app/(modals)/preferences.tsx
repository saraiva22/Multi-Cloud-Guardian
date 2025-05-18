import {
  View,
  Text,
  Image,
  useColorScheme,
  TouchableOpacity,
  Alert,
} from "react-native";
import React, { useEffect, useReducer, useState } from "react";
import { useAuthentication } from "@/context/AuthProvider";
import { icons } from "@/constants";
import { router } from "expo-router";
import { UserInfoOutputModel } from "@/services/users/models/UserInfoOutuputModel";
import {
  LOCATION_ARRAY,
  LocationType,
  LocationTypeInfo,
  LocationTypeLabel,
} from "@/domain/preferences/LocationType";
import {
  PERFORMANCE_ARRAY,
  PerformanceType,
  PerformanceTypeInfo,
  PerformanceTypeLabel,
} from "@/domain/preferences/PerformanceType";
import { apiRoutes, PREFIX_API } from "@/services/utils/HttpService";
import {
  getProblemMessage,
  isProblem,
  Problem,
} from "@/services/media/Problem";
import { getUserByUsername } from "@/services/users/UserService";

// The State
type State =
  | { tag: "begin" }
  | { tag: "loading"; url: string }
  | {
      tag: "loaded";
      userInfo: UserInfoOutputModel;
      locationInfo: any;
      performanceInfo: any;
    }
  | { tag: "error"; error: Error | Problem; url: string };

// The Action
type Action =
  | { type: "start-loading"; url: string }
  | {
      type: "loading-success";
      userInfo: UserInfoOutputModel;
      locationInfo: any;
      performanceInfo: any;
    }
  | { type: "loading-error"; error: Error | Problem };

// The Logger
function logUnexpectedAction(state: State, action: Action) {
  console.log(`Unexpected action '${action.type} on state '${state.tag}'`);
  return state;
}

// The reducer
function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "start-loading":
      return { tag: "loading", url: action.url };
    case "loading-success":
      if (state.tag !== "loading") {
        return logUnexpectedAction(state, action);
      }
      return {
        tag: "loaded",
        userInfo: action.userInfo,
        locationInfo: action.locationInfo,
        performanceInfo: action.performanceInfo,
      };
    case "loading-error":
      if (state.tag !== "loading") {
        return logUnexpectedAction(state, action);
      }
      return { tag: "error", error: action.error, url: state.url };
  }
}

async function fetchPreferences(
  username: string,
  isFetching: boolean,
  setIsFetching: (isFetching: boolean) => void,
  dispatch: (action: Action) => void
) {
  if (isFetching) return;

  const params = new URLSearchParams({ username }).toString();
  const path = `${PREFIX_API}${apiRoutes.GET_USER_BY_USERNAME}?${params}`;
  setIsFetching(true);
  dispatch({ type: "start-loading", url: path });
  try {
    const res = await getUserByUsername(username);

    const locationInfo = LocationTypeInfo[res.locationType as LocationType];
    const performanceInfo =
      PerformanceTypeInfo[res.performanceType as PerformanceType];

    dispatch({
      type: "loading-success",
      userInfo: res,
      locationInfo,
      performanceInfo,
    });
  } catch (error) {
    Alert.alert(
      "Error",
      `${isProblem(error) ? getProblemMessage(error) : error}`
    );
    dispatch({ type: "loading-error", error: error });
  } finally {
    setIsFetching(false);
  }
}

const firstState: State = { tag: "begin" };

const Preferences = () => {
  const [state, dispatch] = useReducer(reducer, firstState);
  const { username } = useAuthentication();
  const [isFetching, setIsFetching] = useState(false);

  useEffect(() => {
    if (state.tag == "begin" && username !== undefined) {
      fetchPreferences(username, isFetching, setIsFetching, dispatch);
    }
  }, [isFetching, setIsFetching, state]);
  switch (state.tag) {
    case "begin":
      return (
        <View>
          <Text>Idle</Text>
        </View>
      );
    case "loading":
      return (
        <View>
          <Text>Loading...</Text>
        </View>
      );
    case "loaded": {
      return (
        <View className="flex-1 bg-primary h-full px-6 py-12">
          <TouchableOpacity
            className="absolute left-6 top-8 z-10 mt-10"
            onPress={() => router.back()}
            hitSlop={12}
          >
            <Image
              source={icons.back}
              className="w-6 h-6"
              resizeMode="contain"
              tintColor="white"
            />
          </TouchableOpacity>

          <Text className="text-[24px] font-semibold text-white text-center mb-16 mt-4">
            Preferences
          </Text>

          <View className="items-center mb-12">
            <Image
              source={icons.preferences}
              className="w-[135px] h-[135px] mb-8"
              resizeMode="contain"
              tintColor="white"
            />
            <Text className="text-[24px] font-semibold text-white mb-4">
              {state.userInfo.username}
            </Text>
            <Text className="text-[16px] text-zinc-300">
              {state.userInfo.email}
            </Text>
          </View>

          <View
            className="flex-row items-center justify-between bg-white rounded-3xl px-7 py-6 mb-12 shadow-lg"
            style={{
              shadowColor: "#000",
              shadowOffset: { width: 0, height: 6 },
              shadowOpacity: 0.08,
              shadowRadius: 16,
              elevation: 4,
            }}
          >
            <View>
              <Text className="text-[24px] font-semibold text-neutral-900">
                Location
              </Text>
              <Text className="text-[24px] text-neutral-400">
                {state.locationInfo.label}
              </Text>
            </View>
            <Image
              source={state.locationInfo.icon}
              className="w-[72px] h-[72px]"
              resizeMode="contain"
            />
          </View>

          <View
            className="flex-row items-center justify-between bg-white rounded-3xl px-7 py-6 shadow-lg"
            style={{
              shadowColor: "#000",
              shadowOffset: { width: 0, height: 6 },
              shadowOpacity: 0.08,
              shadowRadius: 16,
              elevation: 4,
            }}
          >
            <View>
              <Text className="text-[24px] font-semibold text-neutral-900">
                Performance
              </Text>
              <Text className="text-[24px] text-neutral-400">
                {state.performanceInfo.label}
              </Text>
            </View>
            <Image
              source={state.performanceInfo.icon}
              className="w-[72px] h-[72px]"
              resizeMode="contain"
            />
          </View>
        </View>
      );
    }
    case "error":
      return (
        <View>
          <Text>{`${
            isProblem(state.error)
              ? getProblemMessage(state.error)
              : state.error
          }`}</Text>
        </View>
      );
  }
};

export default Preferences;
