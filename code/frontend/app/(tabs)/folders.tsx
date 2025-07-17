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
import OwnershipSelector, {
  OwnershipCombination,
  ownershipOptions,
} from "@/components/OwnershipSelector";
import { UserInfo } from "@/domain/user/UserInfo";
import { File } from "@/domain/storage/File";
import { getSSE } from "@/services/notifications/SSEManager";
import { EventSourceListener } from "react-native-sse";
import { FolderInfo } from "@/domain/storage/FolderInfo";

// The State
type State =
  | {
      tag: "begin";
      refreshing: boolean;
      sort: SortOption;
      filter: OwnershipCombination;
      search: string;
      inputs: {
        searchValue: string;
      };
    }
  | {
      tag: "loading";
      refreshing: boolean;
      sort: SortOption;
      filter: OwnershipCombination;
      search: string;
      inputs: {
        searchValue: string;
      };
    }
  | {
      tag: "loaded";
      folders: PageResult<Folder>;
      refreshing: boolean;
      sort: SortOption;
      search: string;
      filter: OwnershipCombination;
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
      folders: PageResult<Folder>;
    }
  | {
      type: "edit";
      inputName: string | number;
      inputValue: string | number;
    }
  | { type: "loading-error"; error: Problem }
  | { type: "fetch-more-start" }
  | { type: "fetch-more-success"; folders: PageResult<Folder> }
  | {
      type: "refreshing";
      refreshing: boolean;
      sort: SortOption;
      filter: OwnershipCombination;
    }
  | { type: "search"; searchValue: string }
  | {
      type: "new-file";
      folderId: number;
      size: number;
      updatedAt: number;
    }
  | {
      type: "delete-file";
      fileId: number;
      user: UserInfo;
      folderId: number;
      size: number;
      updatedAt: number;
    }
  | { type: "delete-folder"; user: UserInfo; folderInfo: FolderInfo }
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
          folders: action.folders,
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
            folders: state.folders,
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
        case "new-file":
          return {
            ...state,
            folders: {
              ...state.folders,
              content: state.folders.content.map((folder) => {
                if (folder.folderId === action.folderId) {
                  return {
                    ...folder,
                    size: folder.size + action.size,
                    numberFile: folder.numberFile + 1,
                    updatedAt: action.updatedAt,
                  };
                }
                return folder;
              }),
            },
          };
        case "delete-file":
          return {
            ...state,
            folders: {
              ...state.folders,
              content: state.folders.content.map((folder) => {
                if (folder.folderId === action.folderId) {
                  return {
                    ...folder,
                    numberFile: Math.max(folder.numberFile - 1, 0),
                    size: Math.max(folder.size - action.size, 0),
                    updatedAt: action.updatedAt,
                  };
                }
                return folder;
              }),
            },
          };

        case "search":
          return {
            tag: "begin",
            refreshing: false,
            sort: state.sort,
            filter: state.filter,
            search: action.searchValue,
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
            folders: {
              ...action.folders,
              content: [...state.folders.content, ...action.folders.content],
            },
            isFetchingMore: false,
          };
        case "reset":
          return {
            tag: "begin",
            refreshing: false,
            sort: sortOptions[0],
            search: "",
            filter: ownershipOptions[0],
            inputs: {
              searchValue: "",
            },
          };
        case "delete-folder": {
          return {
            ...state,
            tag: "loaded",
            folders: {
              ...state.folders,
              content: state.folders.content.filter(
                (folder) => folder.folderId !== action.folderInfo.folderId
              ),
            },
          };
        }
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
  inputs: {
    searchValue: "",
  },
};

const SIZE_MIN_FOLDER = 2;
const DEFAULT_PAGE_SIZE = 14;
const STARTING_PAGE = 0;

type CustomEventsFile = "file";
type CustomEventDeleteFile = "deleteFile";
type CustomEventDeleteFolder = "deleteFolder";

