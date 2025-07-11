import {
  View,
  Text,
  Alert,
  SafeAreaView,
  TouchableOpacity,
  Image,
  ActivityIndicator,
  FlatList,
} from "react-native";
import React, { useEffect, useReducer } from "react";
import {
  getProblemMessage,
  isProblem,
  Problem,
} from "@/services/media/Problem";
import { PageResult } from "@/domain/utils/PageResult";
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
import FolderItemComponent from "@/components/FolderItemComponent";
import CustomButton from "@/components/CustomButton";
import { getUserByUsername, getUsers } from "@/services/users/UserService";
import { UserHomeOutputModel } from "@/services/users/models/UserHomeOutputModel";
import EmptyState from "@/components/EmptyState";
import { FolderType } from "@/domain/storage/FolderType";
import { OwnershipFilter } from "@/domain/storage/OwnershipFilter";

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
        selectUsername: string;
        folderId: number | undefined;
      };
      folders: PageResult<Folder>;
      users: UserHomeOutputModel[];
      isSearchingUsers: boolean;
    }
  | { tag: "error"; error: Problem | string }
  | {
      tag: "submitting";
      selectUsername: string;
      folderId: number | undefined;
      folders: PageResult<Folder>;
    }
  | { tag: "redirect" };

// The Action
type Action =
  | { type: "start-loading" }
  | {
      type: "loading-success";
      folders: PageResult<Folder>;
    }
  | { type: "loading-error"; error: Problem | string }
  | {
      type: "edit";
      inputName: string | number;
      inputValue: string | number;
    }
  | { type: "user-search" }
  | { type: "search-success"; users: UserHomeOutputModel[] }
  | { type: "reset" }
  | { type: "search-error"; error: Problem | string }
  | { type: "select-user"; selectUsername: string }
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
            selectUsername: "",
            folderId: undefined,
          },
          folders: action.folders,
          users: [],
          isSearchingUsers: false,
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
      switch (action.type) {
        case "edit":
          return {
            ...state,
            error: undefined,
            inputs: {
              ...state.inputs,
              [action.inputName]: action.inputValue,
            },
          };
        case "user-search":
          return { ...state, isSearchingUsers: true, users: [] };
        case "search-success":
          return { ...state, isSearchingUsers: false, users: action.users };
        case "reset":
          return { ...state, isSearchingUsers: false, users: [] };
        case "select-user":
          return {
            ...state,
            inputs: {
              ...state.inputs,
              selectUsername: action.selectUsername,
              username: "",
            },
            users: [],
            isSearchingUsers: false,
          };
        case "search-error":
          return { tag: "error", error: action.error };
        case "submit":
          return {
            tag: "submitting",
            selectUsername: state.inputs.selectUsername,
            folderId: state.inputs.folderId,
            folders: state.folders,
          };
        default:
          return logUnexpectedAction(state, action);
      }
    case "submitting":
      switch (action.type) {
        case "success":
          return { tag: "redirect" };
        case "error":
          return {
            tag: "editing",
            error: action.error,
            inputs: {
              username: "",
              selectUsername: "",
              folderId: undefined,
            },
            folders: state.folders,
            users: [],
            isSearchingUsers: false,
          };
        default:
          return logUnexpectedAction(state, action);
      }

    case "redirect":
      logUnexpectedAction(state, action);
      return state;
  }
}

