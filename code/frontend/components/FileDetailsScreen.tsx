import {
  ActivityIndicator,
  Alert,
  Text,
  View,
  Image,
  TouchableOpacity,
} from "react-native";
import React, { useEffect, useReducer } from "react";
import { router } from "expo-router";
import {
  generateTemporaryUrl,
  processAndSaveDownloadedFile,
} from "@/services/storage/StorageService";
import { FileOutputModel } from "@/services/storage/model/FileOutputModel";
import { SafeAreaView } from "react-native-safe-area-context";
import {
  getProblemMessage,
  isProblem,
  Problem,
} from "@/services/media/Problem";
import { KEY_NAME, useAuthentication } from "@/context/AuthProvider";
import { removeValueFor } from "@/services/storage/SecureStorage";
import { icons } from "@/constants";
import CustomButton from "@/components/CustomButton";
import { formatDate, formatSize } from "@/services/utils/Function";
import * as Clipboard from "expo-clipboard";
import { FolderType } from "@/domain/storage/FolderType";

// The State
type State =
  | { tag: "begin" }
  | { tag: "loading" }
  | {
      tag: "loaded";
      details: FileOutputModel;
    }
  | { tag: "redirect" }
  | { tag: "error"; error: Problem | string };

// The Action
type Action =
  | { type: "start-loading" }
  | { type: "loading-success"; details: FileOutputModel }
  | { type: "loading-error"; error: Problem | string }
  | { type: "download-loading"; details: FileOutputModel }
  | { type: "url-loading"; details: FileOutputModel }
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

type FileInfoProps = {
  fileInfo: FileOutputModel;
  username: string;
  owner: string;
  state: State;
  handleDownload: (func: () => void) => Promise<any>;
  handleDelete: (func: () => void) => Promise<any>;
  handleGenerateTemporaryUrl: () => Promise<any>;
};

const FileInfo = ({
  fileInfo,
  username,
  owner,
  state,
  handleDownload,
  handleDelete,
  handleGenerateTemporaryUrl,
}: FileInfoProps) => (
  <SafeAreaView className="flex-1 bg-primary h-full px-6 py-12">
    <TouchableOpacity
      className="absolute left-6 top-6 z-10 mt-28"
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
      File Details
    </Text>

    <View className="items-center mb-12">
      <Image
        source={
          fileInfo.contentType.startsWith("image")
            ? icons.image_icon
            : icons.document
        }
        className="w-[100px] h-[100px] mb-8"
        resizeMode="contain"
      />
      <Text className="text-[24px] font-semibold text-white mb-4">
        Created File: {fileInfo.user.username}
      </Text>
      <Text className="text-[16px] text-zinc-300">
        File Name: {fileInfo.name}
      </Text>
      <Text className="text-[16px] text-zinc-300">
        File Size: {formatSize(fileInfo.size)}
      </Text>
      <Text className="text-[16px] text-zinc-300">
        Created At: {formatDate(fileInfo.createdAt)}
      </Text>
      <Text className="text-[16px] text-zinc-300">
        Encrypted: {fileInfo.encryption ? "Yes" : "No"}
      </Text>
      {fileInfo.folderInfo != null && (
        <Text className="text-[16px] text-zinc-300">
          ParentFolder Name: {fileInfo.folderInfo.folderName}
        </Text>
      )}
      {fileInfo.folderInfo != null && (
        <Text className="text-[16px] text-zinc-300">
          ParentFolder Type: {fileInfo.folderInfo.folderType}
        </Text>
      )}
    </View>

    <View>
      <CustomButton
        title="Download"
        handlePress={handleDownload}
        containerStyles="w-full mb-4 bg-secondary-200 rounded-lg py-4"
        textStyles="text-black text-center font-bold"
        isLoading={state.tag === "loading"}
        color="border-secondary"
      />
      {fileInfo.encryption === false &&
        fileInfo.folderInfo?.folderType !== FolderType.SHARED && (
          <CustomButton
            title="Generate Temporary URL "
            handlePress={handleGenerateTemporaryUrl}
            containerStyles="w-full mb-4 bg-secondary-200 rounded-lg py-4"
            textStyles="text-black text-center font-bold"
            isLoading={state.tag === "loading"}
            color="border-secondary"
          />
        )}
      {(fileInfo.user.username === username ||
        (fileInfo.folderInfo?.folderType === FolderType.SHARED &&
          owner === username)) && (
        <CustomButton
          title="Delete"
          handlePress={handleDelete}
          containerStyles="w-full mb-4 bg-secondary-200 rounded-lg py-4"
          textStyles="text-black text-center font-bold"
          isLoading={state.tag === "loading"}
          color="border-secondary"
        />
      )}
    </View>
    {state.tag === "loaded" &&
      fileInfo.encryption === false &&
      fileInfo.url !== null && (
        <View className="w-full mb-4 rounded-lg py-4">
          <Text className="text-white text-center text-xl mb-2 font-medium">
            Temporary access link generated
          </Text>
          <TouchableOpacity
            onPress={async () => {
              await Clipboard.setStringAsync(`${fileInfo.url}`);
              Alert.alert(
                "Link Copied",
                "You can now paste the link anywhere to share temporary access to this file."
              );
            }}
            className="bg-tertiary rounded-xl py-3 px-6 active:opacity-80"
          >
            <Text className="text-white text-center font-bold text-xl">
              📋 Copy Link
            </Text>
          </TouchableOpacity>
        </View>
      )}
  </SafeAreaView>
);

