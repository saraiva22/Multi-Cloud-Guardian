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
import React, { useEffect, useReducer, useRef, useState } from "react";
import { router, useLocalSearchParams, useRouter } from "expo-router";
import {
  deleteFolder,
  getFilesInFolder,
  getFolder,
  getFoldersInFolder,
  leaveFolder,
} from "@/services/storage/StorageService";
import { FolderOutputModel } from "@/services/storage/model/FolderOutputModel";
import {
  getProblemMessage,
  isProblem,
  Problem,
} from "@/services/media/Problem";
import CustomButton from "@/components/CustomButton";
import icons from "@/constants/icons";
import {
  formatDate,
  formatFolderType,
  formatSize,
} from "@/services/utils/Function";
import { removeValueFor } from "@/services/storage/SecureStorage";
import { KEY_NAME, useAuthentication } from "@/context/AuthProvider";
import { File } from "@/domain/storage/File";
import { PageResult } from "@/domain/utils/PageResult";
import { Folder } from "@/domain/storage/Folder";
import FileItemComponent from "@/components/FileItemComponent";
import FolderCard from "@/components/FolderCard";
import EmptyState from "@/components/EmptyState";
import { FolderType } from "@/domain/storage/FolderType";
import { getSSE } from "@/services/notifications/SSEManager";
import { EventSourceListener } from "react-native-sse";
import { UserInfo } from "@/domain/user/UserInfo";
import MemberCard from "@/components/MemberCard";
import BottomSheet from "@gorhom/bottom-sheet";
import { GestureHandlerRootView } from "react-native-gesture-handler";
import FileActionsBottomSheet from "@/components/FileActionsBottomSheet";

// The State
type State =
  | { tag: "begin" }
  | { tag: "loading" }
  | {
      tag: "loaded";
      details: FolderOutputModel;
      files: PageResult<File>;
      folders: PageResult<Folder> | null;
      isFetchingMore: boolean;
      selectFile: File | null;
    }
  | { tag: "redirect" }
  | { tag: "error"; error: Problem | string };

