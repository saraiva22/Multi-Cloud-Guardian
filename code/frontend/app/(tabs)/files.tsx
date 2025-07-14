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
import { useFocusEffect, useRouter } from "expo-router";
import { getFiles } from "@/services/storage/StorageService";
import { removeValueFor } from "@/services/storage/SecureStorage";
import FileItemComponent from "@/components/FileItemComponent";
import SortSelector, {
  SortOption,
  sortOptions,
} from "@/components/SortSelector";
import BottomSheet from "@gorhom/bottom-sheet";
import MoveBottomSheet from "@/components/MoveBottomSheet";
import { FolderType } from "@/domain/storage/FolderType";
import FilterSelector, {
  FilterOption,
  filterOptions,
} from "@/components/FilterFolderSelector";

// The State
type State =
  | {
      tag: "begin";
      refreshing: boolean;
      sort: SortOption;
      filter: FilterOption;
      search: string;
      inputs: {
        searchValue: string;
      };
    }
  | {
      tag: "loading";
      refreshing: boolean;
      sort: SortOption;
      filter: FilterOption;
      search: string;
      inputs: {
        searchValue: string;
      };
    }
  | {
      tag: "loaded";
      files: PageResult<File>;
      refreshing: boolean;
      sort: SortOption;
      search: string;
      filter: FilterOption;
      inputs: {
        searchValue: string;
      };
      isFetchingMore: boolean;
    }
  | { tag: "error"; error: Problem };

// The Action
type Action =
  | { type: "start-loading" }
  | {
      type: "loading-success";
      files: PageResult<File>;
    }
  | {
      type: "edit";
      inputName: string | number;
      inputValue: string | number;
    }
  | { type: "loading-error"; error: Problem }
  | { type: "fetch-more-start" }
  | { type: "fetch-more-success"; files: PageResult<File> }
  | {
      type: "refreshing";
      refreshing: boolean;
      sort: SortOption;
      filter: FilterOption;
    }
  | { type: "search"; search: string }
  | { type: "delete-select-file"; file: File }
  | {
      type: "reset";
    };

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
          refreshing: false,
          sort: state.sort,
          filter: state.filter,
          inputs: {
            searchValue: state.inputs.searchValue,
          },
          search: state.search,
          isFetchingMore: false,
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
            inputs: {
              searchValue: "",
            },
          };
        case "edit":
          return {
            tag: "loaded",
            files: state.files,
            refreshing: false,
            sort: state.sort,
            filter: state.filter,
            inputs: {
              ...state.inputs,
              [action.inputName]: action.inputValue,
            },
            search: state.search,
            isFetchingMore: state.isFetchingMore,
          };

        case "search":
          return {
            tag: "begin",
            refreshing: false,
            sort: state.sort,
            filter: state.filter,
            search: action.search,
            inputs: {
              searchValue: state.inputs.searchValue,
            },
          };
        case "fetch-more-start":
          return {
            ...state,
            isFetchingMore: true,
          };
        case "fetch-more-success":
          return {
            ...state,
            files: {
              ...action.files,
              content: [...state.files.content, ...action.files.content],
            },
            isFetchingMore: false,
          };
        case "reset":
          return {
            tag: "begin",
            refreshing: false,
            sort: sortOptions[0],
            search: "",
            filter: filterOptions[0],
            inputs: {
              searchValue: "",
            },
          };
        case "delete-select-file":
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
  filter: filterOptions[0],
  inputs: {
    searchValue: "",
  },
};

const SIZE_MIN_FILE = 2;

