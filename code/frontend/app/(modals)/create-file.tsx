import {
  View,
  Text,
  ScrollView,
  Image,
  TouchableOpacity,
  Alert,
  ActivityIndicator,
} from "react-native";
import React, { useEffect, useReducer, useState } from "react";
import { SafeAreaView } from "react-native-safe-area-context";
import * as DocumentPicker from "expo-document-picker";
import FormField from "@/components/FormField";
import CustomButton from "@/components/CustomButton";
import { icons } from "@/constants";
import { router } from "expo-router";
import EncryptionToggle from "@/components/EncryptionToggle";
import { useAuthentication } from "@/context/AuthProvider";
import {
  getProblemMessage,
  isProblem,
  Problem,
} from "@/services/media/Problem";
import { getFolders, uploadFile } from "@/services/storage/StorageService";
import { PageResult } from "@/domain/utils/PageResult";
import { Folder } from "@/domain/storage/Folder";
import FolderItemComponent from "@/components/FolderItemComponent";
import { FolderType } from "@/domain/storage/FolderType";

// The State
type State =
  | { tag: "begin" }
  | { tag: "loading" }
  | {
      tag: "editing";
      error?: Problem | string;
      inputs: {
        fileName: string;
        fileExtension: string;
        encryption: boolean;
        file: any;
        parentFolderId: number | undefined;
        parentFolderType: FolderType | undefined;
      };
      folders: PageResult<Folder>;
    }
  | { tag: "error"; error: Problem | string }
  | {
      tag: "submitting";
      fileName: string;
      fileExtension: string;
      encryption: boolean;
      file: any;
      parentFolderId: number | undefined;
      parentFolderType: FolderType | undefined;
      folders: PageResult<Folder>;
    }
  | { tag: "redirect" };

// The Action
type Action =
  | { type: "start-loading" }
  | { type: "loading-success"; folders: PageResult<Folder> }
  | { type: "loading-error"; error: Problem | string }
  | { type: "edit"; inputName: string; inputValue: string | boolean | any }
  | { type: "submit" }
  | { type: "error"; message: Problem | string }
  | { type: "success" };

// The Logger
function logUnexpectedAction(state: State, action: Action) {
  console.log(`Unexpected action '${action.type} on state '${state.tag}'`);
  return state;
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
          tag: "editing",
          inputs: {
            fileName: "",
            fileExtension: "",
            encryption: false,
            file: null,
            parentFolderId: undefined,
            parentFolderType: undefined,
          },
          folders: action.folders,
        };
      } else if (action.type === "loading-error") {
        return {
          tag: "error",
          error: action.error,
        };
      } else {
        logUnexpectedAction(state, action);
        return state;
      }
    case "error":
      return state;
    case "editing":
      if (action.type === "edit") {
        return {
          tag: "editing",
          error: undefined,
          inputs: { ...state.inputs, [action.inputName]: action.inputValue },
          folders: state.folders,
        };
      } else if (action.type === "submit") {
        return {
          tag: "submitting",
          fileName: state.inputs.fileName,
          fileExtension: state.inputs.fileExtension,
          encryption: state.inputs.encryption,
          file: state.inputs.file,
          parentFolderId: state.inputs.parentFolderId,
          parentFolderType: state.inputs.parentFolderType,
          folders: state.folders,
        };
      } else {
        logUnexpectedAction(state, action);
        return state;
      }

    case "submitting":
      if (action.type === "success") {
        return { tag: "redirect" };
      } else if (action.type === "error") {
        return {
          tag: "editing",
          error: action.message,
          inputs: {
            fileName: "",
            fileExtension: "",
            encryption: false,
            file: null,
            parentFolderId: undefined,
            parentFolderType: undefined,
          },
          folders: state.folders,
        };
      } else {
        logUnexpectedAction(state, action);
        return state;
      }

    case "redirect":
      logUnexpectedAction(state, action);
      return state;
  }
}

const firstState: State = {
  tag: "begin",
};

const sortBy = "created_desc";

