import {
  FlatList,
  RefreshControl,
  View,
  Text,
  Image,
  Alert,
  ActivityIndicator,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import EmptyState from "@/components/EmptyState";
import SearchInput from "@/components/SearchBar";
import { icons } from "@/constants";
import { useAuthentication } from "@/context/AuthProvider";
import React, { useEffect, useReducer, useState } from "react";
import { PageResult } from "@/domain/utils/PageResult";
import { FileType } from "@/domain/storage/FileType";
import {
  getProblemMessage,
  isProblem,
  Problem,
} from "@/services/media/Problem";
import { useRouter } from "expo-router";
import { getFiles } from "@/services/storage/StorageService";
import { removeValueFor } from "@/services/storage/SecureStorage";
import FileItemComponent from "@/components/FileItemComponent";

// The State
type State =
  | { tag: "begin"; refreshing: boolean }
  | { tag: "loading" }
  | {
      tag: "loaded";
      files: PageResult<FileType>;
      refreshing: boolean;
    }
  | { tag: "error"; error: Problem | string };

// The Action
type Action =
  | { type: "start-loading" }
  | {
      type: "loading-success";
      files: PageResult<FileType>;
    }
  | { type: "loading-error"; error: Problem | string }
  | { type: "refreshing"; refreshing: boolean };

function logUnexpectedAction(state: State, action: Action) {
  console.log(`Unexpected action '${action.type} on state '${state.tag}'`);
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
          tag: "loaded",
          files: action.files,
          refreshing: false,
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
        return { tag: "begin", refreshing: action.refreshing };
      }
      return state;
  }
}

const firstState: State = {
  tag: "begin",
  refreshing: false,
};

// User info key
const KEY_NAME = "user_info";

const FilesScreen = () => {
  const { username, setUsername, setIsLogged } = useAuthentication();
  const [state, dispatch] = useReducer(reducer, firstState);
  const router = useRouter();

  const loadData = async () => {
    try {
      dispatch({ type: "start-loading" });
      console.log("FILE API GETFILE");
      const files = await getFiles();
      dispatch({ type: "loading-success", files });
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
    dispatch({ type: "refreshing", refreshing: true });
  };

  const files =
    state.tag === "loaded" && Array.isArray(state.files.content)
      ? state.files.content
      : [];

  const refreshing = state.tag === "loaded" && state.refreshing;

  return (
    <SafeAreaView className="bg-primary h-full">
      {state.tag === "loaded" ? (
        <FlatList
          data={files}
          keyExtractor={(item, index) => String(item.fileId || index)}
          renderItem={({ item }) => <FileItemComponent item={item} />}
          contentContainerStyle={{ paddingBottom: 80 }}
          ListHeaderComponent={() => (
            <View className="my-6 px-4 space-y-6">
              <View className="justify-between items-start flex-row mb-6">
                <View>
                  <Text className="text-2xl font-psemibold text-white">
                    Files
                  </Text>
                </View>

                <View className="mt-1.5">
                  <Image
                    source={icons.filter_white}
                    className="w-[18px] h-[20px]"
                    resizeMode="contain"
                  />
                </View>
              </View>
              <SearchInput />
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
    </SafeAreaView>
  );
};

export default FilesScreen;
