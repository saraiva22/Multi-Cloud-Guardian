import {
  FlatList,
  RefreshControl,
  View,
  Text,
  Image,
  Alert,
  ActivityIndicator,
  TouchableOpacity,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import EmptyState from "@/components/EmptyState";
import SearchBar from "@/components/SearchBar";
import { icons } from "@/constants";
import { KEY_NAME, useAuthentication } from "@/context/AuthProvider";
import React, { useEffect, useMemo, useReducer, useRef, useState } from "react";
import { useRouter } from "expo-router";
import { getFiles, getFolders } from "@/services/storage/StorageService";

import {
  getProblemMessage,
  isProblem,
  Problem,
} from "@/services/media/Problem";
import { removeValueFor } from "@/services/storage/SecureStorage";
import FolderCard from "@/components/FolderCard";
import FileItemComponent from "@/components/FileItemComponent";
import { PageResult } from "@/domain/utils/PageResult";
import { File } from "@/domain/storage/File";
import { Folder } from "@/domain/storage/Folder";
import BottomSheet from "@gorhom/bottom-sheet";
import SortSelector, {
  SortOption,
  sortOptions,
} from "@/components/SortSelector";
import SearchInput from "@/components/SearchBar";
import { FolderType } from "@/domain/storage/FolderType";
import { OwnershipFilter } from "@/domain/storage/OwnershipFilter";
import FileActionsBottomSheet from "@/components/FileActionsBottomSheet";

// The State
type State =
  | {
      tag: "begin";
      refreshing: boolean;
      sort: SortOption;
      search: string;
      inputs: {
        searchValue: string;
      };
    }
  | {
      tag: "loading";
      refreshing: boolean;
      sort: SortOption;
      search: string;
      inputs: {
        searchValue: string;
      };
    }
  | {
      tag: "loaded";
      files: PageResult<File>;
      folders: PageResult<Folder>;
      refreshing: boolean;
      search: string;
      sort: SortOption;
      inputs: {
        searchValue: string;
      };
      isFetchingMore: boolean;
      selectFile: File | null;
    }
  | { tag: "error"; error: Problem | string };

// The Action
type Action =
  | { type: "start-loading" }
  | {
      type: "loading-success";
      files: PageResult<File>;
      folders: PageResult<Folder>;
    }
  | {
      type: "edit";
      inputName: string | number;
      inputValue: string | number;
    }
  | { type: "fetch-more-start" }
  | { type: "fetch-more-files-success"; files: PageResult<File> }
  | { type: "fetch-more-folders-success"; folders: PageResult<Folder> }
  | { type: "loading-error"; error: Problem | string }
  | { type: "refreshing"; refreshing: boolean; sort: SortOption }
  | { type: "search"; search: string }
  | { type: "delete-select-file"; file: File }
  | { type: "select-file"; file: File }
  | {
      type: "reset";
    };

function logUnexpectedAction(state: State, action: Action) {
  console.log(`Unexpected action '${action.type} on state '${state.tag}'`);
}

// The Reducer
function reducer(state: State, action: Action): State {
  switch (state.tag) {
    case "begin":
      if (action.type === "start-loading") {
        return {
          tag: "loading",
          refreshing: false,
          sort: state.sort,
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
          tag: "loaded",
          files: action.files,
          folders: action.folders,
          refreshing: false,
          sort: state.sort,
          inputs: {
            searchValue: state.inputs.searchValue,
          },
          search: state.search,
          isFetchingMore: false,
          selectFile: null,
        };
      } else if (action.type === "loading-error") {
        return { tag: "error", error: action.error };
      } else {
        logUnexpectedAction(state, action);
        return state;
      }
    case "error":
      return { tag: "error", error: state.error };
    case "loaded":
      if (action.type === "refreshing") {
        return {
          tag: "begin",
          refreshing: action.refreshing,
          sort: action.sort,
          search: "",
          inputs: {
            searchValue: "",
          },
        };
      } else if (action.type === "search") {
        return {
          tag: "begin",
          refreshing: false,
          sort: state.sort,
          search: action.search,
          inputs: {
            searchValue: state.inputs.searchValue,
          },
        };
      } else if (action.type === "edit") {
        return {
          ...state,
          tag: "loaded",
          inputs: { ...state.inputs, [action.inputName]: action.inputValue },
        };
      } else if (action.type === "select-file") {
        return { ...state, selectFile: action.file };
      } else if (action.type === "delete-select-file") {
        return {
          ...state,
          tag: "loaded",
          files: {
            ...state.files,
            content: state.files.content.filter(
              (file) => file.fileId !== action.file.fileId
            ),
            totalElements: state.files.totalElements - 1,
          },
        };
      } else if (action.type === "fetch-more-start") {
        return {
          ...state,
          isFetchingMore: true,
        };
      } else if (action.type === "fetch-more-files-success") {
        return {
          ...state,
          files: {
            ...action.files,
            content: [...state.files.content, ...action.files.content],
          },
          isFetchingMore: false,
        };
      } else if (action.type === "fetch-more-folders-success") {
        return {
          ...state,
          folders: {
            ...action.folders,
            content: [...state.folders.content, ...action.folders.content],
          },
          isFetchingMore: false,
        };
      } else if (action.type === "reset") {
        return {
          tag: "begin",
          refreshing: false,
          sort: sortOptions[0],
          search: "",
          inputs: {
            searchValue: "",
          },
        };
      } else {
        logUnexpectedAction(state, action);
        return state;
      }
  }
}

const firstState: State = {
  tag: "begin",
  refreshing: false,
  sort: sortOptions[0],
  search: "",
  inputs: {
    searchValue: "",
  },
};

const SIZE_MIN_FILE = 2;

const HomeScreen = () => {
  const { token, username, setUsername, setIsLogged } = useAuthentication();
  const [state, dispatch] = useReducer(reducer, firstState);
  const router = useRouter();
  const bottomSheetRef = useRef<BottomSheet>(null);
  const fileSheetRef = useRef<BottomSheet>(null);

  const sort = state.tag === "error" ? sortOptions[0] : state.sort;

  const search = state.tag === "error" ? "" : state.search;

  const openSortSheet = () => {
    fileSheetRef.current?.close();
    bottomSheetRef.current?.expand();
  };

  const openMoveSheet = (file: File) => {
    bottomSheetRef.current?.close();
    dispatch({ type: "select-file", file });
    fileSheetRef.current?.expand();
  };

  const handleSelectSort = (sort: SortOption) => {
    bottomSheetRef.current?.close();
    dispatch({ type: "refreshing", refreshing: true, sort: sort });
  };

  useEffect(() => {
    if (state.tag === "begin") {
      loadData();
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

  // Handle input changes
  function handleChange(inputName: string, inputValue: string | boolean | any) {
    dispatch({
      type: "edit",
      inputName,
      inputValue,
    });
  }

  const loadData = async () => {
    try {
      dispatch({ type: "start-loading" });
      const files = await getFiles(
        token,
        sort.sortBy,
        search,
        FolderType.PRIVATE
      );
      const folders = await getFolders(
        token,
        undefined,
        FolderType.PRIVATE,
        OwnershipFilter.OWNER
      );
      dispatch({
        type: "loading-success",
        files,
        folders,
      });
    } catch (error) {
      dispatch({ type: "loading-error", error: error });
    }
  };

  // Handle Fetch More Files
  const fetchMoreFiles = async () => {
    if (state.tag !== "loaded" || state.isFetchingMore || state.files.last) {
      return;
    }

    try {
      dispatch({ type: "fetch-more-start" });

      const nextPage = state.files.number + 1;

      const moreFiles = await getFiles(
        token,
        sort.sortBy,
        search,
        FolderType.PRIVATE,
        nextPage
      );

      dispatch({ type: "fetch-more-files-success", files: moreFiles });
    } catch (error) {
      dispatch({ type: "loading-error", error: error });
    }
  };

  // Handle Fetch More Folders
  const fetchMoreFolders = async () => {
    if (state.tag !== "loaded" || state.isFetchingMore || state.folders.last) {
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
        undefined,
        nextPage
      );

      dispatch({ type: "fetch-more-folders-success", folders: moreFiles });
    } catch (error) {
      dispatch({ type: "loading-error", error: error });
    }
  };

  const onRefresh = async () => {
    setTimeout(() => {
      dispatch({ type: "refreshing", refreshing: true, sort: sort });
    }, 200);
  };

  const searchValue = state.tag === "loaded" ? state.inputs.searchValue : "";

  // Debounced search effect
  useEffect(() => {
    if (state.tag !== "loaded") return;
    const value = searchValue.trim();
    if (value.length <= SIZE_MIN_FILE || value === search) {
      return;
    }

    const timeoutId = setTimeout(async () => {
      dispatch({ type: "search", search: value });
    }, 500);

    return () => clearTimeout(timeoutId);
  }, [searchValue]);

  const files =
    state.tag === "loaded" && Array.isArray(state.files.content)
      ? state.files.content
      : [];

  const folders =
    state.tag === "loaded" && Array.isArray(state.folders.content)
      ? state.folders.content
      : [];
  const refreshing = state.tag === "loaded" && state.refreshing;

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
    case "loaded":
      return (
        <SafeAreaView className="bg-primary h-full">
          <View className="my-6 px-4 space-y-6" style={{ flex: 1 }}>
            <View className="flex-row justify-between items-center mb-5">
              <View className="flex-row items-center space-x-10">
                <Image
                  source={icons.profile}
                  className="w-[32] h-[32] mr-2"
                  resizeMode="contain"
                />
                <Text className="text-xl font-psemibold text-white">
                  Hello, {username}
                </Text>
              </View>
            </View>

            <SearchInput
              placeholder="Files"
              value={searchValue}
              onChangeText={(text) => handleChange("searchValue", text)}
            />

            <FlatList
              data={files}
              keyExtractor={(item) => item.fileId.toString()}
              renderItem={({ item }) => (
                <FileItemComponent
                  item={item}
                  onMovePress={() => openMoveSheet(item)}
                />
              )}
              style={{ flex: 1 }}
              contentContainerStyle={{ paddingBottom: 70 }}
              onEndReached={fetchMoreFiles}
              onEndReachedThreshold={0.1}
              ListFooterComponent={() =>
                state.isFetchingMore ? (
                  <View className="bg-primary py-4 justify-center items-center">
                    <ActivityIndicator size="small" color="#fff" />
                  </View>
                ) : null
              }
              ListHeaderComponent={() => (
                <>
                  <View className="w-full flex-1 pt-5 pb-8">
                    <View className="flex-row items-center justify-between mb-3">
                      <Text className="text-2xl font-bold text-gray-100 mb-3">
                        My Folders
                      </Text>
                      <Text className="text-xl font-pregular text-secondary-200 mb-3">
                        <TouchableOpacity
                          onPress={() => router.push("/create-folder")}
                        >
                          <Image
                            source={icons.plus_folder_black}
                            className="w-3 h-3 mr-1"
                            resizeMode="contain"
                          />
                        </TouchableOpacity>
                        Create
                      </Text>
                    </View>

                    <FolderCard
                      folders={folders}
                      fetchFolders={fetchMoreFolders}
                    />
                  </View>
                  <View className="w-full flex-1 pt-2">
                    <Text className="text-2xl font-bold text-gray-100">
                      My Files
                    </Text>
                    <View className="flex-row justify-between items-center my-3 mx-2">
                      <View className="border border-gray-200 rounded-full bg-white px-4 py-2 flex-row items-center">
                        <Text className="text-lg text-gray-900 font-medium">
                          {sort.label}
                        </Text>
                      </View>
                      <View className="flex-row ">
                        <TouchableOpacity
                          onPress={openSortSheet}
                          className="borderrounded-full p-1"
                          activeOpacity={0.85}
                        >
                          <Image
                            className="w-[40px] h-[34px]"
                            source={icons.sort_black1}
                            resizeMode="contain"
                          />
                        </TouchableOpacity>
                        <TouchableOpacity
                          activeOpacity={0.85}
                          className="p-1"
                          onPress={() => dispatch({ type: "reset" })}
                        >
                          <Image
                            source={icons.reset}
                            className="w-[40px] h-[30px]"
                            resizeMode="contain"
                            tintColor="white"
                          />
                        </TouchableOpacity>
                      </View>
                    </View>
                  </View>
                </>
              )}
              ListEmptyComponent={() => (
                <View className="flex-1 items-center justify-center py-10">
                  <Text className="text-white text-lg font-semibold mb-2 text-center">
                    No files match your search
                  </Text>
                </View>
              )}
              refreshControl={
                <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
              }
            />
          </View>
          <SortSelector ref={bottomSheetRef} onSortChange={handleSelectSort} />
          <FileActionsBottomSheet ref={fileSheetRef} file={state.selectFile} />
        </SafeAreaView>
      );
  }
};

export default HomeScreen;
