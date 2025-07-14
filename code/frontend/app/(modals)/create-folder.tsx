import {
  View,
  Text,
  Alert,
  SafeAreaView,
  ScrollView,
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
import { router } from "expo-router";
import {
  createFolder,
  createSubFolder,
  getFolders,
} from "@/services/storage/StorageService";
import FormField from "@/components/FormField";
import { icons } from "@/constants";
import CustomButton from "@/components/CustomButton";
import SearchInput from "@/components/SearchBar";
import { Folder } from "@/domain/storage/Folder";
import { PageResult } from "@/domain/utils/PageResult";
import FolderItemComponent from "@/components/FolderItemComponent";
import { FolderType } from "@/domain/storage/FolderType";
import FolderTypeSelector from "@/components/ FolderTypeSelector";
import { KEY_NAME, useAuthentication } from "@/context/AuthProvider";
import { removeValueFor } from "@/services/storage/SecureStorage";
import { OwnershipFilter } from "@/domain/storage/OwnershipFilter";

// The State
type State =
  | {
      tag: "begin";
      search: string;
      inputs: {
        searchValue: string;
      };
    }
  | {
      tag: "loading";
      search: string;
      inputs: {
        searchValue: string;
      };
    }
  | {
      tag: "editing";
      error?: Problem | string;
      inputs: {
        folderName: string;
        folderType: FolderType;
        parentFolderId: number | undefined;
        searchValue: string;
      };
      search: string;
      folders: PageResult<Folder>;
      isFetchingMore: boolean;
    }
  | { tag: "error"; error: Problem | string }
  | {
      tag: "submitting";
      folderName: string;
      search: string;
      folderType: FolderType;
      parentFolderId: number | undefined;
      folders: PageResult<Folder>;
    }
  | { tag: "redirect" };

// The Action
type Action =
  | { type: "start-loading" }
  | { type: "loading-success"; folders: PageResult<Folder> }
  | { type: "loading-error"; error: Problem | string }
  | {
      type: "edit";
      inputName: string | number;
      inputValue: string | number | undefined;
    }
  | { type: "submit" }
  | { type: "fetch-more-start" }
  | { type: "fetch-more-success"; folders: PageResult<Folder> }
  | { type: "error"; error: Problem | string }
  | { type: "search"; searchValue: string }
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
        return {
          tag: "loading",
          search: state.search,
          inputs: {
            searchValue: state.inputs.searchValue,
          },
        };
      } else {
        logUnexpectedAction(state, action);
        return state;
      }
    case "loading":
      if (action.type === "loading-success") {
        return {
          tag: "editing",
          inputs: {
            folderName: "",
            folderType: FolderType.PRIVATE,
            parentFolderId: undefined,
            searchValue: state.inputs.searchValue,
          },
          search: state.search,
          isFetchingMore: false,
          folders: action.folders,
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
          search: state.search,
          error: undefined,
          inputs: { ...state.inputs, [action.inputName]: action.inputValue },
          isFetchingMore: state.isFetchingMore,
          folders: state.folders,
        };
      } else if (action.type === "submit") {
        return {
          tag: "submitting",
          folderName: state.inputs.folderName,
          folderType: state.inputs.folderType,
          parentFolderId: state.inputs.parentFolderId,
          folders: state.folders,
          search: state.search,
        };
      } else if (action.type === "search") {
        return {
          tag: "begin",
          inputs: {
            searchValue: state.inputs.searchValue,
          },
          search: action.searchValue,
        };
      } else if (action.type === "fetch-more-start") {
        return {
          ...state,
          isFetchingMore: true,
        };
      } else if (action.type === "fetch-more-success") {
        return {
          ...state,
          folders: {
            ...action.folders,
            content: [...state.folders.content, ...action.folders.content],
          },
          isFetchingMore: false,
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
            folderName: "",
            folderType: state.folderType,
            parentFolderId: undefined,
            searchValue: "",
          },
          search: "",
          folders: state.folders,
          isFetchingMore: false,
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

const firstState: State = {
  tag: "begin",
  search: "",
  inputs: {
    searchValue: "",
  },
};

const SIZE_MIN_FOLDER = 2;

const CreateFolder = () => {
  const [state, dispatch] = useReducer(reducer, firstState);
  const { token, setIsLogged, setUsername } = useAuthentication();

  const search =
    state.tag === "error" || state.tag === "redirect" ? "" : state.search;

  useEffect(() => {
    if (state.tag === "begin") {
      handleGetFolders();
    }

    if (state.tag === "error") {
      const message = isProblem(state.error)
        ? getProblemMessage(state.error)
        : state.error;
      Alert.alert("Error", `${message}`);
      setUsername(null);
      setIsLogged(false);
      removeValueFor(KEY_NAME);
      router.replace("/sign-in");
    }

    if (state.tag === "redirect") {
      router.replace("/folders");
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
      dispatch({ type: "start-loading" });
      const folders = await getFolders(
        token,
        undefined,
        FolderType.PRIVATE,
        OwnershipFilter.OWNER,
        search
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

  // Handle Fetch More Files
  const fetchMoreFolders = async () => {
    if (state.tag !== "editing" || state.isFetchingMore || state.folders.last) {
      return;
    }

    try {
      dispatch({ type: "fetch-more-start" });

      const nextPage = state.folders.number + 1;

      const moreFiles = await getFolders(
        token,
        undefined,
        FolderType.PRIVATE,
        OwnershipFilter.OWNER,
        search,
        nextPage
      );

      dispatch({ type: "fetch-more-success", folders: moreFiles });
    } catch (error) {
      dispatch({ type: "loading-error", error: error });
    }
  };

  // Handle form submission
  async function handleSubmit() {
    if (state.tag !== "editing") return;

    dispatch({ type: "submit" });

    const folderName = state.inputs.folderName;

    const folderType = state.inputs.folderType;
    const parentFolderId = state.inputs.parentFolderId;

    if (!folderName?.trim()) {
      Alert.alert("Error", "Please fill in all fields");
      dispatch({ type: "error", error: "Please fill in all field" });
      return;
    }

    try {
      if (parentFolderId) {
        if (folderType == FolderType.SHARED) {
          Alert.alert(
            "Error",
            "You cannot create subfolders inside shared folders."
          );
          dispatch({
            type: "error",
            error: "You cannot create subfolders inside shared folders.",
          });
          return;
        }
        await createSubFolder(
          parentFolderId.toString(),
          folderName,
          folderType,
          token
        );
      } else {
        await createFolder(folderName, folderType, token);
      }

      dispatch({ type: "success" });
    } catch (error) {
      Alert.alert(
        "Error",
        `${isProblem(error) ? getProblemMessage(error) : error}`
      );
      dispatch({ type: "error", error: error });
    }
  }

  const searchValue = state.tag === "editing" ? state.inputs.searchValue : "";

  // Debounced search effect
  useEffect(() => {
    if (state.tag !== "editing") return;
    const value = searchValue.trim();
    if (value.length <= SIZE_MIN_FOLDER || value === search) {
      return;
    }

    const timeoutId = setTimeout(async () => {
      dispatch({ type: "search", searchValue: value });
    }, 500);

    return () => clearTimeout(timeoutId);
  }, [searchValue]);

  const folderName =
    state.tag === "editing" && state.inputs.folderName
      ? state.inputs.folderName
      : "";

  const folderType =
    state.tag === "editing" && state.inputs.folderType
      ? state.inputs.folderType
      : FolderType.PRIVATE;

  const folders =
    state.tag === "editing" && Array.isArray(state.folders?.content)
      ? state.folders.content
      : [];

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
    case "submitting":
      return (
        <SafeAreaView className="bg-primary flex-1 justify-center items-center">
          <ActivityIndicator size="large" color="#fff" />
          <Text className="mt-4 text-white text-lg font-semibold">
            Create Folder...
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

    case "error":
      return (
        <SafeAreaView className="bg-primary flex-1">
          <ActivityIndicator />
          <Text className="text-[24px] font-semibold text-white text-center mb-16 mt-4">
            {state.tag === "error" &&
              (typeof state.error === "string"
                ? state.error
                : state.error?.detail)}
          </Text>
        </SafeAreaView>
      );
    case "editing":
      return (
        <SafeAreaView className="bg-primary h-full">
          <View className="px-6 py-6">
            <View className="flex-row  py-5 items-center">
              <TouchableOpacity onPress={() => router.back()} hitSlop={12}>
                <Image
                  source={icons.back}
                  className="w-6 h-6"
                  resizeMode="contain"
                  tintColor="white"
                />
              </TouchableOpacity>
              <Text className="text-2xl text-white font-psemibold ml-28">
                Create Folder
              </Text>
            </View>

            {state.inputs.folderType === FolderType.PRIVATE && (
              <>
                <SearchInput
                  placeholder="Folders"
                  value={searchValue}
                  onChangeText={(text) => handleChange("searchValue", text)}
                />

                <View
                  style={{
                    height: 1,
                    backgroundColor: "#23232a",
                    marginVertical: 18,
                  }}
                />
              </>
            )}

            <FormField
              title="Folder Name"
              value={folderName}
              placeholder="Enter a title for your folder..."
              handleChangeText={(text) => handleChange("folderName", text)}
              otherStyles="mt-5"
            />

            <View
              style={{
                height: 1,
                backgroundColor: "#23232a",
                marginVertical: 18,
              }}
            />

            <FolderTypeSelector
              title="Folder Type"
              value={folderType}
              onChange={(type) => {
                handleChange("folderType", type);
                if (type === FolderType.SHARED) {
                  handleChange("parentFolderId", undefined);
                }
              }}
            />

            <View
              style={{
                height: 1,
                backgroundColor: "#23232a",
                marginVertical: 18,
              }}
            />

            <View className="mt-2">
              {state.inputs.folderType === FolderType.PRIVATE && (
                <>
                  <View className="flex-row items-center mb-2">
                    <Text className="text-xl text-white font-semibold">
                      Recent Folders
                    </Text>
                    <TouchableOpacity
                      onPress={() => handleChange("parentFolderId", undefined)}
                      className="m-4"
                    >
                      <Image
                        source={icons.reset}
                        className="w-6 h-6"
                        resizeMode="contain"
                        tintColor="white"
                      />
                    </TouchableOpacity>
                  </View>

                  <View style={{ flexGrow: 1 }}>
                    <FlatList
                      style={{ maxHeight: 200 }}
                      data={folders}
                      keyExtractor={(item) => String(item.folderId)}
                      renderItem={({ item }) => (
                        <FolderItemComponent
                          key={item.folderId}
                          item={item}
                          onPress={(folderId) =>
                            handleChange("parentFolderId", folderId)
                          }
                          selectedFolderId={state.inputs.parentFolderId}
                        />
                      )}
                      onEndReached={fetchMoreFolders}
                      onEndReachedThreshold={0.1}
                      ListEmptyComponent={() => (
                        <View className="flex-1 items-center justify-center py-10">
                          <Text className="text-white text-lg font-semibold mb-2 text-center">
                            No files match your search
                          </Text>
                        </View>
                      )}
                    />
                  </View>
                </>
              )}

              <CustomButton
                title="Create Folder"
                handlePress={handleSubmit}
                containerStyles="mt-7 rounded-lg"
                isLoading={false}
                textStyles="text-base font-semibold"
                color="bg-secondary"
              />
            </View>
          </View>
        </SafeAreaView>
      );
  }
};

export default CreateFolder;