// The Action
type Action =
  | { type: "start-loading" }
  | {
      type: "loading-success";
      details: FolderOutputModel;
      files: PageResult<File>;
      folders: PageResult<Folder> | null;
    }
  | { type: "loading-error"; error: Problem | string }
  | { type: "delete-loading" }
  | { type: "leave-loading" }
  | { type: "success-delete" }
  | { type: "success-leave" }
  | { type: "fetch-more-start" }
  | { type: "select-file"; file: File }
  | { type: "fetch-more-folders-success"; folders: PageResult<Folder> }
  | { type: "fetch-more-files-success"; files: PageResult<File> }
  | { type: "new-file"; file: File }
  | { type: "new-member"; member: UserInfo }
  | { type: "leave-user"; member: UserInfo }
  | {
      type: "delete-file";
      fileId: number;
      user: UserInfo;
      folderId: number;
      updatedAt: number;
    }
  | { type: "delete-folder" }
  | { type: "delete-select-file"; file: File };

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
          isFetchingMore: false,
          selectFile: null,
        };
      } else if (action.type === "loading-error") {
        return { tag: "error", error: action.error };
      } else if (action.type === "success-delete") {
        return { tag: "redirect" };
      } else if (action.type === "success-leave") {
        return { tag: "redirect" };
      } else {
        logUnexpectedAction(state, action);
        return state;
      }

    case "loaded":
      if (action.type === "delete-loading") {
        return { tag: "loading" };
      } else if (action.type === "leave-loading") {
        return {
          tag: "loading",
        };
      } else if (action.type === "new-file") {
        return {
          tag: "loaded",
          details: {
            ...state.details,
            numberFile: state.details.numberFile + 1,
            createdAt: action.file.createdAt,
            size: state.details.size + action.file.size,
          },
          files: {
            ...state.files,
            content: [...state.files.content, action.file],
            totalElements: state.files.totalElements + 1,
          },
          folders: state.folders,
          isFetchingMore: state.isFetchingMore,
          selectFile: null,
        };
      } else if (action.type === "new-member") {
        return {
          tag: "loaded",
          details: {
            ...state.details,
            members: [...state.details.members, action.member],
          },
          files: state.files,
          folders: state.folders,
          isFetchingMore: state.isFetchingMore,
          selectFile: null,
        };
      } else if (action.type === "select-file") {
        return { ...state, selectFile: action.file };
      } else if (
        action.type === "delete-file" ||
        action.type === "delete-select-file"
      ) {
        const fileId =
          action.type === "delete-file" ? action.fileId : action.file.fileId;
        const updatedAt =
          action.type === "delete-file" && action.updatedAt
            ? action.updatedAt
            : Date.now();

        return {
          tag: "loaded",
          details: {
            ...state.details,
            numberFile: state.details.numberFile - 1,
            createdAt: updatedAt,
          },
          files: {
            ...state.files,
            content: state.files.content.filter(
              (file) => file.fileId !== fileId
            ),
            totalElements: state.files.totalElements - 1,
          },
          folders: state.folders,
          isFetchingMore: state.isFetchingMore,
          selectFile: null,
        };
      } else if (action.type === "leave-user") {
        const numberFile = state.files.content.filter(
          (file) => file.user.id === action.member.id
        ).length;
        return {
          ...state,
          tag: "loaded",
          details: {
            ...state.details,
            numberFile: state.details.numberFile - numberFile,
            members: state.details.members.filter(
              (value) => value.id !== action.member.id
            ),
          },
          files: {
            ...state.files,
            content: state.files.content.filter(
              (file) => file.user.id !== action.member.id
            ),
          },
        };
      } else if (action.type === "delete-folder") {
        return { tag: "redirect" };
      } else if (action.type === "fetch-more-start") {
        return {
          ...state,
          isFetchingMore: true,
        };
      } else if (
        action.type === "fetch-more-folders-success" &&
        state.details.type === FolderType.PRIVATE
      ) {
        return {
          ...state,
          folders: state.folders
            ? {
                ...action.folders,
                content: [...state.folders.content, ...action.folders.content],
              }
            : action.folders,
          isFetchingMore: false,
        };
      } else if (action.type === "fetch-more-files-success") {
        const existingIds = new Set(
          state.files.content.map((file) => file.fileId)
        );
        const newFiles = action.files.content.filter(
          (file) => !existingIds.has(file.fileId)
        );

        return {
          ...state,
          files: {
            ...action.files,
            content: [...state.files.content, ...newFiles],
          },
          isFetchingMore: false,
        };
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
type CustomEventsFile = "file";
type CustomEventsMember = "newMember";
type CustomEventLeaveFolder = "leaveFolder";
type CustomEventDeleteFile = "deleteFile";
type CustomEventDeleteFolder = "deleteFolder";

const sortBy = "updated_desc";

type FolderInfoDetailsProps = {
  folderDetails: FolderOutputModel;
  members: Array<UserInfo>;
  username: string | undefined;
  handleDelete: () => Promise<any>;
  handleLeave: () => Promise<any>;
  handleUploadFile: () => Promise<any>;
};

const FolderInfoDetails = ({
  folderDetails: folderDetails,
  members: members,
  username,
  handleDelete,
  handleLeave,
  handleUploadFile,
}: FolderInfoDetailsProps) => (
  <>
    <View className="flex-row items-center justify-between px-4 mt-12 mb-10">
      <TouchableOpacity onPress={() => router.back()} hitSlop={12}>
        <Image
          source={icons.back}
          className="w-6 h-6"
          resizeMode="contain"
          tintColor="white"
        />
      </TouchableOpacity>

      <Text className="text-[24px] ml-14 font-semibold text-white text-center">
        Folder Details
      </Text>
      <View className="flex-row gap-3">
        <TouchableOpacity onPress={handleUploadFile} hitSlop={12}>
          <Image
            source={icons.upload}
            className="w-8 h-8"
            resizeMode="contain"
          />
        </TouchableOpacity>
        {username &&
          folderDetails.user.username !== username &&
          folderDetails.type === FolderType.SHARED && (
            <TouchableOpacity onPress={handleLeave} hitSlop={12}>
              <Image
                source={icons.leave}
                className="w-8 h-8"
                resizeMode="contain"
              />
            </TouchableOpacity>
          )}

        {username && folderDetails.user.username === username && (
          <TouchableOpacity onPress={handleDelete} hitSlop={12}>
            <Image
              source={icons.delete_icon}
              className="w-8 h-8"
              resizeMode="contain"
            />
          </TouchableOpacity>
        )}
      </View>
    </View>

    <View className="items-center mb-17">
      <Image
        source={icons.folder}
        className="w-20 h-20 mb-8"
        resizeMode="contain"
      />
      <Text className="text-[24px] font-semibold text-white mb-4">
        Created Folder: {folderDetails.user.username}
      </Text>
      <Text className="text-[16px] text-zinc-300">
        Folder Name: {folderDetails.folderName}
      </Text>
      <Text className="text-[16px] text-zinc-300">
        Folder Type: {formatFolderType(folderDetails.type)}
      </Text>
      <Text className="text-[16px] text-zinc-300">
        Folder Size: {formatSize(folderDetails.size)}
      </Text>
      <Text className="text-[16px] text-zinc-300">
        Numbers File: {folderDetails.numberFile}
      </Text>
      <Text className="text-[16px] text-zinc-300">
        Created At: {formatDate(folderDetails.createdAt)}
      </Text>
      {folderDetails.parentFolderInfo != null && (
        <Text className="text-[16px] text-zinc-300">
          Parent Folder: {folderDetails.parentFolderInfo.folderName}
        </Text>
      )}
      {username && folderDetails.type === FolderType.SHARED && (
        <MemberCard members={members} />
      )}
    </View>
  </>
);

const FolderDetails = () => {
  const { folderId } = useLocalSearchParams();
  const [state, dispatch] = useReducer(reducer, firstState);
  const { token, username, setIsLogged, setUsername } = useAuthentication();
  const moveSheetRef = useRef<BottomSheet>(null);
  const router = useRouter();

  const listener = getSSE();

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
      router.replace(`/folders`);
    }
  }, [state.tag]);

  const openMoveSheet = (file: File) => {
    dispatch({ type: "select-file", file });
    moveSheetRef.current?.expand();
  };

  useEffect(() => {
    if (!listener) return;
    listener.addEventListener("file", handleNewFile);
    listener.addEventListener("newMember", handleNewMember);
    listener.addEventListener("leaveFolder", handleLeaveFolder);
    listener.addEventListener("deleteFile", handleDeleteFile);
    listener.addEventListener("deleteFolder", handleDeleteFolder);

    return () => {
      listener.removeEventListener("file", handleNewFile);
      listener.removeEventListener("newMember", handleNewMember);
      listener.removeEventListener("leaveFolder", handleLeaveFolder);
      listener.removeEventListener("deleteFile", handleDeleteFile);
      listener.removeEventListener("deleteFolder", handleDeleteFolder);
    };
  }, []);

  // Handle EventListener - New File
  const handleNewFile: EventSourceListener<CustomEventsFile> = (event) => {
    if (event.type === "file") {
      const eventData = JSON.parse(event.data);
      const newFile = {
        fileId: eventData.fileId,
        user: eventData.user,
        folderInfo: eventData.folderInfo,
        name: eventData.fileName,
        path: eventData.path,
        size: eventData.size,
        contentType: eventData.contentType,
        createdAt: eventData.createdAt,
        encryption: eventData.encryption,
        url: null,
      };

      dispatch({ type: "new-file", file: newFile });
    }
  };

  // Handle EventListener - New Member
  const handleNewMember: EventSourceListener<CustomEventsMember> = (event) => {
    if (event.type === "newMember") {
      const eventData = JSON.parse(event.data);
      const member = {
        id: eventData.newMember.id,
        username: eventData.newMember.username,
        email: eventData.newMember.email,
      };

      dispatch({ type: "new-member", member: member });
    }
  };

  // Handle EventListener - Leave Folder
  const handleLeaveFolder: EventSourceListener<CustomEventLeaveFolder> = (
    event
  ) => {
    if (event.type === "leaveFolder") {
      const eventData = JSON.parse(event.data);
      const member = {
        id: eventData.user.id,
        username: eventData.user.username,
        email: eventData.user.email,
      };

      dispatch({ type: "leave-user", member: member });
    }
  };

  // Handle EventListener - Delete Folder
  const handleDeleteFolder: EventSourceListener<CustomEventDeleteFolder> = (
    event
  ) => {
    if (event.type === "deleteFolder") {
      const eventData = JSON.parse(event.data);
      if (eventData.user.username === username) return;
      dispatch({ type: "delete-folder" });
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
        updatedAt: eventData.createdAt,
      });
    }
  };

  const fetchFileDetails = async () => {
    dispatch({ type: "start-loading" });
    try {
      const details = await getFolder(folderId.toString(), token);
      const files = await getFilesInFolder(folderId.toString(), token);
      const isPrivate = details.type === FolderType.PRIVATE;

      const folders = isPrivate
        ? await getFoldersInFolder(folderId.toString(), token, sortBy)
        : null;

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

  // Handle Fetch More Files
  const fetchMoreFiles = async () => {
    if (state.tag !== "loaded" || state.isFetchingMore || state.files.last) {
      return;
    }
    try {
      dispatch({ type: "fetch-more-start" });

      const nextPage = state.files.number + 1;

      const moreFiles = await getFilesInFolder(
        folderId.toString(),
        token,
        undefined,
        nextPage
      );

      dispatch({ type: "fetch-more-files-success", files: moreFiles });
    } catch (error) {
      dispatch({ type: "loading-error", error: error });
    }
  };

  // Handle Fetch More Folders
  const fetchMoreFolders = async () => {
    if (
      state.tag !== "loaded" ||
      state.isFetchingMore ||
      state.folders === null ||
      state.details.type !== FolderType.PRIVATE ||
      state.folders.last
    ) {
      return;
    }

    try {
      dispatch({ type: "fetch-more-start" });

      const nextPage = state.folders.number + 1;

      const moreFolders = await getFoldersInFolder(
        folderId.toString(),
        token,
        sortBy,
        nextPage
      );

      dispatch({ type: "fetch-more-folders-success", folders: moreFolders });
    } catch (error) {
      dispatch({ type: "loading-error", error: error });
    }
  };

  async function handleDelete() {
    if (state.tag !== "loaded") return;
    dispatch({ type: "delete-loading" });
    try {
      await deleteFolder(folderId.toString(), token);
      dispatch({ type: "success-delete" });
    } catch (error) {
      dispatch({ type: "loading-error", error: error });
    }
  }

  async function handleLeave() {
    if (state.tag !== "loaded") return;
    dispatch({ type: "leave-loading" });
    try {
      await leaveFolder(folderId.toString(), token);
      dispatch({ type: "success-leave" });
    } catch (error) {
      dispatch({ type: "loading-error", error: error });
    }
  }

  async function handleUploadFile() {
    if (folderId) {
      router.push({
        pathname: "/(modals)/create-file",
        params: { folderId: folderId },
      });
    } else {
      router.push("/(modals)/create-file");
    }
  }

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
          <ActivityIndicator size="large" color="#FFFFFF" />
          <Text className="mt-4 text-white text-base">Loading...</Text>
        </SafeAreaView>
      );

    case "redirect":
      return (
        <SafeAreaView className="bg-primary flex-1 justify-center items-center">
          <ActivityIndicator size="large" color="#fff" />
          <Text className="mt-4 text-white text-lg font-semibold">
            Leaving folder...
          </Text>
        </SafeAreaView>
      );

    case "loaded": {
      return (
        <GestureHandlerRootView style={{ flex: 1 }}>
          <SafeAreaView className="flex-1 bg-primary h-full px-6 py-10">
            <FlatList
              data={state.files.content}
              keyExtractor={(item) => String(item.fileId)}
              renderItem={({ item }) => (
                <FileItemComponent
                  item={item}
                  onMovePress={() => openMoveSheet(item)}
                  owner={state.details.user.username}
                />
              )}
              onEndReached={fetchMoreFiles}
              onEndReachedThreshold={0.1}
              contentContainerStyle={{ paddingBottom: 40 }}
              showsVerticalScrollIndicator={false}
              ListHeaderComponent={() => (
                <View className="my-6 px-4 space-y-6">
                  <FolderInfoDetails
                    folderDetails={state.details}
                    members={state.details.members}
                    username={username}
                    handleDelete={handleDelete}
                    handleLeave={handleLeave}
                    handleUploadFile={handleUploadFile}
                  />
                  {state.folders != null ? (
                    <View className="w-full flex-1 pt-5 pb-8">
                      <View className="flex-row items-center justify-between mb-3">
                        <Text className="text-2xl font-bold text-gray-100 mb-3">
                          Folders in {`${state.details.folderName}`}
                        </Text>
                      </View>

                      <FolderCard
                        folders={state.folders.content}
                        fetchFolders={fetchMoreFolders}
                      />
                    </View>
                  ) : null}
                  <View className="w-full flex-1 pt-2">
                    <Text className="text-2xl font-bold text-gray-100">
                      Files in {`${state.details.folderName}`}
                    </Text>
                  </View>
                </View>
              )}
              ListFooterComponent={() =>
                state.isFetchingMore ? (
                  <View className="bg-primary py-4 justify-center items-center">
                    <ActivityIndicator size="small" color="#fff" />
                  </View>
                ) : null
              }
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
            <FileActionsBottomSheet
              ref={moveSheetRef}
              file={state.selectFile}
            />
          </SafeAreaView>
        </GestureHandlerRootView>
      );
    }
  }
};

export default FolderDetails;