const FilesScreen = () => {
  const { token, setUsername, setIsLogged } = useAuthentication();
  const [state, dispatch] = useReducer(reducer, firstState);
  const router = useRouter();
  const bottomSheetRef = useRef<BottomSheet>(null);
  const moveSheetRef = useRef<BottomSheet>(null);
  const filterSheetRef = useRef<BottomSheet>(null);
  const [selectFile, setSelectFile] = useState<File | null>(null);

  const sort = state.tag === "error" ? sortOptions[0] : state.sort;

  const filter = state.tag === "error" ? filterOptions[0] : state.filter;

  const search = state.tag === "error" ? "" : state.search;

  const openSortSheet = () => {
    filterSheetRef.current?.close();
    bottomSheetRef.current?.expand();
  };

  const openFilterSheet = () => {
    bottomSheetRef.current?.close();
    filterSheetRef.current?.expand();
  };

  const openMoveSheet = (file: File) => {
    setSelectFile(file);
    moveSheetRef.current?.expand();
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

  const handleSelectFilter = (filter: FilterOption) => {
    filterSheetRef.current?.close();
    dispatch({
      type: "refreshing",
      refreshing: true,
      sort: sort,
      filter: filter,
    });
  };

  useEffect(() => {
    if (state.tag === "begin") {
      loadData();
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
      const filterValue = filter.value || undefined;
      const files = await getFiles(token, sort.sortBy, search, filterValue);

      dispatch({ type: "loading-success", files });
    } catch (error) {
      Alert.alert(
        "Error",
        `${isProblem(error) ? getProblemMessage(error) : error}`
      );
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
      const filterValue = filter.value || undefined;

      const moreFiles = await getFiles(
        token,
        sort.sortBy,
        search,
        filterValue,
        nextPage
      );

      dispatch({ type: "fetch-more-success", files: moreFiles });
    } catch (error) {
      dispatch({ type: "loading-error", error: error });
    }
  };

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
    state.tag === "loaded" && Array.isArray(state.files?.content)
      ? state.files.content
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
          <ActivityIndicator size="large" color="#fff" />
          <Text className="text-[24px] font-semibold text-white text-center mb-16 mt-4">
            {state.tag === "error" && state.error.detail}
          </Text>
        </SafeAreaView>
      );
    case "loaded":
      return (
        <SafeAreaView className="bg-primary h-full">
          <View style={{ flex: 1 }}>
            <View className="my-6 px-5 space-y-6">
              <View className="justify-between items-start flex-row mb-6">
                <Text className="text-2xl font-psemibold text-white">
                  Files
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
                    activeOpacity={0.85}
                    onPress={() => dispatch({ type: "reset" })}
                  >
                    <Image
                      source={icons.reset}
                      className="w-[18px] h-[20px]"
                      resizeMode="contain"
                      tintColor="white"
                    />
                  </TouchableOpacity>
                </View>
              </View>
              <SearchInput
                placeholder="Files"
                value={searchValue}
                onChangeText={(text) => handleChange("searchValue", text)}
              />
            </View>

            <FlatList
              className="mt-5"
              data={files}
              keyExtractor={(item) => item.fileId.toString()}
              renderItem={({ item }) => (
                <FileItemComponent
                  item={item}
                  onMovePress={() => openMoveSheet(item)}
                />
              )}
              style={{ flex: 1 }}
              contentContainerStyle={{ paddingBottom: 90 }}
              onEndReached={fetchMoreFiles}
              onEndReachedThreshold={0.1}
              ListEmptyComponent={() => (
                <View className="flex-1 items-center justify-center py-10">
                  <Text className="text-white text-lg font-semibold mb-2 text-center">
                    No files match your search
                  </Text>
                </View>
              )}
              ListFooterComponent={() =>
                state.isFetchingMore ? (
                  <View className="bg-primary py-4 justify-center items-center">
                    <ActivityIndicator size="small" color="#fff" />
                  </View>
                ) : null
              }
              refreshControl={
                <RefreshControl
                  className="color-white"
                  refreshing={refreshing}
                  onRefresh={onRefresh}
                />
              }
            />
          </View>

          <SortSelector ref={bottomSheetRef} onSortChange={handleSelectSort} />
          <FilterSelector
            ref={filterSheetRef}
            onFilterChange={handleSelectFilter}
          />
          <MoveBottomSheet
            ref={moveSheetRef}
            file={selectFile}
            onDelete={(file) => dispatch({ type: "delete-select-file", file })}
          />
        </SafeAreaView>
      );
  }
};

export default FilesScreen;