const CreateFile = () => {
  const [state, dispatch] = useReducer(reducer, firstState);
  const { token, keyMaster } = useAuthentication();

  useEffect(() => {
    if (state.tag === "begin") {
      dispatch({ type: "start-loading" });
      handleGetFolder();
    }

    if (state.tag === "redirect") {
      router.replace("/files");
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

  async function handleGetFolder() {
    try {
      const folders = await getFolders(token, sortBy);
      dispatch({ type: "loading-success", folders });
    } catch (error) {
      Alert.alert(
        "Error",
        `${isProblem(error) ? getProblemMessage(error) : error}`
      );
      dispatch({ type: "loading-error", error: error });
    }
  }

  // Handle file picker
  async function openPicker() {
    const result = await DocumentPicker.getDocumentAsync({
      type: ["*/*"],
      copyToCacheDirectory: true,
    });

    const fileName =
      state.tag === "submitting" && state.fileName
        ? state.fileName
        : state.inputs?.fileName || "";

    if (!result.canceled && result.assets && result.assets.length > 0) {
      const file = result.assets[0];

      const extension = file.name.split(".").pop();

      const fileExtension = `${fileName}.${extension}`;

      dispatch({
        type: "edit",
        inputName: "file",
        inputValue: {
          uri: file.uri,
          name: fileName,
          type: file.mimeType,
        },
      });

      dispatch({
        type: "edit",
        inputName: "fileExtension",
        inputValue: fileExtension,
      });
    } else {
      Alert.alert("No file selected", "Please select a file to upload.");
    }
  }

  // Handle form submission
  async function handleSubmit() {
    if (state.tag !== "editing") {
      return;
    }
    dispatch({ type: "submit" });

    const fileName = state.inputs.fileName;
    const fileExtension = state.inputs.fileExtension;
    const encryption = state.inputs.encryption;
    const file = state.inputs.file;
    const parentFolderId = state.inputs.parentFolderId;

    if (!fileName?.trim() || !file) {
      Alert.alert("Error", "Please fill in all fields");
      dispatch({ type: "error", message: "Please fill in all field" });
      return;
    }

    try {
      if (parentFolderId) {
        await uploadFile(
          file,
          fileExtension,
          encryption,
          keyMaster,
          token,
          parentFolderId.toString()
        );
      } else {
        await uploadFile(file, fileExtension, encryption, keyMaster, token);
      }

      Alert.alert("Success", "Your file has been uploaded successfully.");
      dispatch({ type: "success" });
    } catch (error) {
      Alert.alert(
        "Error",
        `${isProblem(error) ? getProblemMessage(error) : error}`
      );
      dispatch({ type: "error", message: error });
    }
  }

  const fileName =
    state.tag === "submitting" && state.fileName
      ? state.fileName
      : state.inputs?.fileName || "";

  const encryption =
    state.tag === "submitting" && state.encryption
      ? state.encryption
      : state.inputs?.encryption || false;

  const file =
    state.tag === "submitting" && state.file
      ? state.file
      : state.inputs?.file || null;

  // Render UI
  switch (state.tag) {
    case "begin":
      return (
        <SafeAreaView className="bg-primary flex-1 justify-center items-center">
          <ActivityIndicator size="large" color="#fff" />
        </SafeAreaView>
      );
    case "submitting":
      return (
        <SafeAreaView className="bg-primary flex-1 justify-center items-center">
          <ActivityIndicator size="large" color="#fff" />
          <Text className="mt-4 text-white text-lg font-semibold">
            Upload File...
          </Text>
        </SafeAreaView>
      );
    case "error":
      return (
        <SafeAreaView className="bg-primary flex-1">
          <ActivityIndicator />
          <Text className="text-[24px] font-semibold text-white text-center mb-16 mt-4">
            {state.tag === "error" &&
              (typeof state.error === "string"
                ? state.error
                : state.error?.detail)}
          </Text>
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
    case "editing":
      return (
        <SafeAreaView className="bg-primary h-full">
          <ScrollView className="px-4 my-6 mt-12">
            <View className="flex-row items-center mb-8">
              <TouchableOpacity onPress={() => router.back()} hitSlop={12}>
                <Image
                  source={icons.back}
                  className="w-6 h-6"
                  resizeMode="contain"
                  tintColor="white"
                />
              </TouchableOpacity>
              <Text className="text-2xl text-white font-psemibold ml-32">
                Upload Flie
              </Text>
            </View>

            <FormField
              title="File Name"
              value={fileName}
              placeholder="Enter a title for your file..."
              handleChangeText={(text) => handleChange("fileName", text)}
              otherStyles="mt-7"
            />

            <View
              style={{ height: 0.5, backgroundColor: "#F8F8F8", marginTop: 20 }}
            />

            {(state.inputs.parentFolderId === undefined ||
              state.inputs.parentFolderType === FolderType.PRIVATE) && (
              <>
                <EncryptionToggle
                  value={encryption}
                  onChange={(value) => handleChange("encryption", value)}
                  otherStyles=" mt-7"
                />
                <View
                  style={{
                    height: 0.5,
                    backgroundColor: "#F8F8F8",
                    marginTop: 20,
                  }}
                />
              </>
            )}

            <View className="mt-7 space-y-2">
              <Text className="text-base text-gray-100 font-pmedium">
                Upload Video
              </Text>

              <TouchableOpacity onPress={openPicker} className="mt-3">
                <View className="w-full h-40 px-4 bg-black-100 rounded-2xl border border-black-200 flex justify-center items-center">
                  <View className="w-14 h-14 border border-dashed border-secondary-100 flex justify-center items-center">
                    {file === null ? (
                      <Image
                        source={icons.upload}
                        resizeMode="contain"
                        alt="upload"
                        className="w-1/2 h-1/2"
                      />
                    ) : (
                      <Image
                        source={icons.uploadFile}
                        resizeMode="contain"
                        alt="upload"
                        className="w-1/2 h-1/2"
                      />
                    )}
                  </View>
                </View>
              </TouchableOpacity>
            </View>

            <View className="mt-2">
              <View className="flex-row items-center mb-2">
                <Text className="text-xl text-white font-semibold">
                  Recent Folders
                </Text>
                <TouchableOpacity
                  onPress={() => {
                    handleChange("parentFolderId", undefined);
                    handleChange("parentFolderType", undefined);
                  }}
                  className="m-4"
                >
                  <Image
                    source={icons.reset}
                    className="w-6 h-6"
                    resizeMode="contain"
                    tintColor="white"
                  />
                </TouchableOpacity>
              </View>

              {state.tag === "editing" &&
                state.folders.content.map((folder) => (
                  <FolderItemComponent
                    key={folder.folderId}
                    item={folder}
                    onPress={(folderId) => {
                      handleChange("parentFolderId", folderId);
                      handleChange("parentFolderType", folder.type);
                      if (folder.type === FolderType.SHARED) {
                        handleChange("encryption", false);
                      }
                    }}
                    selectedFolderId={state.inputs.parentFolderId}
                  />
                ))}
            </View>
            <CustomButton
              title="Submit & Publish"
              handlePress={handleSubmit}
              containerStyles="mt-7"
              isLoading={false}
              textStyles={""}
              color="bg-secondary"
            />
          </ScrollView>
        </SafeAreaView>
      );
  }
};

export default CreateFile;
