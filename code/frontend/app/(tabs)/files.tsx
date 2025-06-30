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
import SearchInput from "@/components/SearchBar";
import { icons } from "@/constants";
import { KEY_NAME, useAuthentication } from "@/context/AuthProvider";
import React, { useEffect, useReducer, useRef, useState } from "react";
import { PageResult } from "@/domain/utils/PageResult";
import { File } from "@/domain/storage/File";
import {
  getProblemMessage,
  isProblem,
  Problem,
} from "@/services/media/Problem";
import { useRouter } from "expo-router";
import { getFiles } from "@/services/storage/StorageService";
import { removeValueFor } from "@/services/storage/SecureStorage";
import FileItemComponent from "@/components/FileItemComponent";
import SortSelector, {
  SortOption,
  sortOptions,
} from "@/components/SortSelector";
import BottomSheet from "@gorhom/bottom-sheet";

// The State
type State =
  | { tag: "begin"; refreshing: boolean; sort: SortOption }
  | { tag: "loading"; refreshing: boolean; sort: SortOption }
  | {
      tag: "loaded";
      files: PageResult<File>;
      refreshing: boolean;
      sort: SortOption;
    }
  | { tag: "error"; error: Problem | string };

// The Action
type Action =
  | { type: "start-loading" }
  | {
      type: "loading-success";
      files: PageResult<File>;
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

const FilesScreen = () => {
  const { token, setUsername, setIsLogged } = useAuthentication();
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
      const files = await getFiles(token, sort.sortBy);
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
    dispatch({ type: "refreshing", refreshing: true, sort: sort });
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
                  <TouchableOpacity
                    onPress={openSortSheet}
                    className="borderrounded-full p-2 ml-2"
                    activeOpacity={0.85}
                  >
                    <Image
                      source={icons.filter_white}
                      className="w-[18px] h-[20px]"
                      resizeMode="contain"
                    />
                  </TouchableOpacity>
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
      <SortSelector ref={bottomSheetRef} onSortChange={handleSelectSort} />
    </SafeAreaView>
  );
};

export default FilesScreen;