const FoldersScreen = () => {
  const { token, setUsername, setIsLogged } = useAuthentication();
  const [state, dispatch] = useReducer(reducer, firstState);
  const router = useRouter();
  const bottomSheetRef = useRef<BottomSheet>(null);
  const filterSheetRef = useRef<BottomSheet>(null);
  const [isVerticalLayout, setIsVerticalLayout] = useState(false);
  const listener = getSSE();

  const sort = state.tag === "error" ? sortOptions[0] : state.sort;

  const filter = state.tag === "error" ? ownershipOptions[0] : state.filter;

  const search = state.tag === "error" ? "" : state.search;

  const openSortSheet = () => {
    filterSheetRef.current?.close();
    bottomSheetRef.current?.expand();
  };

  const openFilterSheet = () => {
    bottomSheetRef.current?.close();
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

  const handleSelectFilter = (filter: OwnershipCombination) => {
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

  useEffect(() => {
    if (!listener) return;
    listener.addEventListener("file", handleNewFile);
    listener.addEventListener("deleteFile", handleDeleteFile);
    listener.addEventListener("deleteFolder", handleDeleteFolder);

    return () => {
      listener.removeEventListener("file", handleNewFile);
      listener.removeEventListener("deleteFile", handleDeleteFile);
      listener.removeEventListener("deleteFolder", handleDeleteFolder);
    };
  }, []);

  // Handle EventListener - New File
  const handleNewFile: EventSourceListener<CustomEventsFile> = (event) => {
    if (event.type === "file") {
      const eventData = JSON.parse(event.data);
      dispatch({
        type: "new-file",
        folderId: eventData.folderInfo.folderId,
        size: eventData.size,
        updatedAt: eventData.createdAt,
      });
    }
  };

  // Handle EventListener - Delete File
  const handleDeleteFile: EventSourceListener<CustomEventDeleteFile> = (
    event
  ) => {
    if (event.type === "deleteFile") {
      const eventData = JSON.parse(event.data);
      dispatch({
        type: "delete-file",
        fileId: eventData.fileId,
        user: eventData.user,
        folderId: eventData.folderInfo.folderId,
        size: 0,
        updatedAt: eventData.createdAt,
      });
    }
  };

  // Handle EventListener - Delete Folder
  const handleDeleteFolder: EventSourceListener<CustomEventDeleteFolder> = (
    event
  ) => {
    if (event.type === "deleteFolder") {
      const eventData = JSON.parse(event.data);
      const user = {
        id: eventData.user.id,
        username: eventData.user.username,
        email: eventData.user.email,
      };
      const folderInfo = {
        folderId: eventData.folderInfo.folderId,
        folderName: eventData.folderInfo.folderName,
        folderType: eventData.folderInfo.folderType,
      };

      dispatch({ type: "delete-folder", user, folderInfo });
    }
  };

  const loadData = async () => {
    try {
      dispatch({ type: "start-loading" });
      const folderType = filter.folderType || undefined;
      const folders = await getFolders(
        token,
        sort.sortBy,
        folderType,
        filter.ownership,
        search,
        STARTING_PAGE,
        DEFAULT_PAGE_SIZE
      );
      dispatch({ type: "loading-success", folders });
    } catch (error) {
      Alert.alert(
        "Error",
        `${isProblem(error) ? getProblemMessage(error) : error}`
      );
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
      const folderType = filter.folderType || undefined;

      const moreFolders = await getFolders(
        token,
        sort.sortBy,
        folderType,
        filter.ownership,
        search,
        nextPage,
        DEFAULT_PAGE_SIZE
      );

      dispatch({ type: "fetch-more-success", folders: moreFolders });
    } catch (error) {
      dispatch({ type: "loading-error", error: error });
    }
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
    if (value.length <= SIZE_MIN_FOLDER || value === search) {
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
          <View style={{ flex: 1 }}>
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
              keyExtractor={(item) => String(item.folderId)}
              columnWrapperStyle={
                !isVerticalLayout && {
                  justifyContent: "space-between",
                  paddingHorizontal: 16,
                }
              }
              renderItem={({ item }) =>
                isVerticalLayout ? (
                  <FolderItemComponent item={item} selectedFolderId={null} />
                ) : (
                  <FolderItemGrid item={item} />
                )
              }
              style={{ flex: 1 }}
              contentContainerStyle={{
                paddingBottom: 80,
                paddingHorizontal: isVerticalLayout ? 0 : 4,
              }}
              onEndReached={fetchMoreFolders}
              onEndReachedThreshold={0.1}
              ListEmptyComponent={() => (
                <View className="flex-1 items-center justify-center py-10">
                  <Text className="text-white text-lg font-semibold mb-2 text-center">
                    No foles match your search
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
