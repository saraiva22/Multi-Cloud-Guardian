import BottomSheet, { BottomSheetView } from "@gorhom/bottom-sheet";
import {
  View,
  Text,
  TouchableOpacity,
  FlatList,
  StyleSheet,
  Alert,
  SafeAreaView,
  ActivityIndicator,
  Image,
} from "react-native";
import { forwardRef, useEffect, act, useReducer } from "react";
import { Folder } from "@/domain/storage/Folder";
import SearchInput from "./SearchBar";
import { File } from "@/domain/storage/File";
import { PageResult } from "@/domain/utils/PageResult";
import {
  getProblemMessage,
  isProblem,
  Problem,
} from "@/services/media/Problem";
import CustomButton from "./CustomButton";
import { Picker } from "@react-native-picker/picker";
import {
  downloadFile,
  downloadFileInFolder,
  generateTemporaryUrl,
  getFolders,
  moveFile,
  processAndSaveDownloadedFile,
} from "@/services/storage/StorageService";
import * as Clipboard from "expo-clipboard";
import { FolderType } from "@/domain/storage/FolderType";
import { OwnershipFilter } from "@/domain/storage/OwnershipFilter";
import { useAuthentication } from "@/context/AuthProvider";
import { MaterialCommunityIcons } from "@expo/vector-icons";
import { icons } from "@/constants";
import { router } from "expo-router";

// The State
type State =
  | { tag: "begin" }
  | { tag: "loading"; search: string }
  | { tag: "url-editing"; inputs: { minutes: number } }
  | { tag: "url-loaded"; url: string }
  | {
      tag: "move-editing";
      inputs: { folderName: string; selectFolderId: number | null };
      fileDetails: File;
      folders: PageResult<Folder>;
    }
  | { tag: "redirect"; replaceHistory: boolean }
  | { tag: "error"; error: Problem };

// The Action
type Action =
  | { type: "start-loading" }
  | { type: "loading-error"; error: Problem }
  | { type: "loading-success"; replaceHistory: boolean }
  | { type: "url-start" }
  | { type: "edit-url"; minutes: number }
  | { type: "url-success"; url: string }
  | { type: "loading-move-success"; file: File; folders: PageResult<Folder> }
  | { type: "submit-move"; fileId: number; folderId: number | null }
  | { type: "update-folders"; search: string }
  | { type: "edit"; inputName: string; inputValue: string | number | null }
  | { type: "reset" };

// The Logger
function logUnexpectedAction(state: State, action: Action) {
  console.log(`Unexpected action '${action.type} on state '${state.tag}'`);
}

// The Reducer
function reducer(state: State, action: Action): State {
  switch (state.tag) {
    case "begin":
      if (action.type === "start-loading")
        return { tag: "loading", search: "" };
      else if (action.type === "url-start")
        return { tag: "url-editing", inputs: { minutes: 15 } };
      else if (action.type === "reset") return { tag: "begin" };
      else {
        logUnexpectedAction(state, action);
        return state;
      }
    case "loading":
      if (action.type === "loading-success") {
        return { tag: "redirect", replaceHistory: action.replaceHistory };
      } else if (action.type === "loading-error") {
        return { tag: "error", error: action.error };
      } else if (action.type === "url-success") {
        return { tag: "url-loaded", url: action.url };
      } else if (action.type === "loading-move-success") {
        return {
          tag: "move-editing",
          inputs: { folderName: "", selectFolderId: null },
          fileDetails: action.file,
          folders: action.folders,
        };
      } else {
        logUnexpectedAction(state, action);
        return state;
      }
    case "url-editing":
      if (action.type === "reset") {
        return { tag: "begin" };
      } else if (action.type === "edit-url") {
        return { ...state, inputs: { minutes: action.minutes } };
      } else if (action.type === "start-loading")
        return { tag: "loading", search: "" };
      else {
        logUnexpectedAction(state, action);
        return state;
      }
    case "move-editing":
      if (action.type === "reset") return { tag: "begin" };
      else if (action.type === "edit") {
        return {
          ...state,
          tag: "move-editing",
          inputs: {
            ...state.inputs,
            [action.inputName]: action.inputValue,
          },
        };
      } else if (action.type === "update-folders") {
        return { tag: "loading", search: action.search };
      } else if (action.type === "submit-move") {
        return { tag: "loading", search: "" };
      } else {
        logUnexpectedAction(state, action);
        return state;
      }

    case "redirect":
      if (action.type === "reset") {
        return { tag: "begin" };
      } else {
        logUnexpectedAction(state, action);
        return state;
      }

    case "url-loaded":
      if (action.type === "reset") {
        return { tag: "begin" };
      } else {
        logUnexpectedAction(state, action);
        return state;
      }

    case "error":
      if (action.type === "reset") {
        return { tag: "begin" };
      } else {
        return state;
      }
  }
}

