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
import {
  getProblemMessage,
  isProblem,
  Problem,
} from "@/services/media/Problem";
import { Folder } from "@/domain/storage/Folder";
import { useRouter } from "expo-router";
import { getFolders } from "@/services/storage/StorageService";
import { removeValueFor } from "@/services/storage/SecureStorage";
import FolderItemComponent from "@/components/FolderItemComponent";
import SortSelector, {
  SortOption,
  sortOptions,
} from "@/components/SortSelector";
import BottomSheet from "@gorhom/bottom-sheet";
import FolderItemGrid from "@/components/FolderItemGrid";
// The State
type State =
  | { tag: "begin"; refreshing: boolean; sort: SortOption }
  | { tag: "loading"; refreshing: boolean; sort: SortOption }
  | {
      tag: "loaded";
      folders: PageResult<Folder>;
      refreshing: boolean;
      sort: SortOption;
    }
  | { tag: "error"; error: Problem | string };

// The Action
type Action =
  | { type: "start-loading" }
  | {
      type: "loading-success";
      folders: PageResult<Folder>;
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



const FoldersScreen = () => {
  const { username, setUsername, setIsLogged } = useAuthentication();
  const [state, dispatch] = useReducer(reducer, firstState);
  const router = useRouter();
  const bottomSheetRef = useRef<BottomSheet>(null);
  const [isVerticalLayout, setIsVerticalLayout] = useState(false);

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
      const folders = await getFolders(sort.sortBy);
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
    dispatch({ type: "refreshing", refreshing: true, sort: sort });
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
          horizontal={false}
          numColumns={isVerticalLayout ? 1 : 2}
          key={isVerticalLayout ? "grid" : "list"}
          keyExtractor={(item, index) => String(item.folderId || index)}
          columnWrapperStyle={
            !isVerticalLayout && {
              justifyContent: "space-between",
              paddingHorizontal: 16,
            }
          }
          renderItem={({ item }) =>
            isVerticalLayout ? (
              <FolderItemComponent item={item} />
            ) : (
              <FolderItemGrid item={item} />
            )
          }
          contentContainerStyle={{
            paddingBottom: 80,
            paddingHorizontal: isVerticalLayout ? 0 : 4,
          }}
          ListHeaderComponent={() => (
            <View className="my-6 px-4 space-y-6">
              <View className="justify-between items-start flex-row mb-6">
                <Text className="text-2xl font-psemibold text-white">
                  Folders
                </Text>
                <View className="mt-1.5 flex-row gap-2">
                  <TouchableOpacity
                    onPress={openSortSheet}
                    activeOpacity={0.85}
                  >
                    <Image
                      source={icons.filter_white}
                      className="w-[18px] h-[20px]"
                      resizeMode="contain"
                    />
                  </TouchableOpacity>
                  <TouchableOpacity
                    onPress={() => setIsVerticalLayout(!isVerticalLayout)}
                    activeOpacity={0.85}
                  >
                    <Image
                      source={icons.org_white}
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
                title="No folders found"
                subtitle="Try creating a new folder or adjust your search"
                page="/(modals)/create-folder"
                titleButton="Create Folder"
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

export default FoldersScreen;
