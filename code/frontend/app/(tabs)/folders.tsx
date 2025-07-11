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
import React, { act, useEffect, useReducer, useRef, useState } from "react";
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
import { OwnershipFilter } from "@/domain/storage/OwnershipFilter";
import OwnershipSelector, {
  OwnershipOption,
  ownershipOptions,
} from "@/components/OwnershipSelector";

// The State
type State =
  | {
      tag: "begin";
      refreshing: boolean;
      sort: SortOption;
      filter: OwnershipOption;
      search: string;
    }
  | {
      tag: "loading";
      refreshing: boolean;
      sort: SortOption;
      filter: OwnershipOption;
      search: string;
    }
  | {
      tag: "loaded";
      folders: PageResult<Folder>;
      refreshing: boolean;
      sort: SortOption;
      filter: OwnershipOption;
      inputs: {
        searchValue: string;
      };
    }
  | { tag: "error"; error: Problem };

// The Action
type Action =
  | { type: "start-loading" }
  | {
      type: "loading-success";
      folders: PageResult<Folder>;
    }
  | {
      type: "edit";
      inputName: string | number;
      inputValue: string | number;
    }
  | { type: "loading-error"; error: Problem }
  | {
      type: "refreshing";
      refreshing: boolean;
      sort: SortOption;
      filter: OwnershipOption;
    }
  | { type: "search"; searchValue: string };

// The Logger
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
          filter: state.filter,
        };
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
          filter: state.filter,
          inputs: {
            searchValue: "",
          },
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
      switch (action.type) {
        case "refreshing":
          return {
            tag: "begin",
            refreshing: action.refreshing,
            sort: action.sort,
            filter: action.filter,
            search: "",
          };
        case "edit":
          return {
            tag: "loaded",
            folders: state.folders,
            refreshing: false,
            sort: state.sort,
            filter: state.filter,
            inputs: {
              ...state.inputs,
              [action.inputName]: action.inputValue,
            },
          };
        case "search":
          return {
            tag: "begin",
            refreshing: false,
            sort: state.sort,
            filter: state.filter,
            search: action.searchValue,
          };
        default:
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
  filter: ownershipOptions[0],
};

const SIZE_MIN_FOLDER = 2;

const FoldersScreen = () => {
  const { token, setUsername, setIsLogged } = useAuthentication();
  const [state, dispatch] = useReducer(reducer, firstState);
  const router = useRouter();
  const bottomSheetRef = useRef<BottomSheet>(null);
  const filterSheetRef = useRef<BottomSheet>(null);
  const [isVerticalLayout, setIsVerticalLayout] = useState(false);

  const sort = state.tag === "error" ? sortOptions[0] : state.sort;

  const filter = state.tag === "error" ? ownershipOptions[0] : state.filter;

  const search =
    state.tag === "loading" && state.search
      ? state.search
      : state?.search || "";

  const openSortSheet = () => {
    bottomSheetRef.current?.expand();
  };

  const openFilterSheet = () => {
    filterSheetRef.current?.expand();
  };

  const handleSelectSort = (sort: SortOption) => {
    bottomSheetRef.current?.close();
    dispatch({
      type: "refreshing",
      refreshing: true,
      sort: sort,
      filter: filter,
    });
  };

  const handleSelectFilter = (filter: OwnershipOption) => {
    filterSheetRef.current?.close();
    dispatch({
      type: "refreshing",
      refreshing: true,
      sort: sort,
      filter: filter,
    });
  };

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
      const folders = await getFolders(
        token,
        sort.sortBy,
        undefined,
        filter.value,
        search
      );
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
    setTimeout(() => {
      dispatch({
        type: "refreshing",
        refreshing: true,
        sort: sort,
        filter: filter,
      });
    }, 200);
  };

  const searchValue =
    state.tag === "loaded" && state.inputs.searchValue
      ? state.inputs.searchValue
      : state.inputs?.searchValue || "";

  // Debounced search effect
  useEffect(() => {
    if (state.tag !== "loaded") return;
    const value = searchValue.trim();
    if (value.length <= SIZE_MIN_FOLDER) {
      return;
    }

    const timeoutId = setTimeout(async () => {
      dispatch({ type: "search", searchValue: value });
    }, 500);

    return () => clearTimeout(timeoutId);
  }, [searchValue]);

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
        <SafeAreaView className="bg-primary flex-1 justify-center items-center">
          <ActivityIndicator size="large" color="#fff" />
          <Text className="text-[24px] font-semibold text-white text-center mb-16 mt-4">
            {state.tag === "error" && state.error.detail}
          </Text>
        </SafeAreaView>
      );

    case "loaded":
      return (
        <SafeAreaView className="bg-primary h-full">
          <View>
            <View className="my-6 px-5 space-y-6">
              <View className="justify-between items-start flex-row mb-6">
                <Text className="text-2xl font-psemibold text-white">
                  Folders
                </Text>
                <View className="mt-1.5 flex-row gap-2">
                  <TouchableOpacity
                    onPress={openFilterSheet}
                    activeOpacity={0.85}
                  >
                    <Image
                      source={icons.filter}
                      className="w-[18px] h-[20px]"
                      style={{ tintColor: "#fff" }}
                      resizeMode="contain"
                    />
                  </TouchableOpacity>
                  <TouchableOpacity
                    onPress={openSortSheet}
                    activeOpacity={0.85}
                  >
                    <Image
                      source={icons.sort_white}
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
              <SearchInput
                placeholder="Folders"
                value={searchValue}
                onChangeText={(text) => handleChange("searchValue", text)}
              />
            </View>

            <FlatList
              className="mt-5"
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
              ListEmptyComponent={() => (
                <View className="flex-1 items-center justify-center py-10">
                  <Text className="text-white text-lg font-semibold mb-2 text-center">
                    No foles match your search
                  </Text>
                </View>
              )}
              refreshControl={
                <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
              }
            />
          </View>

          <OwnershipSelector
            ref={filterSheetRef}
            onOwnershipChange={handleSelectFilter}
          />
          <SortSelector ref={bottomSheetRef} onSortChange={handleSelectSort} />
        </SafeAreaView>
      );
  }
};

export default FoldersScreen;