type Props = { file: File };

const firstState: State = { tag: "begin" };

const FileActionsBottomSheet = forwardRef<BottomSheet, Props>(
  ({ file }: Props, ref) => {
    const selectFile = file;
    const [state, dispatch] = useReducer(reducer, firstState);

    const { token, username, keyMaster } = useAuthentication();

    const search =
      state.tag === "loading" && state.search
        ? state.search
        : state?.search || "";

    const folderId =
      state.tag === "move-editing" && state.inputs.selectFolderId
        ? state.inputs.selectFolderId
        : state?.inputs?.selectFolderId || null;

    const folderName =
      state.tag === "move-editing" && state.inputs.folderName
        ? state.inputs.folderName
        : state?.inputs?.folderName || null;

    const handleAction = () => {
      // Small delay to allow the bottom sheet to close smoothly before navigation
      setTimeout(() => {
        dispatch({ type: "reset" });
        if (ref && typeof ref !== "function" && ref.current) {
          ref.current.close();
        }
      }, 200);
    };

    useEffect(() => {
      if (state.tag === "redirect" || state.tag === "error") {
        handleAction();
      }

      if (state.tag === "redirect" && state.replaceHistory) {
        router.replace("/files");
      }
    }, [state]);

    async function handleGenerateTemporaryUrl() {
      if (state.tag !== "url-editing") return;
      const minutes = state.inputs.minutes;
      dispatch({ type: "start-loading" });
      try {
        const value = await generateTemporaryUrl(
          selectFile.fileId.toString(),
          minutes,
          token
        );
        const url = value.url ? value.url : "";
        dispatch({ type: "url-success", url: url });
      } catch (error) {
        dispatch({ type: "loading-error", error: error });
      }
    }

    // Handle input changes
    function handleChange(
      inputName: string,
      inputValue: string | number | null
    ) {
      dispatch({
        type: "edit",
        inputName,
        inputValue,
      });
    }

    //Handle Download
    async function handleDownload() {
      if (state.tag !== "begin") return;
      dispatch({ type: "start-loading" });
      try {
        const fileId = file.fileId.toString();
        const result = file.folderInfo?.folderId
          ? await downloadFileInFolder(
              file.folderInfo.folderId.toString(),
              fileId,
              token
            )
          : await downloadFile(fileId, token);

        await processAndSaveDownloadedFile(result, keyMaster);
        dispatch({ type: "loading-success", replaceHistory: false });
      } catch (error) {
        Alert.alert(
          "Error",
          `${isProblem(error) ? getProblemMessage(error) : error}`
        );
        dispatch({ type: "loading-error", error: error });
      }
    }

    // Handle GetFolders
    async function handleGetFolders() {
      dispatch({ type: "start-loading" });
      try {
        const folders = await getFolders(
          token,
          undefined,
          FolderType.PRIVATE,
          OwnershipFilter.OWNER,
          search
        );
        dispatch({
          type: "loading-move-success",
          file: selectFile,
          folders: folders,
        });
      } catch (error) {
        Alert.alert(
          "Error",
          `${isProblem(error) ? getProblemMessage(error) : error}`
        );
        dispatch({ type: "loading-error", error: error });
      }
    }

    // Handle MoveFile
    async function handleMoveFile() {
      try {
        const selectFolderId = folderId;
        const fileId = selectFile.fileId.toString();
        dispatch({
          type: "submit-move",
          fileId: Number(fileId),
          folderId: folderId,
        });

        await moveFile(fileId, selectFolderId, token);

        dispatch({ type: "loading-success", replaceHistory: true });
      } catch (error) {
        Alert.alert(
          "Error",
          `${isProblem(error) ? getProblemMessage(error) : error}`
        );
        dispatch({ type: "loading-error", error: error });
      }
    }

    // Render UI
    return (
      <BottomSheet
        ref={ref}
        index={-1}
        snapPoints={["80%"]}
        enablePanDownToClose={true}
        backgroundStyle={styles.sheetBackground}
        handleIndicatorStyle={styles.handleIndicator}
        onClose={() => dispatch({ type: "reset" })}
      >
        <BottomSheetView style={styles.contentContainer}>
          <View style={{ flex: 1, width: "100%" }}>
            {(() => {
              switch (state.tag) {
                case "begin":
                  return (
                    <View style={{ gap: 12 }}>
                      {file?.encryption === false &&
                        (file?.folderInfo === null ||
                          file?.folderInfo?.folderType ===
                            FolderType.PRIVATE) && (
                          <CustomButton
                            title="Generate Temporary URL"
                            handlePress={() => dispatch({ type: "url-start" })}
                            containerStyles="mt-10 rounded-lg"
                            isLoading={false}
                            textStyles="text-base font-semibold"
                            color="bg-secondary"
                          />
                        )}
                      <CustomButton
                        title="Download File"
                        handlePress={handleDownload}
                        containerStyles="mt-10 rounded-lg"
                        isLoading={false}
                        textStyles="text-base font-semibold"
                        color="bg-secondary"
                      />
                      {(file?.folderInfo === null ||
                        file?.folderInfo?.folderType ===
                          FolderType.PRIVATE) && (
                        <CustomButton
                          title="Move FIle"
                          handlePress={handleGetFolders}
                          containerStyles="mt-10 rounded-lg"
                          isLoading={false}
                          textStyles="text-base font-semibold"
                          color="bg-secondary"
                        />
                      )}
                    </View>
                  );

                case "loading":
                  return (
                    <SafeAreaView className="flex-1 justify-center items-center">
                      <ActivityIndicator size="small" color="#fff" />
                      <Text className="mt-4 text-white text-lg font-semibold">
                        Loading...
                      </Text>
                    </SafeAreaView>
                  );

                case "url-editing":
                  return (
                    <>
                      <TouchableOpacity
                        onPress={() => dispatch({ type: "reset" })}
                        hitSlop={12}
                      >
                        <Image
                          source={icons.back}
                          className="w-6 h-6"
                          resizeMode="contain"
                          tintColor="white"
                        />
                      </TouchableOpacity>
                      <View className="my-6 px-5 space-y-6">
                        <Text style={styles.label}>
                          URL Expiration (minutes):
                        </Text>
                        <View
                          style={{
                            backgroundColor: "#FAF9F6",
                            borderRadius: 8,
                          }}
                        >
                          <Picker
                            selectedValue={state.inputs.minutes}
                            onValueChange={(value) =>
                              dispatch({ type: "edit-url", minutes: value })
                            }
                            dropdownIconColor="#FFA001"
                            style={{ color: "#222" }}
                          >
                            {Array.from({ length: 60 }, (_, i) => i + 1).map(
                              (minute) => (
                                <Picker.Item
                                  key={minute}
                                  label={`${minute} minuto${
                                    minute > 1 ? "s" : ""
                                  }`}
                                  value={minute}
                                  style={{
                                    color: "#222",
                                    backgroundColor: "#FAF9F6",
                                  }}
                                />
                              )
                            )}
                          </Picker>
                        </View>

                        <CustomButton
                          title="Generate Temporary URL"
                          handlePress={handleGenerateTemporaryUrl}
                          containerStyles="mt-7"
                          isLoading={false}
                          textStyles={""}
                          color="bg-secondary"
                        />
                      </View>
                    </>
                  );

                case "url-loaded":
                  return (
                    <>
                      <View className="w-full mb-4 rounded-lg py-4">
                        <Text className="text-white text-center text-xl mb-2 font-medium">
                          Temporary access link generated
                        </Text>
                        <TouchableOpacity
                          onPress={async () => {
                            await Clipboard.setStringAsync(`${state.url}`);
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
                    </>
                  );

                case "move-editing":
                  return (
                    <>
                      <TouchableOpacity
                        onPress={() => dispatch({ type: "reset" })}
                        hitSlop={12}
                      >
                        <Image
                          source={icons.back}
                          className="w-6 h-6"
                          resizeMode="contain"
                          tintColor="white"
                        />
                      </TouchableOpacity>
                      <View className="flex-col justify-items-center m-4">
                        <SearchInput
                          placeholder="Folder"
                          value={search}
                          onChangeText={(text) =>
                            handleChange("folderName", text)
                          }
                        />

                        <View className="flex-row items-center mt-2">
                          <Text className="text-xl text-white font-semibold">
                            Recent Folders
                          </Text>
                          <TouchableOpacity
                            onPress={() => handleChange("selectFolderId", null)}
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

                        <FlatList
                          className="mt-3"
                          data={state.folders.content.filter(
                            (folder) =>
                              folder.folderId !== file.folderInfo?.folderId
                          )}
                          style={{ maxHeight: 225 }}
                          keyExtractor={(item) => item.folderId.toString()}
                          renderItem={({ item }) => {
                            const isSelected =
                              state.inputs.selectFolderId === item.folderId;
                            return (
                              <TouchableOpacity
                                onPress={() =>
                                  handleChange("selectFolderId", item.folderId)
                                }
                                className={`flex-row items-center p-2 my-1 rounded-lg ${
                                  isSelected ? "bg-gray-700" : "bg-gray-800"
                                }`}
                              >
                                <MaterialCommunityIcons
                                  name={
                                    isSelected
                                      ? "check-circle"
                                      : "checkbox-blank-circle-outline"
                                  }
                                  size={24}
                                  color={isSelected ? "#FFA001" : "#888"}
                                  style={{ marginRight: 10 }}
                                />
                                <Text className="text-white">
                                  {item.folderName}
                                </Text>
                              </TouchableOpacity>
                            );
                          }}
                        />

                        {file.folderInfo !== null &&
                          state.inputs.selectFolderId == null && (
                            <View className="mt-4 px-4 py-3 bg-yellow-100 rounded-lg flex-row items-center">
                              <Text className="flex-1 text-yellow-800">
                                No folder selected. The file will be moved to
                                the root if you proceed
                              </Text>
                              <TouchableOpacity
                                className="ml-3 px-4 py-2 bg-secondary rounded"
                                onPress={() => {
                                  Alert.alert(
                                    "No Folder Selected",
                                    "You have not selected any folder. If you continue, the file will be moved to the root of your storage. Do you want to proceed?",
                                    [
                                      { text: "Cancel", style: "cancel" },
                                      {
                                        text: "Proceed",
                                        style: "destructive",
                                        onPress: handleMoveFile,
                                      },
                                    ]
                                  );
                                }}
                              >
                                <Text className="text-white font-semibold">
                                  Move File
                                </Text>
                              </TouchableOpacity>
                            </View>
                          )}

                        {file.folderInfo === null &&
                          state.inputs.selectFolderId == null && (
                            <View className="mt-4 px-4 py-3 bg-yellow-100 rounded-lg flex-row items-center">
                              <Text className="flex-1 text-yellow-800">
                                The file is already located in the root
                                directory. To move it, please choose a target
                                folder.
                              </Text>
                            </View>
                          )}

                        {state.inputs.selectFolderId !== null && (
                          <CustomButton
                            title="Move FIle"
                            handlePress={handleMoveFile}
                            containerStyles="mt-10 rounded-lg"
                            isLoading={false}
                            textStyles="text-base font-semibold"
                            color="bg-secondary"
                          />
                        )}
                      </View>
                    </>
                  );

                case "error":
                  return (
                    <SafeAreaView className="flex-1">
                      <ActivityIndicator />
                      <Text className="text-[24px] font-semibold text-white text-center mb-16 mt-4">
                        {state.tag === "error" &&
                          (typeof state.error === "string"
                            ? state.error
                            : state.error?.detail)}
                      </Text>
                    </SafeAreaView>
                  );

                case "redirect":
                  <SafeAreaView className="flex-1 justify-center items-center">
                    <ActivityIndicator size="small" color="#fff" />
                    <Text className="mt-4 text-white text-lg font-semibold">
                      Redirect...
                    </Text>
                  </SafeAreaView>;

                default:
                  return null;
              }
            })()}
          </View>
        </BottomSheetView>
      </BottomSheet>
    );
  }
);

export default FileActionsBottomSheet;

const styles = StyleSheet.create({
  sheetBackground: {
    backgroundColor: "#232533",
  },
  handleIndicator: {
    backgroundColor: "#FFA001",
    width: 40,
    height: 5,
    borderRadius: 5,
  },
  contentContainer: {
    flex: 1,
    alignItems: "center",
    justifyContent: "flex-start",
    padding: 16,
  },
  moveButton: {
    backgroundColor: "#007AFF",
    padding: 16,
    borderRadius: 8,
    alignItems: "center",
    position: "absolute",
    bottom: 20,
    left: 16,
    right: 16,
  },
  buttonText: {
    color: "#fff",
    fontWeight: "bold",
  },
  label: {
    color: "#FFA001",
    fontSize: 16,
    fontWeight: "600",
    marginBottom: 8,
  },
  loadingText: {
    color: "#ccc",
    fontSize: 18,
  },
  actionButton: {
    backgroundColor: "#444",
    padding: 14,
    borderRadius: 10,
    alignItems: "center",
    marginVertical: 6,
  },
});