const firstState: State = { tag: "begin" };
const SIZE_MIN_USERNAME = 2;

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
      const folders = await getFolders(token, undefined, FolderType.SHARED,OwnershipFilter.OWNER);
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

    const { selectUsername, folderId } = state.inputs;
    console.log("SELECT NAME ", selectUsername);

    if (!selectUsername.trim() || folderId == null) {
      Alert.alert("Error", "Please fill in all fields");
      dispatch({ type: "error", error: "Please fill in all field" });
      return;
    }

    try {
      await createInviteFolder(folderId.toString(), selectUsername, token);

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
    state.tag === "editing" && state.inputs.username
      ? state.inputs.username
      : state.inputs?.username || "";

  // Debounced search effect
  useEffect(() => {
    if (state.tag !== "editing") return;
    const value = username.trim();
    if (value.length <= SIZE_MIN_USERNAME) {
      dispatch({ type: "reset" });
      return;
    }

    const timeoutId = setTimeout(async () => {
      try {
        dispatch({ type: "user-search" });
        const users = await getUsers(value, token);
        dispatch({ type: "search-success", users: users.content });
      } catch (error) {
        dispatch({ type: "search-error", error: error });
      }
    }, 500);

    return () => clearTimeout(timeoutId);
  }, [username]);

  // Render UI
  switch (state.tag) {
    case "begin":
      return (
        <SafeAreaView className="bg-primary flex-1 justify-center items-center">
          <ActivityIndicator size="large" color="#fff" />
        </SafeAreaView>
      );
    case "loading":
      return (
        <SafeAreaView className="bg-primary flex-1 justify-center items-center">
          <ActivityIndicator size="large" color="#fff" />
          <Text className="mt-4 text-white text-lg font-semibold">
            Loading...
          </Text>
        </SafeAreaView>
      );
    case "editing":
      return (
        <SafeAreaView className="bg-primary flex-1">
          <View className="px-6 py-12 flex-1">
            <View className="flex-row items-center justify-between px-4 mb-6 relative">
              <TouchableOpacity onPress={() => router.back()} hitSlop={12}>
                <Image
                  source={icons.back}
                  className="w-6 h-6"
                  resizeMode="contain"
                  tintColor="white"
                />
              </TouchableOpacity>
              <Text className="absolute left-10 right-0 text-2xl text-white font-semibold text-center">
                Create Invite in Folder
              </Text>
            </View>

            <SearchInput
              placeholder="Username"
              value={username}
              onChangeText={(text) => handleChange("username", text)}
            />

            {state.isSearchingUsers && (
              <View className="items-center justify-center mt-4">
                <ActivityIndicator size="small" color="#FFFFFF" />
                <Text className="mt-2 text-white text-base">
                  Loading Users...
                </Text>
              </View>
            )}

            <FlatList
              data={state.users}
              keyExtractor={(item) => item.username}
              renderItem={({ item }) => (
                <TouchableOpacity
                  onPress={() =>
                    dispatch({
                      type: "select-user",
                      selectUsername: item.username,
                    })
                  }
                  style={{
                    padding: 10,
                    borderBottomWidth: 1,
                    borderColor: "#444",
                  }}
                >
                  <Text style={{ color: "white" }}>{item.username}</Text>
                </TouchableOpacity>
              )}
              style={{
                marginTop: 8,
                maxHeight: state.users.length > 0 ? 150 : 0,
              }}
            />

            {state.inputs.selectUsername && (
              <>
                <View
                  style={{
                    height: 1,
                    backgroundColor: "#eee",
                    marginVertical: 18,
                  }}
                />
                <Text>
                  <Text className="text-xl text-white font-semibold">
                    Selected User: {state.inputs.selectUsername}
                  </Text>
                </Text>
                <View
                  style={{
                    height: 1,
                    backgroundColor: "#eee",
                    marginVertical: 18,
                  }}
                />
              </>
            )}

            <View className="mt-2 flex-1">
              <View className="flex-row items-center justify-between mb-2">
                <Text className="text-xl text-white font-semibold">
                  Recent Folders
                </Text>
              </View>

              <FlatList
                data={state.folders.content}
                keyExtractor={(item) => item.folderId.toString()}
                renderItem={({ item }) => (
                  <FolderItemComponent
                    item={item}
                    onPress={(folderId) => handleChange("folderId", folderId)}
                  />
                )}
                showsVerticalScrollIndicator={false}
                ListEmptyComponent={() => (
                  <EmptyState
                    title="No folders found"
                    subtitle="Try creating a new folder"
                    page="/(modals)/create-folder"
                    titleButton="Create Folder"
                  />
                )}
              />

              <CustomButton
                title="Create Invite"
                handlePress={handleSubmit}
                containerStyles="rounded-lg"
                isLoading={false}
                textStyles="text-base font-semibold"
                color="bg-secondary"
              />
            </View>
          </View>
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
    case "submitting":
      return (
        <SafeAreaView className="bg-primary flex-1 justify-center items-center">
          <ActivityIndicator size="large" color="#fff" />
        </SafeAreaView>
      );
  }
};

export default CreateInvite;
