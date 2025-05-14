import {
  View,
  Text,
  ScrollView,
  Image,
  TouchableOpacity,
  Alert,
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
import { uploadFile } from "@/services/storage/StorageService";

type State =
  | {
      tag: "editing";
      error?: Problem | string;
      inputs: {
        fileName: string;
        encryption: boolean;
        file: any;
      };
    }
  | {
      tag: "submitting";
      fileName: string;
      encryption: boolean;
      file: any;
    }
  | { tag: "redirect" };

type Action =
  | { type: "edit"; inputName: string; inputValue: string | boolean | any }
  | { type: "submit" }
  | { type: "error"; message: Problem | string }
  | { type: "success" };

function logUnexpectedAction(state: State, action: Action) {
  console.log(`Unexpected action '${action.type} on state '${state.tag}'`);
}

function reduce(state: State, action: Action): State {
  switch (state.tag) {
    case "editing":
      if (action.type === "edit") {
        return {
          tag: "editing",
          error: undefined,
          inputs: { ...state.inputs, [action.inputName]: action.inputValue },
        };
      } else if (action.type === "submit") {
        return {
          tag: "submitting",
          fileName: state.inputs.fileName,
          encryption: state.inputs.encryption,
          file: state.inputs.file,
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
          inputs: { fileName: state.fileName, encryption: false, file: null },
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
  tag: "editing",
  inputs: { fileName: "", encryption: false, file: null },
};

const CreateFile = () => {
  const [state, dispatch] = useReducer(reduce, firstState);
  const { username, keyMaster } = useAuthentication();

  useEffect(() => {
    if (state.tag === "redirect") {
      router.replace("/files");
    }
  });

  function handleChange(inputName: string, inputValue: string | boolean | any) {
    dispatch({
      type: "edit",
      inputName,
      inputValue,
    });
  }

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

      const fileExtension = file.name.split(".").pop();
      const nameFile = `${fileName}.${fileExtension}`;

      dispatch({
        type: "edit",
        inputName: "file",
        inputValue: {
          uri: file.uri,
          name: nameFile,
          type: file.mimeType,
        },
      });

      dispatch({
        type: "edit",
        inputName: "fileName",
        inputValue: nameFile,
      });
    } else {
      Alert.alert("No file selected", "Please select a file to upload.");
    }
  }

  async function handleSubmit() {
    if (state.tag !== "editing") {
      return;
    }
    dispatch({ type: "submit" });

    const fileName = state.inputs.fileName;
    const encryption = state.inputs.encryption;
    const file = state.inputs.file;

    if (!fileName?.trim() || !file) {
      Alert.alert("Error", "Please fill in all fields");
      dispatch({ type: "error", message: "Invalid username or password" });
      return;
    }

    try {
      await uploadFile(file, fileName, encryption, keyMaster);

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

  return (
    <SafeAreaView className="bg-primary h-full">
      <ScrollView className="px-4 my-6 mt-12">
        <Text className="text-2xl text-white font-psemibold">Upload Flie</Text>

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

        <EncryptionToggle
          value={encryption}
          onChange={(value) => handleChange("encryption", value)}
          otherStyles="mt-7"
        />

        <View
          style={{ height: 0.5, backgroundColor: "#F8F8F8", marginTop: 20 }}
        />
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

        <CustomButton
          title="Submit & Publish"
          handlePress={handleSubmit}
          containerStyles="mt-7"
          isLoading={state.tag === "submitting"}
          textStyles={""}
          color="bg-secondary"
        />
      </ScrollView>
    </SafeAreaView>
  );
};

export default CreateFile;
