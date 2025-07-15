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
      users: PageResult<UserHomeOutputModel> | null;
      isFetchingMoreFolders: boolean;
      isFetchingMoreUsers: boolean;
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
  | { type: "search-success"; users: PageResult<UserHomeOutputModel> }
  | { type: "reset" }
  | { type: "search-error"; error: Problem | string }
  | { type: "select-user"; selectUsername: string }
  | { type: "fetch-more-start-folders" }
  | { type: "fetch-more-folders-success"; folders: PageResult<Folder> }
  | { type: "fetch-more-start-users" }
  | { type: "fetch-more-users-success"; users: PageResult<UserHomeOutputModel> }
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
          users: null,
          isFetchingMoreFolders: false,
          isFetchingMoreUsers: false,
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
          return { ...state, users: state.users, isSearchingUsers: true };
        case "search-success":
          return { ...state, users: action.users, isSearchingUsers: true };
        case "reset":
          return { ...state, users: null };
        case "select-user":
          return {
            ...state,
            inputs: {
              ...state.inputs,
              selectUsername: action.selectUsername,
              username: "",
            },
            users: null,
            isSearchingUsers: false,
          };
        case "search-error":
          return { tag: "error", error: action.error };
        case "fetch-more-start-users":
          return {
            ...state,
            isFetchingMoreUsers: true,
          };
        case "fetch-more-start-folders":
          return {
            ...state,
            isFetchingMoreFolders: true,
          };
        case "fetch-more-folders-success":
          return {
            ...state,
            folders: {
              ...action.folders,
              content: [...state.folders.content, ...action.folders.content],
            },
            isFetchingMoreFolders: false,
          };
        case "fetch-more-users-success":
          return {
            ...state,
            users: state.users
              ? {
                  ...action.users,
                  content: [...state.users.content, ...action.users.content],
                }
              : action.users,
            isFetchingMoreUsers: false,
          };
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
            users: null,
            isSearchingUsers: false,
            isFetchingMoreUsers: false,
            isFetchingMoreFolders: false,
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
const sortBy = "updated_desc";

