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
import { useAuthentication } from "@/context/AuthProvider";
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
import { FileType } from "@/domain/storage/FileType";
import { FolderType } from "@/domain/storage/FolderType";
import BottomSheet from "@gorhom/bottom-sheet";
import SortSelector, {
  SortOption,
  sortOptions,
} from "@/components/SortSelector";

// The State
type State =
  | { tag: "begin"; refreshing: boolean; sort: SortOption }
  | { tag: "loading"; refreshing: boolean; sort: SortOption }
  | {
      tag: "loaded";
      files: PageResult<FileType>;
      folders: PageResult<FolderType>;
      refreshing: boolean;
      sort: SortOption;
    }
  | { tag: "error"; error: Problem | string };

// The Action
type Action =
  | { type: "start-loading" }
  | {
      type: "loading-success";
      files: PageResult<FileType>;
      folders: PageResult<FolderType>;
    }
  | { type: "loading-error"; error: Problem | string }
  | { type: "refreshing"; refreshing: boolean; sort: SortOption };

function logUnexpectedAction(state: State, action: Action) {
  console.log(`Unexpected action '${action.type} on state '${state.tag}'`);
}

// The Reducer
function reducer(state: State, action: Action): State {
  switch (state.tag) {
    case "begin":
      if (action.type === "start-loading") {
        return { tag: "loading", refreshing: false, sort: state.sort };
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
        };
      }
      return state;
  }
}

const firstState: State = {
  tag: "begin",
  refreshing: false,
  sort: sortOptions[0],
};

// User info key
const KEY_NAME = "user_info";

const HomeScreen = () => {
  const { username, setUsername, setIsLogged } = useAuthentication();
  const [state, dispatch] = useReducer(reducer, firstState);
  const router = useRouter();
  const bottomSheetRef = useRef<BottomSheet>(null);

  const sort = state.tag === "error" ? sortOptions[0] : state.sort;


  const openSortSheet = () => {
    bottomSheetRef.current?.expand();
  };

  const handleSelectSort = (sort: SortOption) => {
    bottomSheetRef.current?.close();
    dispatch({ type: "refreshing", refreshing: true, sort: sort });
  };

  const loadData = async () => {
    try {
      dispatch({ type: "start-loading" });

      const files = await getFiles(sort.sortBy);
      const folders = await getFolders();
      dispatch({
        type: "loading-success",
        files,
        folders,
      });
    } catch (error) {
      dispatch({ type: "loading-error", error: error });
    }
  };

  useEffect(() => {
    if (state.tag === "begin") {
      loadData();
    }

    if (state.tag === "error") {
      const message = isProblem(state.error)
        ? getProblemMessage(state.error)
        : isProblem(state.error.body)
        ? getProblemMessage(state.error.body)
        : state.error;
      Alert.alert("Error", `${message}`);
      setUsername(null);
      setIsLogged(false);
      removeValueFor(KEY_NAME);
      router.replace("/sign-in");
    }
  }, [state]);

  const onRefresh = async () => {
    dispatch({ type: "refreshing", refreshing: true, sort: sort });
  };

  const files =
    state.tag === "loaded" && Array.isArray(state.files.content)
      ? state.files.content
      : [];

  const folders =
    state.tag === "loaded" && Array.isArray(state.folders.content)
      ? state.folders.content
      : [];
  const refreshing = state.tag === "loaded" && state.refreshing;

  return (
    <SafeAreaView className="bg-primary h-full">
      {state.tag === "loaded" ? (
        <FlatList
          data={files}
          keyExtractor={(item) => String(item.fileId)}
          renderItem={({ item }) => <FileItemComponent item={item} />}
          contentContainerStyle={{ paddingBottom: 80 }}
          ListHeaderComponent={() => (
            <View className="my-6 px-4 space-y-6">
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
                <View className="mt-1.5">
                  <Image
                    source={icons.notificiation_black}
                    className="w-[24] h-[24]"
                    resizeMode="contain"
                  />
                </View>
              </View>

              <SearchBar />

              <View className="w-full flex-1 pt-5 pb-8">
                <View className="flex-row items-center justify-between mb-3">
                  <Text className="text-2xl font-bold text-gray-100 mb-3">
                    Folders
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

                <FolderCard folders={folders} />
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

                  <TouchableOpacity
                    onPress={openSortSheet}
                    className="borderrounded-full p-2 ml-2"
                    activeOpacity={0.85}
                  >
                    <Image
                      className="w-[44px] h-[32px]"
                      source={icons.filter_black1}
                      resizeMode="contain"
                    />
                  </TouchableOpacity>
                </View>
              </View>
            </View>
          )}
          ListEmptyComponent={() =>
            state.tag === "loaded" ? (
              <EmptyState
                title="No Files Found"
                subtitle="Be the first one to upload a file"
                page="/(modals)/create-file"
                titleButton="Upload File"
              />
            ) : null
          }
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
          }
        />
      ) : (
        <View className="flex-1 items-center justify-center">
          <ActivityIndicator size="large" color="#FFFFFF" />
          <Text className="mt-4 text-white text-base">Loading files...</Text>
        </View>
      )}
      <SortSelector ref={bottomSheetRef} onSortChange={handleSelectSort} />
    </SafeAreaView>
  );
};

export default HomeScreen;
