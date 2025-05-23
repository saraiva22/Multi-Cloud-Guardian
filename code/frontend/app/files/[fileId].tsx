import {
  ActivityIndicator,
  Alert,
  Text,
  View,
  Image,
  TouchableOpacity,
} from "react-native";
import React, { useEffect, useReducer } from "react";
import { router, useLocalSearchParams } from "expo-router";
import {
  downloadFile,
  getFile,
  processAndSaveDownloadedFile,
} from "@/services/storage/StorageService";
import { FileOutputModel } from "@/services/storage/model/FileOutputModel";
import { SafeAreaView } from "react-native-safe-area-context";
import {
  getProblemMessage,
  isProblem,
  Problem,
} from "@/services/media/Problem";
import { useAuthentication } from "@/context/AuthProvider";
import { removeValueFor } from "@/services/storage/SecureStorage";
import { icons } from "@/constants";
import CustomButton from "@/components/CustomButton";
import { formatDate, formatSize } from "@/services/utils/Function";
import { DownloadOutputModel } from "@/services/storage/model/DownloadOutputModel";

// The State
type State =
  | { tag: "begin" }
  | { tag: "loading" }
  | {
      tag: "loaded";
      details: FileOutputModel;
      downloadFile?: DownloadOutputModel | null;
    }
  | { tag: "redirect" }
  | { tag: "error"; error: Problem | string };

// The Action
type Action =
  | { type: "start-loading" }
  | { type: "loading-success"; details: FileOutputModel }
  | { type: "loading-error"; error: Problem | string }
  | { type: "download-loading"; details: FileOutputModel }
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

type FileInfoProps = {
  fileInfo: FileOutputModel;
  state: State;
  handleDownload: () => Promise<any>;
};

const FileInfo = ({ fileInfo, state, handleDownload }: FileInfoProps) => (
  <SafeAreaView className="flex-1 bg-primary h-full px-6 py-12">
    <TouchableOpacity
      className="absolute left-6 top-8 z-10 mt-10"
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
        source={icons.image_icon}
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
      {fileInfo.folderId && (
        <Text className="text-[16px] text-zinc-300">
          Folder ID: {fileInfo.folderId}
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
    </View>
  </SafeAreaView>
);

const FileDetails = () => {
  const { fileId } = useLocalSearchParams();
  const [state, dispatch] = useReducer(reducer, firstState);
  const { keyMaster, setIsLogged, setUsername } = useAuthentication();

  async function handleDownload() {
    if (state.tag !== "loaded") return;
    dispatch({ type: "download-loading", details: state.details });

    try {
      const result = await downloadFile(fileId.toString());
      await processAndSaveDownloadedFile(result, keyMaster);
      dispatch({ type: "loading-success", details: state.details });
    } catch (error) {
      dispatch({ type: "loading-error", error: error });
    }
  }

  async function handleDelete() {
    if (state.tag !== "loaded") return;
  }

  const fetchFileDetails = async () => {
    dispatch({ type: "start-loading" });
    try {
      console.log("PEDIDO GETFILE")
      const details = await getFile(fileId.toString());
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

  useEffect(() => {
    if (state.tag === "begin") {
      fetchFileDetails();
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
      router.replace(`/files/${fileId}`);
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
          state={state}
          handleDownload={handleDownload}
        />
      );
    }
  }
};

export default FileDetails;
