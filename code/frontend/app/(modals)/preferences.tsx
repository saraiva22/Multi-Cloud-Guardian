import {
  View,
  Text,
  Image,
  useColorScheme,
  TouchableOpacity,
  Alert,
  SafeAreaView,
  ActivityIndicator,
} from "react-native";
import React, { useEffect, useReducer, useState } from "react";
import { KEY_NAME, useAuthentication } from "@/context/AuthProvider";
import { icons } from "@/constants";
import { Link, router } from "expo-router";
import { UserInfoOutputModel } from "@/services/users/models/UserInfoOutuputModel";
import {
  LOCATION_ARRAY,
  LocationType,
  LocationTypeInfo,
  LocationTypeLabel,
} from "@/domain/preferences/LocationType";
import {
  COST_ARRAY,
  CostType,
  CostTypeInfo,
  CostTypeLabel,
} from "@/domain/preferences/CostType";
import { apiRoutes, PREFIX_API } from "@/services/utils/HttpService";
import {
  getProblemMessage,
  isProblem,
  Problem,
} from "@/services/media/Problem";
import { getUserByUsername } from "@/services/users/UserService";
import { removeValueFor } from "@/services/storage/SecureStorage";

// The State
type State =
  | { tag: "begin" }
  | { tag: "loading"; url: string }
  | {
      tag: "loaded";
      userInfo: UserInfoOutputModel;
      locationInfo: any;
      costInfo: any;
    }
  | { tag: "error"; error: Problem | string; url: string };

// The Action
type Action =
  | { type: "start-loading"; url: string }
  | {
      type: "loading-success";
      userInfo: UserInfoOutputModel;
      locationInfo: any;
      costInfo: any;
    }
  | { type: "loading-error"; error: Problem | string };

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
        costInfo: action.costInfo,
      };
    case "loading-error":
      if (state.tag !== "loading") {
        return logUnexpectedAction(state, action);
      }
      return { tag: "error", error: action.error, url: state.url };
  }
}

const firstState: State = { tag: "begin" };

const Preferences = () => {
  const [state, dispatch] = useReducer(reducer, firstState);
  const { token, username, setIsLogged, setUsername } = useAuthentication();

  async function handlePreferences(
    username: string,
    dispatch: (action: Action) => void
  ) {
    const params = new URLSearchParams({ username }).toString();
    const path = `${PREFIX_API}${apiRoutes.GET_USER_BY_USERNAME}?${params}`;

    dispatch({ type: "start-loading", url: path });
    try {
      const res = await getUserByUsername(username, token);

      const locationInfo = LocationTypeInfo[res.locationType as LocationType];
      const costInfo = CostTypeInfo[res.costType as CostType];

      dispatch({
        type: "loading-success",
        userInfo: res,
        locationInfo,
        costInfo: costInfo,
      });
    } catch (error) {
      console.log("Error fetching preferences:", error);
      dispatch({ type: "loading-error", error: error });
    }
  }

  useEffect(() => {
    if (state.tag == "begin" && username !== undefined) {
      handlePreferences(username, dispatch);
    }

    if (state.tag === "error") {
      Alert.alert(
        "Error",
        `${
          isProblem(state.error)
            ? getProblemMessage(state.error)
            : isProblem(state.error.body)
            ? getProblemMessage(state.error.body)
            : state.error
        }`
      );
      setUsername(null);
      setIsLogged(false);
      removeValueFor(KEY_NAME);
      router.replace("/sign-in");
    }
  }, [state]);
  switch (state.tag) {
    case "begin":
      return (
        <SafeAreaView className="bg-primary flex-1">
          <ActivityIndicator />
        </SafeAreaView>
      );
    case "loading":
      return (
        <SafeAreaView className="bg-primary flex-1">
          <ActivityIndicator />
        </SafeAreaView>
      );
    case "loaded": {
      return (
        <SafeAreaView className="flex-1 bg-primary h-full px-6 py-12">
          <TouchableOpacity
            className="absolute left-6 top-6 z-10 mt-16"
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
                Cost
              </Text>
              <Text className="text-[24px] text-neutral-400">
                {state.costInfo.label}
              </Text>
            </View>
            <Image
              source={state.costInfo.icon}
              className="w-[72px] h-[72px]"
              resizeMode="contain"
            />
          </View>
        </SafeAreaView>
      );
    }
    case "error":
      return (
        <SafeAreaView className="bg-primary flex-1">
          <ActivityIndicator />
          <Text className="text-[24px] font-semibold text-white text-center mb-16 mt-4">
            {state.tag === "error" && "Go to Sign In"}
          </Text>
        </SafeAreaView>
      );
  }
};

export default Preferences;
