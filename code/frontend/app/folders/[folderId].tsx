import {
  ActivityIndicator,
  Alert,
  Text,
  View,
  Image,
  TouchableOpacity,
  SafeAreaView,
  FlatList,
} from "react-native";
import React, { useEffect, useReducer } from "react";
import { router, useLocalSearchParams } from "expo-router";
import {
  deleteFile,
  deleteFolder,
  downloadFile,
  generateTemporaryUrl,
  getFile,
  getFiles,
  getFilesInFolder,
  getFolder,
  getFolders,
  getFoldersInFolder,
  processAndSaveDownloadedFile,
} from "@/services/storage/StorageService";
import { FolderOutputModel } from "@/services/storage/model/FolderOutputModel";
import {
  getProblemMessage,
  isProblem,
  Problem,
} from "@/services/media/Problem";
import CustomButton from "@/components/CustomButton";
import icons from "@/constants/icons";
import { formatDate, formatSize } from "@/services/utils/Function";
import { removeValueFor } from "@/services/storage/SecureStorage";
import { useAuthentication } from "@/context/AuthProvider";
import { FileType } from "@/domain/storage/FileType";
import { PageResult } from "@/domain/utils/PageResult";
import { FolderType } from "@/domain/storage/FolderType";
import FileItemComponent from "@/components/FileItemComponent";
import FolderCard from "@/components/FolderCard";
import EmptyState from "@/components/EmptyState";

// The State
type State =
  | { tag: "begin" }
  | { tag: "loading" }
  | {
      tag: "loaded";
      details: FolderOutputModel;
      files: PageResult<FileType>;
      folders: PageResult<FolderType>;
    }
  | { tag: "redirect" }
  | { tag: "error"; error: Problem | string };

// The Action
type Action =
  | { type: "start-loading" }
  | {
      type: "loading-success";
      details: FolderOutputModel;
      files: PageResult<FileType>;
      folders: PageResult<FolderType>;
    }
  | { type: "loading-error"; error: Problem | string }
  | { type: "download-loading"; details: FolderOutputModel }
  | { type: "url-loading"; details: FolderOutputModel }
  | { type: "delete-loading" }
  | { type: "success-delete" };

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
          details: action.details,
          files: action.files,
          folders: action.folders,
        };
      } else if (action.type === "loading-error") {
        return { tag: "error", error: action.error };
      } else if (action.type === "success-delete") {
        return { tag: "redirect" };
      } else {
        logUnexpectedAction(state, action);
        return state;
      }

    case "loaded":
      if (action.type === "download-loading") {
        return { tag: "loading" };
      } else if (action.type === "url-loading") {
        return { tag: "loading" };
      } else if (action.type === "delete-loading") {
        return { tag: "loading" };
      } else {
        logUnexpectedAction(state, action);
        return state;
      }
    case "error":
      logUnexpectedAction(state, action);
      return state;
    case "redirect":
      logUnexpectedAction(state, action);
      return state;
  }
}

const firstState: State = {
  tag: "begin",
};

const KEY_NAME = "user_info";

type FolderInfoProps = {
  folderInfo: FolderOutputModel;
  state: State;
  handleDelete: () => Promise<any>;
};

const FolderInfo = ({ folderInfo, state, handleDelete }: FolderInfoProps) => (
  <>
    <TouchableOpacity
      className="absolute left-6 z-10 mt-6"
      onPress={() => router.back()}
      hitSlop={12}
    >
      <Image
        source={icons.back}
        className="w-6 h-6"
        resizeMode="contain"
        tintColor="white"
      />
    </TouchableOpacity>

    <Text className="text-[24px] font-semibold text-white text-center mb-16 mt-4">
      Folder Details
    </Text>

    <View className="items-center mb-12">
      <Image
        source={icons.folder}
        className="w-20 h-20 mb-8"
        resizeMode="contain"
      />
      <Text className="text-[24px] font-semibold text-white mb-4">
        Created Folder: {folderInfo.user.username}
      </Text>
      <Text className="text-[16px] text-zinc-300">
        Folder Name: {folderInfo.folderName}
      </Text>
      <Text className="text-[16px] text-zinc-300">
        Folder Size: {formatSize(folderInfo.size)}
      </Text>
      <Text className="text-[16px] text-zinc-300">
        Numbers File: {folderInfo.numberFile}
      </Text>
      <Text className="text-[16px] text-zinc-300">
        Created At: {formatDate(folderInfo.createdAt)}
      </Text>
    </View>

    <View>
      <CustomButton
        title="Delete"
        handlePress={handleDelete}
        containerStyles="w-full mb-4 bg-secondary-200 rounded-lg py-4"
        textStyles="text-black text-center font-bold"
        isLoading={state.tag === "loading"}
        color="border-secondary"
      />
    </View>
  </>
);

const FolderDetails = () => {
  const { folderId } = useLocalSearchParams();
  const [state, dispatch] = useReducer(reducer, firstState);
  const { keyMaster, setIsLogged, setUsername } = useAuthentication();

  const fetchFileDetails = async () => {
    dispatch({ type: "start-loading" });
    try {
      const details = await getFolder(folderId.toString());
      const files = await getFilesInFolder(folderId.toString());
      const folders = await getFoldersInFolder(folderId.toString());
      dispatch({ type: "loading-success", details, files, folders });
    } catch (error) {
      dispatch({ type: "loading-error", error: error });
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
    }
  };

  async function handleDelete() {
    if (state.tag !== "loaded") return;
    dispatch({ type: "delete-loading" });
    try {
      await deleteFolder(folderId.toString());
      dispatch({ type: "success-delete" });
    } catch (error) {
      dispatch({ type: "loading-error", error: error });
    }
  }

  useEffect(() => {
    if (state.tag === "begin") {
      fetchFileDetails();
    }
    if (state.tag === "redirect") {
      router.replace("/folders");
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
      router.replace(`/folders/${folderId}`);
    }
  }, [state.tag]);

  switch (state.tag) {
    case "begin":
      return (
        <SafeAreaView className="bg-primary flex-1">
          <ActivityIndicator />
        </SafeAreaView>
      );
    case "loading":
      return (
        <SafeAreaView className="bg-primary flex-1">
          <ActivityIndicator size="large" color="#FFFFFF" />
          <Text className="mt-4 text-white text-base">Loading File...</Text>
        </SafeAreaView>
      );

    case "loaded": {
      return (
        <SafeAreaView className="flex-1 bg-primary h-full px-6 py-12">
          <FlatList
            data={state.files.content}
            keyExtractor={(item) => String(item.fileId)}
            renderItem={({ item }) => <FileItemComponent item={item} />}
            contentContainerStyle={{ paddingBottom: 80 }}
            showsVerticalScrollIndicator={false}
            ListHeaderComponent={() => (
              <View className="my-6 px-4 space-y-6">
                <FolderInfo
                  folderInfo={state.details}
                  state={state}
                  handleDelete={handleDelete}
                />
                <View className="w-full flex-1 pt-5 pb-8">
                  <View className="flex-row items-center justify-between mb-3">
                    <Text className="text-2xl font-bold text-gray-100 mb-3">
                      Folders in {`${state.details.folderName}`}
                    </Text>
                  </View>

                  <FolderCard folders={state.folders.content} />
                </View>
                <View className="w-full flex-1 pt-2">
                  <Text className="text-2xl font-bold text-gray-100">
                    Files in {`${state.details.folderName}`}
                  </Text>
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
          />
        </SafeAreaView>
      );
    }
  }
};

export default FolderDetails;
