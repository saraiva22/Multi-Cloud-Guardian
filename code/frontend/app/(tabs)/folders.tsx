import {
  FlatList,
  RefreshControl,
  View,
  Text,
  Image,
  Alert,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import EmptyState from "@/components/EmptyState";
import SearchInput from "@/components/SearchBar";
import { icons } from "@/constants";
import { useAuthentication } from "@/context/AuthProvider";
import React, { useEffect, useReducer, useState } from "react";
import { PageResult } from "@/domain/utils/PageResult";
import {
  getProblemMessage,
  isProblem,
  Problem,
} from "@/services/media/Problem";
import { FolderType } from "@/domain/storage/FolderType";
import { useRouter } from "expo-router";
import { getFolders } from "@/services/storage/StorageService";
import { removeValueFor } from "@/services/storage/SecureStorage";
import FolderItemComponent from "@/components/FolderItemComponent";

// The State
type State =
  | { tag: "begin"; refreshing: boolean }
  | { tag: "loading" }
  | {
      tag: "loaded";
      folders: PageResult<FolderType>;
      refreshing: boolean;
    }
  | { tag: "error"; error: Problem | string };

// The Action
type Action =
  | { type: "start-loading" }
  | {
      type: "loading-success";
      folders: PageResult<FolderType>;
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
          folders: action.folders,
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

const FoldersScreen = () => {
  const { username, setUsername, setIsLogged } = useAuthentication();
  const [state, dispatch] = useReducer(reducer, firstState);
  const router = useRouter();

  const loadData = async () => {
    try {
      dispatch({ type: "start-loading" });
      const folders = await getFolders();
      dispatch({ type: "loading-success", folders });
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

  const folders =
    state.tag === "loaded" && Array.isArray(state.folders.content)
      ? state.folders.content
      : [];

  const refreshing = state.tag === "loaded" && state.refreshing;

  return (
    <SafeAreaView className="bg-primary h-full">
      {state.tag === "loaded" ? (
        <FlatList
          data={folders}
          keyExtractor={(item, index) => String(item.folderId || index)}
          renderItem={({ item }) => <FolderItemComponent item={item} />}
          contentContainerStyle={{ paddingBottom: 80 }}
          ListHeaderComponent={() => (
            <View className="my-6 px-4 space-y-6">
              <View className="justify-between items-start flex-row mb-6">
                <Text className="text-2xl font-psemibold text-white">
                  Folders
                </Text>
                <View className="mt-1.5 flex-row gap-2">
                  <Image
                    source={icons.filter_white}
                    className="w-[18px] h-[20px]"
                  />
                  <Image
                    source={icons.org_white}
                    className="w-[18px] h-[20px]"
                  />
                </View>
              </View>
              <SearchInput />
            </View>
          )}
          ListEmptyComponent={() => (
            <EmptyState
              title="No folders found"
              subtitle="Try creating a new folder or adjust your search"
              page="/(modals)/create-folder"
              titleButton="Create Folder"
            />
          )}
        />
      ) : null}
    </SafeAreaView>
  );
};

export default FoldersScreen;