type Props = {
  fileId: string;
  owner: string;
  getFileFunc: (fileId: string, token: string) => Promise<FileOutputModel>;
  downloadFunc: (fileId: string, token: string) => Promise<any>;
  deleteFunc: (fileId: string, token: string) => Promise<any>;
};

const FileDetailsScreen = ({
  fileId,
  owner,
  getFileFunc,
  downloadFunc,
  deleteFunc,
}: Props) => {
  const [state, dispatch] = useReducer(reducer, firstState);
  const { username, token, keyMaster, setUsername, setIsLogged } =
    useAuthentication();

  const fetchFileDetails = async () => {
    dispatch({ type: "start-loading" });
    try {
      const details = await getFileFunc(fileId, token);
      dispatch({ type: "loading-success", details });
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

  async function handleDownload() {
    if (state.tag !== "loaded") return;
    dispatch({ type: "download-loading", details: state.details });

    try {
      const result = await downloadFunc(fileId, token);
      await processAndSaveDownloadedFile(result, keyMaster);
      dispatch({ type: "loading-success", details: state.details });
    } catch (error) {
      dispatch({ type: "loading-error", error: error });
    }
  }

  async function handleDelete() {
    if (state.tag !== "loaded") return;
    dispatch({ type: "delete-loading" });
    try {
      await deleteFunc(fileId, token);
      dispatch({ type: "success-delete" });
    } catch (error) {
      dispatch({ type: "loading-error", error: error });
    }
  }

  async function handleGenerateTemporaryUrl() {
    if (state.tag !== "loaded") return;
    dispatch({ type: "url-loading", details: state.details });
    try {
      const defaultTime = 15; // 15 minutes
      const value = await generateTemporaryUrl(fileId, defaultTime, token);
      dispatch({ type: "loading-success", details: value });
    } catch (error) {
      dispatch({ type: "loading-error", error: error });
    }
  }

  useEffect(() => {
    if (state.tag === "begin") {
      fetchFileDetails();
    }
    if (state.tag === "redirect") {
      router.replace("/files");
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
      router.replace(`/files`);
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
        <FileInfo
          fileInfo={state.details}
          username={username ? username : ""}
          owner={owner}
          state={state}
          handleDownload={handleDownload}
          handleDelete={handleDelete}
          handleGenerateTemporaryUrl={handleGenerateTemporaryUrl}
        />
      );
    }
  }
};

export default FileDetailsScreen;
