import {
  View,
  Text,
  Alert,
  SafeAreaView,
  ScrollView,
  TouchableOpacity,
  Image,
  ActivityIndicator,
} from "react-native";
import React, { useEffect, useReducer } from "react";
import {
  getProblemMessage,
  isProblem,
  Problem,
} from "@/services/media/Problem";
import { PageResult } from "@/domain/utils/PageResult";
import { UserInfo } from "@/domain/user/UserInfo";
import { KEY_NAME, useAuthentication } from "@/context/AuthProvider";
import { removeValueFor } from "@/services/storage/SecureStorage";
import { router } from "expo-router";
import { Folder } from "@/domain/storage/Folder";
import {
  createInviteFolder,
  getFolders,
} from "@/services/storage/StorageService";
import { icons } from "@/constants";
import SearchInput from "@/components/SearchBar";
import FormField from "@/components/FormField";
import FolderItemComponent from "@/components/FolderItemComponent";
import CustomButton from "@/components/CustomButton";

// The State
type State =
  | { tag: "begin" }
  | {
      tag: "loading";
    }
  | {
      tag: "editing";
      error?: Problem | string;
      inputs: {
        username: string;
        folderId: number | undefined;
      };
      folders: PageResult<Folder>;
      //   users: PageResult<UserInfo>;
    }
  | { tag: "error"; error: Problem | string }
  | {
      tag: "submitting";
      username: string;
      folderId: number | undefined;
      folders: PageResult<Folder>;
      //  users: PageResult<UserInfo>;
    }
  | { tag: "redirect" };

// The Action
type Action =
  | { type: "start-loading" }
  | {
      type: "loading-success";
      folders: PageResult<Folder>;
      //    users: PageResult<UserInfo>;
    }
  | { type: "loading-error"; error: Problem | string }
  | {
      type: "edit";
      inputName: string | number;
      inputValue: string | number;
    }
  | { type: "submit" }
  | { type: "error"; error: Problem | string }
  | { type: "success" };

// The Logger
function logUnexpectedAction(state: State, action: Action) {
  console.log(`Unexpected action '${action.type} on state '${state.tag}'`);
  return state;
}

// The Reducer
function reducer(state: State, action: Action): State {
  switch (state.tag) {
    case "begin":
      if (action.type === "start-loading") {
        return { tag: "loading" };
      } else {
        logUnexpectedAction(state, action);
        return state;
      }
    case "loading":
      if (action.type === "loading-success") {
        return {
          tag: "editing",
          inputs: {
            username: "",
            folderId: undefined,
          },
          folders: action.folders,
          //      users: action.users,
        };
      } else if (action.type === "loading-error") {
        return {
          tag: "error",
          error: action.error,
        };
      } else {
        logUnexpectedAction(state, action);
        return state;
      }
    case "error":
      return state;
    case "editing":
      if (action.type === "edit") {
        return {
          tag: "editing",
          error: undefined,
          inputs: { ...state.inputs, [action.inputName]: action.inputValue },
          folders: state.folders,
          //       users: state.users,
        };
      } else if (action.type === "submit") {
        return {
          tag: "submitting",
          username: state.inputs.username,
          folderId: state.inputs.folderId,
          folders: state.folders,
          //      users: state.users,
        };
      } else {
        logUnexpectedAction(state, action);
        return state;
      }

    case "submitting":
      if (action.type === "success") {
        return {
          tag: "redirect",
        };
      } else if (action.type === "error") {
        return {
          tag: "editing",
          error: action.error,
          inputs: {
            username: "",
            folderId: undefined,
          },
          folders: state.folders,
          //   users: state.users,
        };
      } else {
        logUnexpectedAction(state, action);
        return state;
      }

    case "redirect":
      logUnexpectedAction(state, action);
      return state;
  }
}

const firstState: State = { tag: "begin" };

const CreateInvite = () => {
  const [state, dispatch] = useReducer(reducer, firstState);
  const { token, setIsLogged, setUsername } = useAuthentication();

  useEffect(() => {
    if (state.tag === "begin") {
      dispatch({ type: "start-loading" });
      handleGetFolders();
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

    if (state.tag === "redirect") {
      router.replace("/(modals)/sent-invites");
    }
  }, [state]);

  // Handle input changes
  function handleChange(inputName: string, inputValue: string | boolean | any) {
    dispatch({
      type: "edit",
      inputName,
      inputValue,
    });
  }

  // Handle FetchRecentFolders()
  async function handleGetFolders() {
    try {
      const folders = await getFolders(token);
      dispatch({ type: "loading-success", folders });
    } catch (error) {
      Alert.alert(
        "Error",
        `${isProblem(error) ? getProblemMessage(error) : error}`
      );
      dispatch({ type: "loading-error", error: error });
    }
  }

  // Handle form submission
  async function handleSubmit() {
    if (state.tag !== "editing") return;

    dispatch({ type: "submit" });

    const username = state.inputs.username;
    const folderId = state.inputs.folderId;

    if (folderId === undefined || !username?.trim()) {
      Alert.alert("Error", "Please fill in all fields");
      dispatch({ type: "error", error: "Please fill in all field" });
      return;
    }

    try {
      await createInviteFolder(folderId.toString(), username, token);

      dispatch({ type: "success" });
    } catch (error) {
      Alert.alert(
        "Error",
        `${isProblem(error) ? getProblemMessage(error) : error}`
      );
      dispatch({ type: "error", error: error });
    }
  }

  const username =
    state.tag === "submitting" && state.username
      ? state.username
      : state.inputs?.username || "";

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
    case "editing":
      return (
        <SafeAreaView className="bg-primary h-full">
          <ScrollView className="px-6 py-12">
            <View className="flex-row items-center mb-8">
              <TouchableOpacity onPress={() => router.back()} hitSlop={12}>
                <Image
                  source={icons.back}
                  className="w-6 h-6"
                  resizeMode="contain"
                  tintColor="white"
                />
              </TouchableOpacity>
              <Text className="text-2xl text-white font-psemibold ml-28">
                Create Invite in Folder
              </Text>
            </View>

            <SearchInput placeholder="Username" />
            <View
              style={{
                height: 1,
                backgroundColor: "#23232a",
                marginVertical: 18,
              }}
            />
            <FormField
              title="Username"
              value={username}
              placeholder="Enter a username..."
              handleChangeText={(text) => handleChange("username", text)}
              otherStyles="mt-5"
            />

            <View
              style={{
                height: 1,
                backgroundColor: "#23232a",
                marginVertical: 18,
              }}
            />

            <View className="mt-2">
              <View className="flex-row items-center justify-between mb-2">
                <Text className="text-xl text-white font-semibold">
                  Recent Folders
                </Text>
                <Image
                  source={icons.filter_black1}
                  className="w-10 h-7"
                  resizeMode="contain"
                  tintColor="#fff"
                />
              </View>

              {state.tag === "editing" &&
                state.folders.content.map((folder) => (
                  <FolderItemComponent
                    key={folder.folderId}
                    item={folder}
                    onPress={(folderId) => handleChange("folderId", folderId)}
                  />
                ))}
            </View>

            <CustomButton
              title="Create Invite"
              handlePress={handleSubmit}
              containerStyles="mt-10 rounded-lg"
              isLoading={false}
              textStyles="text-base font-semibold"
              color="bg-secondary"
            />
          </ScrollView>
        </SafeAreaView>
      );
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

export default CreateInvite;