const CreateInvite = () => {
  const [state, dispatch] = useReducer(reducer, firstState);
  const { username, token, setIsLogged, setUsername } = useAuthentication();

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
      const folders = await getFolders(
        token,
        sortBy,
        FolderType.SHARED,
        OwnershipFilter.OWNER
      );
      dispatch({ type: "loading-success", folders });
    } catch (error) {
      Alert.alert(
        "Error",
        `${isProblem(error) ? getProblemMessage(error) : error}`
      );
      dispatch({ type: "loading-error", error: error });
    }
  }

  // Handle Fetch More Folders
  const fetchMoreFolders = async () => {
    if (
      state.tag !== "editing" ||
      state.isFetchingMoreFolders ||
      state.folders.last
    ) {
      return;
    }

    try {
      dispatch({ type: "fetch-more-start-folders" });

      const nextPage = state.folders.number + 1;

      const moreFolders = await getFolders(
        token,
        sortBy,
        FolderType.SHARED,
        OwnershipFilter.OWNER,
        undefined,
        nextPage
      );

      dispatch({ type: "fetch-more-folders-success", folders: moreFolders });
    } catch (error) {
      dispatch({ type: "loading-error", error: error });
    }
  };

  // Handle Fetch More Users
  const fetchMoreUsers = async () => {
    if (
      state.tag !== "editing" ||
      state.isFetchingMoreUsers ||
      state.users === null ||
      state.users.last
    ) {
      return;
    }

    try {
      dispatch({ type: "fetch-more-start-users" });

      const nextPage = state.users.number + 1;

      const moreUsers = await getUsers(selectUsername, token, nextPage, 4);

      dispatch({ type: "fetch-more-users-success", users: moreUsers });
    } catch (error) {
      dispatch({ type: "loading-error", error: error });
    }
  };

  // Handle form submission
  async function handleSubmit() {
    if (state.tag !== "editing") return;

    dispatch({ type: "submit" });

    const { selectUsername, folderId } = state.inputs;

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

  const selectUsername =
    state.tag === "editing" && state.inputs.username
      ? state.inputs.username
      : state.inputs?.username || "";

  // Debounced search effect
  useEffect(() => {
    if (state.tag !== "editing") return;
    const value = selectUsername.trim();
    if (value.length <= SIZE_MIN_USERNAME) {
      dispatch({ type: "reset" });
      return;
    }

    const timeoutId = setTimeout(async () => {
      try {
        dispatch({ type: "user-search" });
        const users = await getUsers(value, token, 0, 4);
        dispatch({ type: "search-success", users: users });
      } catch (error) {
        Alert.alert(
          "Error",
          `${isProblem(error) ? getProblemMessage(error) : error}`
        );
        dispatch({ type: "error", error: error });
      }
    }, 500);

    return () => clearTimeout(timeoutId);
  }, [selectUsername]);

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
    case "redirect":
      return (
        <SafeAreaView className="bg-primary flex-1 justify-center items-center">
          <ActivityIndicator size="large" color="#fff" />
          <Text className="mt-4 text-white text-lg font-semibold">
            Redirect...
          </Text>
        </SafeAreaView>
      );
    case "editing":
      return (
        <SafeAreaView className="bg-primary flex-1">
          <View className="px-6 py-12 flex-1" style={{ flex: 1 }}>
            <View className="flex-row items-center justify-between px-4 mb-6">
              <TouchableOpacity onPress={() => router.back()} hitSlop={12}>
                <Image
                  source={icons.back}
                  className="w-6 h-6"
                  resizeMode="contain"
                  tintColor="white"
                />
              </TouchableOpacity>
              <Text className="text-2xl text-white font-semibold text-center">
                Create Invite in Folder
              </Text>
              <View></View>
            </View>

            <SearchInput
              placeholder="Username"
              value={selectUsername}
              onChangeText={(text) => handleChange("username", text)}
            />

            <FlatList
              data={state.users?.content.filter(
                (user) => user.username !== username
              )}
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
              onEndReached={fetchMoreUsers}
              ListFooterComponent={() =>
                state.isFetchingMoreUsers ? (
                  <View className="bg-primary py-4 justify-center items-center">
                    <ActivityIndicator size="small" color="#fff" />
                  </View>
                ) : null
              }
              onEndReachedThreshold={0.1}
              ListEmptyComponent={() => (
                <View className="flex-1 items-center justify-center py-10">
                  {state.isSearchingUsers && (
                    <Text className="text-white text-lg font-semibold mb-2 text-center">
                      No users match your search
                    </Text>
                  )}
                </View>
              )}
              style={{
                marginTop: 8,
                maxHeight: 160,
              }}
            />

            {state.inputs.selectUsername && (
              <>
                <View
                  style={{
                    height: 1,
                    backgroundColor: "#eee",
                    marginVertical: 10,
                  }}
                />
                <View className="flex-row items-center justify-between">
                  <Text className="text-xl text-white font-semibold">
                    Selected User: {state.inputs.selectUsername}
                  </Text>
                  <TouchableOpacity
                    onPress={() => handleChange("selectUsername", undefined)}
                    className="m-2"
                  >
                    <Image
                      source={icons.reset}
                      className="w-7 h-7"
                      resizeMode="contain"
                      tintColor="white"
                    />
                  </TouchableOpacity>
                </View>
                <View
                  style={{
                    height: 1,
                    backgroundColor: "#eee",
                    marginVertical: 10,
                  }}
                />
              </>
            )}

            <View className="flex-1">
              <View className="flex-row items-center mb-2">
                <Text className="text-xl text-white font-semibold">
                  Recent Folders
                </Text>
                <TouchableOpacity
                  onPress={() => handleChange("folderId", undefined)}
                  className="m-4"
                >
                  <Image
                    source={icons.reset}
                    className="w-7 h-7"
                    resizeMode="contain"
                    tintColor="white"
                  />
                </TouchableOpacity>
              </View>

              <FlatList
                data={state.folders.content}
                keyExtractor={(item) => item.folderId.toString()}
                renderItem={({ item }) => (
                  <FolderItemComponent
                    item={item}
                    onPress={(folderId) => handleChange("folderId", folderId)}
                    selectedFolderId={state.inputs.folderId}
                  />
                )}
                onEndReached={fetchMoreFolders}
                onEndReachedThreshold={0.1}
                style={{ flex: 1, maxHeight: 250 }}
                contentContainerStyle={{
                  paddingBottom: 80,
                }}
                ListFooterComponent={() =>
                  state.isFetchingMoreFolders ? (
                    <View className="bg-primary py-4 justify-center items-center">
                      <ActivityIndicator size="small" color="#fff" />
                    </View>
                  ) : null
                }
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
                containerStyles="rounded-lg mt-10"
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
