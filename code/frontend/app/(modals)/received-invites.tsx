import {
  View,
  Text,
  Alert,
  SafeAreaView,
  ActivityIndicator,
  TouchableOpacity,
  Image,
  FlatList,
} from "react-native";
import React, { useCallback, useEffect, useReducer, useRef } from "react";
import { PageResult } from "@/domain/utils/PageResult";
import { Invite } from "@/domain/storage/Invite";
import {
  getProblemMessage,
  isProblem,
  Problem,
} from "@/services/media/Problem";
import { KEY_NAME, useAuthentication } from "@/context/AuthProvider";
import { getSSE } from "@/services/notifications/SSEManager";
import { EventSourceListener } from "react-native-sse";
import {
  getReceivedInvites,
  validateFolderInvite,
} from "@/services/storage/StorageService";
import { router } from "expo-router";
import { icons } from "@/constants";

import { removeValueFor } from "@/services/storage/SecureStorage";
import { InviteStatusType } from "@/domain/storage/InviteStatusType";
import InviteItemComponent from "@/components/InviteItemComponent";

// The State
type State =
  | { tag: "begin" }
  | { tag: "loading" }
  | { tag: "loaded"; invites: PageResult<Invite>; isFetchingMore: boolean }
  | { tag: "validating"; inviteId: number; invites: PageResult<Invite> }
  | { tag: "error"; error: Problem | string }
  | { tag: "redirect"; folderId: number };

// The Action
type Action =
  | { type: "start-loading" }
  | { type: "loading-success"; invites: PageResult<Invite> }
  | { type: "loading-error"; error: Problem | string }
  | { type: "fetch-more-start" }
  | { type: "fetch-more-success"; invites: PageResult<Invite> }
  | { type: "start-validating"; inviteId: number }
  | { type: "new-invite"; invite: Invite }
  | { type: "accepted-invite"; folderId: number }
  | { type: "reject-invite"; inviteId: number };

// The Logger
function logUnexpectedAction(state: State, action: Action) {
  console.log(`Unexpected action '${action.type} on state '${state.tag}'`);
  return state;
}

// The Reducer
function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "start-loading":
      return { tag: "loading" };
    case "loading-success":
      if (state.tag !== "loading") {
        return logUnexpectedAction(state, action);
      }
      return {
        tag: "loaded",
        invites: action.invites,
        isFetchingMore: false,
      };
    case "loading-error":
      if (state.tag !== "loading") {
        return logUnexpectedAction(state, action);
      }
      return { tag: "error", error: action.error };

    case "new-invite":
      if (state.tag !== "loaded") {
        return logUnexpectedAction(state, action);
      }
      return {
        ...state,
        invites: {
          ...state.invites,
          content: [...state.invites.content, action.invite],
          totalElements: state.invites.totalElements + 1,
        },
      };

    case "accepted-invite":
      if (state.tag !== "validating") {
        return logUnexpectedAction(state, action);
      }
      return { tag: "redirect", folderId: action.folderId };

    case "reject-invite":
      if (state.tag !== "validating") {
        return logUnexpectedAction(state, action);
      }
      return {
        tag: "loaded",
        invites: {
          ...state.invites,
          content: state.invites.content.map((invite) =>
            invite.inviteId === action.inviteId
              ? { ...invite, status: InviteStatusType.REJECT }
              : invite
          ),
        },
        isFetchingMore: false,
      };
    case "start-validating":
      if (state.tag !== "loaded") {
        return logUnexpectedAction(state, action);
      }
      return {
        tag: "validating",
        inviteId: action.inviteId,
        invites: state.invites,
      };

    case "fetch-more-start":
      if (state.tag !== "loaded") {
        return logUnexpectedAction(state, action);
      }
      return {
        ...state,
        isFetchingMore: true,
      };

    case "fetch-more-success":
      if (state.tag !== "loaded") {
        return logUnexpectedAction(state, action);
      }
      return {
        ...state,
        invites: {
          ...action.invites,
          content: [...state.invites.content, ...action.invites.content],
        },
        isFetchingMore: false,
      };
  }
}

const firstState: State = { tag: "begin" };
type CustomEvents = "invite";

