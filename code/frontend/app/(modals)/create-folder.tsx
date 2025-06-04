import {
  View,
  Text,
  Alert,
  SafeAreaView,
  ScrollView,
  TouchableOpacity,
  Image,
} from "react-native";
import React, { useEffect, useReducer } from "react";
import {
  getProblemMessage,
  isProblem,
  Problem,
} from "@/services/media/Problem";
import { router } from "expo-router";
import { createFolder } from "@/services/storage/StorageService";
import FormField from "@/components/FormField";
import { icons } from "@/constants";
import CustomButton from "@/components/CustomButton";

// The State
type State =
  | {
      tag: "editing";
      error?: Problem | string;
      inputs: {
        folderName: string;
      };
    }
  | { tag: "submitting"; folderName: string }
  | { tag: "redirect" };

// The Action
type Action =
  | { type: "edit"; inputName: string; inputValue: string }
  | { type: "submit" }
  | { type: "error"; message: Problem | string }
  | { type: "success" };

function logUnexpectedAction(state: State, action: Action) {
  console.log(`Unexpected action '${action.type} on state '${state.tag}'`);
}

function reducer(state: State, action: Action): State {
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
          folderName: state.inputs.folderName,
        };
      } else {
        logUnexpectedAction(state, action);
        return state;
      }

    case "submitting":
      if (action.type === "success") {
        return {
          tag: "redirect",
        };
      } else if (action.type === "error") {
        return {
          tag: "editing",
          error: action.message,
          inputs: { folderName: "" },
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
  inputs: { folderName: "" },
};

const CreateFolder = () => {
  const [state, dispatch] = useReducer(reducer, firstState);

  useEffect(() => {
    if (state.tag === "redirect") {
      router.replace("/folders");
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

  // Handle form submission
  async function handleSubmit() {
    if (state.tag !== "editing") return;

    dispatch({ type: "submit" });

    const folderName = state.inputs.folderName;

    if (!folderName?.trim()) {
      Alert.alert("Error", "Please fill in all fields");
      dispatch({ type: "error", message: "Please fill in all field" });
      return;
    }

    try {
      await createFolder(folderName);

      dispatch({ type: "success" });
    } catch (error) {
      Alert.alert(
        "Error",
        `${isProblem(error) ? getProblemMessage(error) : error}`
      );
      dispatch({ type: "error", message: error });
    }
  }

  const folderName =
    state.tag === "submitting" && state.folderName
      ? state.folderName
      : state.inputs?.folderName || "";

  return (
    <SafeAreaView className="bg-primary h-full">
      <ScrollView className="px-4 my-6 mt-12">
        <View className="flex-row items-center mb-6 mt-2">
          <TouchableOpacity onPress={() => router.back()} hitSlop={12}>
            <Image
              source={icons.back}
              className="w-6 h-6"
              resizeMode="contain"
              tintColor="white"
            />
          </TouchableOpacity>
          <Text className="text-2xl text-white font-psemibold ml-32">
            Create Folder
          </Text>
        </View>

        <FormField
          title="Folder Name"
          value={folderName}
          placeholder="Enter a title for your folder..."
          handleChangeText={(text) => handleChange("folderName", text)}
          otherStyles="mt-7"
        />

        <View
          style={{ height: 0.5, backgroundColor: "#F8F8F8", marginTop: 20 }}
        />

        <View
          style={{ height: 0.5, backgroundColor: "#F8F8F8", marginTop: 20 }}
        />

        <CustomButton
          title="Create Folder"
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

export default CreateFolder;
