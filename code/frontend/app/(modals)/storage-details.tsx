import {
  View,
  Text,
  Image,
  TouchableOpacity,
  Alert,
  SafeAreaView,
  ActivityIndicator,
} from "react-native";
import { useAuthentication } from "@/context/AuthProvider";
import {
  getProblemMessage,
  isProblem,
  Problem,
} from "@/services/media/Problem";
import { removeValueFor } from "@/services/storage/SecureStorage";
import { StorageDetailsOutputModel } from "@/services/users/models/StorageDetailsOutputModel";
import { getStorageDetails } from "@/services/users/UserService";
import { router } from "expo-router";
import { useEffect, useReducer } from "react";
import { colors, icons } from "@/constants";
import { formatSize } from "@/services/utils/Function";
import StorageUsageChart from "@/components/StorageDonutChart";
import StorageBarItem from "@/components/StorageBarItem";

// The State
type State =
  | { tag: "begin" }
  | { tag: "loading" }
  | { tag: "loaded"; storageDetails: StorageDetailsOutputModel }
  | { tag: "error"; error: Problem | string };

// The Action
type Action =
  | { type: "start-loading" }
  | { type: "loading-success"; storageDetails: StorageDetailsOutputModel }
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
      return { tag: "loading" };
    case "loading-success":
      if (state.tag !== "loading") {
        return logUnexpectedAction(state, action);
      }
      return {
        tag: "loaded",
        storageDetails: action.storageDetails,
      };
    case "loading-error":
      if (state.tag !== "loading") {
        return logUnexpectedAction(state, action);
      }
      return { tag: "error", error: action.error };
  }
}

const firstState: State = { tag: "begin" };
const KEY_NAME = "user_info";

const StorageDetails = () => {
  const [state, dispatch] = useReducer(reducer, firstState);
  const { username, setUsername, setIsLogged } = useAuthentication();

  async function handleStorageDetails(dispatch: (action: Action) => void) {
    dispatch({ type: "start-loading" });
    try {
      const res = await getStorageDetails();
      dispatch({ type: "loading-success", storageDetails: res });
    } catch (error) {
      console.log("Error fetching storage details:", error);
      dispatch({ type: "loading-error", error: error });
    }
  }

  useEffect(() => {
    if (state.tag == "begin" && username !== undefined) {
      handleStorageDetails(dispatch);
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
      const chartData = [
        {
          label: "Images",
          value: state.storageDetails.images,
          color: colors.Images,
        },
        {
          label: "Videos",
          value: state.storageDetails.video,
          color: colors.Video,
        },
        {
          label: "Documents",
          value: state.storageDetails.documents,
          color: colors.Documents,
        },
        {
          label: "Others",
          value: state.storageDetails.others,
          color: colors.Others,
        },
      ];
      return (
        <SafeAreaView className="flex-1 bg-primary h-full px-2 py-12">
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

          <Text className="text-[24px] font-semibold text-white text-center mb-8 mt-4">
            Storage Details
          </Text>
          <View className="items-center mb-10">
            <StorageUsageChart data={chartData} />
          </View>
          <View className="items-center mb-8">
            <Text className="text-[22px] font-semibold text-white mb-2">
              Total Size
            </Text>
            <Text className="text-[25px] font-bold text-zinc-300">
              {formatSize(state.storageDetails.totalSize)}
            </Text>
          </View>
          <View className="mb-6 mt-2 px-4">
            <View className="mb-6 mt-2 px-4">
              {chartData.map((item) => (
                <StorageBarItem
                  key={item.label}
                  label={item.label}
                  value={item.value}
                  total={state.storageDetails.totalSize}
                  color={item.color}
                />
              ))}
            </View>
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

export default StorageDetails;