const ReceivedInvites = () => {
  const [state, dispatch] = useReducer(reducer, firstState);
  const { token, setIsLogged, setUsername } = useAuthentication();
  const listener = getSSE();
  const hasNavigated = useRef(false);

  useEffect(() => {
    if (state.tag === "begin") {
      handleGetInvites();
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

    if (state.tag === "redirect") {
      router.replace(`/folders/${state.folderId}`);
    }
  }, [state]);

  useEffect(() => {
    if (!listener) return;

    listener.addEventListener("invite", handleInvite);

    return () => {
      listener.removeEventListener("invite", handleInvite);
    };
  }, []);

  // Handle EventListener - New Invite
  const handleInvite: EventSourceListener<CustomEvents> = (event) => {
    if (event.type === "invite") {
      const eventData = JSON.parse(event.data);
      const newInvite = {
        inviteId: eventData.inviteId,
        folderId: eventData.folderId,
        folderName: eventData.folderName,
        user: eventData.user,
        status: eventData.status,
      };
      dispatch({ type: "new-invite", invite: newInvite });
    }
  };

  // Handle FetchInvites()
  async function handleGetInvites() {
    dispatch({ type: "start-loading" });
    try {
      const invites = await getReceivedInvites(token);
      dispatch({ type: "loading-success", invites });
    } catch (error) {
      Alert.alert(
        "Error",
        `${isProblem(error) ? getProblemMessage(error) : error}`
      );
      dispatch({ type: "loading-error", error: error });
    }
  }

  // Handle Fetch More Invites
  const fetchMoreFolders = async () => {
    if (state.tag !== "loaded" || state.isFetchingMore || state.invites.last) {
      return;
    }

    try {
      dispatch({ type: "fetch-more-start" });

      const nextPage = state.invites.number + 1;

      const moreInvites = await getReceivedInvites(token, undefined, nextPage);

      dispatch({ type: "fetch-more-success", invites: moreInvites });
    } catch (error) {
      dispatch({ type: "loading-error", error: error });
    }
  };

  // Handle Validate Invite()
  async function handleValidationInvite(
    folderId: number,
    inviteId: number,
    status: InviteStatusType
  ) {
    if (state.tag === "redirect") return;
    if (hasNavigated.current) return;

    try {
      dispatch({ type: "start-validating", inviteId });

      await validateFolderInvite(
        folderId.toString(),
        inviteId.toString(),
        status,
        token
      );

      if (status === InviteStatusType.ACCEPT) {
        hasNavigated.current = true;
        dispatch({ type: "accepted-invite", folderId });
      } else {
        dispatch({ type: "reject-invite", inviteId });
      }
    } catch (error) {
      Alert.alert(
        "Error",
        `${isProblem(error) ? getProblemMessage(error) : error}`
      );
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
          <ActivityIndicator size="large" color="#fff" />
        </SafeAreaView>
      );

    case "validating":
      return (
        <SafeAreaView className="bg-primary flex-1 justify-center items-center">
          <ActivityIndicator size="large" color="#fff" />
          <Text className="mt-4 text-white text-lg font-semibold">
            Accepting invite...
          </Text>
        </SafeAreaView>
      );

    case "redirect":
      return (
        <SafeAreaView className="bg-primary flex-1 justify-center items-center">
          <ActivityIndicator size="large" color="#fff" />
          <Text className="mt-4 text-white text-lg font-semibold">
            Redirect...
          </Text>
        </SafeAreaView>
      );
    case "loaded": {
      return (
        <SafeAreaView className="flex-1 bg-primary h-full px-6 py-12">
          <TouchableOpacity
            className="absolute left-6 top-6 z-10 mt-16"
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
            Received Invites
          </Text>
          <FlatList
            data={state.invites.content}
            keyExtractor={(item) => String(item.inviteId)}
            renderItem={({ item }) => (
              <InviteItemComponent
                isReceived={true}
                item={item}
                onPress={(status) =>
                  handleValidationInvite(item.folderId, item.inviteId, status)
                }
              />
            )}
            ListFooterComponent={() =>
              state.isFetchingMore ? (
                <View className="bg-primary py-4 justify-center items-center">
                  <ActivityIndicator size="small" color="#fff" />
                </View>
              ) : null
            }
            onEndReached={fetchMoreFolders}
            onEndReachedThreshold={0.1}
            ListEmptyComponent={() => (
              <Text className="text-[18px] font-semibold text-white text-center mb-16 mt-4">
                Not Received Invite
              </Text>
            )}
          />
        </SafeAreaView>
      );
    }
    case "error":
      return (
        <SafeAreaView className="bg-primary flex-1">
          <ActivityIndicator />
          <Text className="text-[24px] font-semibold text-white text-center mb-16 mt-4">
            {state.tag === "error" && "Go to Sign In"}
          </Text>
        </SafeAreaView>
      );
  }
};

export default ReceivedInvites;
