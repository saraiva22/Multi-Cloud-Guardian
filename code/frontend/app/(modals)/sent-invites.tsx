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
import React, { act, useEffect, useReducer } from "react";
import { PageResult } from "@/domain/utils/PageResult";
import { Invite } from "@/domain/storage/Invite";
import {
  getProblemMessage,
  isProblem,
  Problem,
} from "@/services/media/Problem";
import { KEY_NAME, useAuthentication } from "@/context/AuthProvider";
import { removeValueFor } from "@/services/storage/SecureStorage";
import { router } from "expo-router";
import { getSentInvites } from "@/services/storage/StorageService";
import { icons } from "@/constants";
import InviteItemComponent from "@/components/InviteItemComponent";
import EmptyState from "@/components/EmptyState";
import { getSSE } from "@/services/notifications/SSEManager";
import { EventSourceListener } from "react-native-sse";

// The State
type State =
  | { tag: "begin" }
  | { tag: "loading" }
  | { tag: "loaded"; invites: PageResult<Invite> }
  | { tag: "error"; error: Problem | string };

// The Action
type Action =
  | { type: "start-loading" }
  | { type: "loading-success"; invites: PageResult<Invite> }
  | { type: "loading-error"; error: Problem | string }
  | { type: "invite-response"; invite: Invite };

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
      };
    case "loading-error":
      if (state.tag !== "loading") {
        return logUnexpectedAction(state, action);
      }
      return { tag: "error", error: action.error };
    case "invite-response":
      if (state.tag !== "loaded") {
        return logUnexpectedAction(state, action);
      }
      return {
        tag: "loaded",
        invites: {
          ...state.invites,
          content: state.invites.content.map((invite) =>
            invite.inviteId === action.invite.inviteId
              ? { ...invite, status: action.invite.status }
              : invite
          ),
        },
      };
  }
}

const firstState: State = { tag: "begin" };
type CustomEvents = "inviteResponse";

const SentInvites = () => {
  const [state, dispatch] = useReducer(reducer, firstState);
  const { token, setIsLogged, setUsername } = useAuthentication();
  const listener = getSSE();

  useEffect(() => {
    if (state.tag === "begin") {
      dispatch({ type: "start-loading" });
      handleGetInvites();
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

  useEffect(() => {
    if (listener) {
      listener.addEventListener("inviteResponse", handleInvite);
    }
  }, []);

  // Handle EventListener - New Invite
  const handleInvite: EventSourceListener<CustomEvents> = (event) => {
    console.log("EVENTO ", event);
    if (event.type === "inviteResponse") {
      const eventData = JSON.parse(event.data);
      const newInvite = {
        inviteId: eventData.inviteId,
        folderId: eventData.folderId,
        folderName: eventData.folderName,
        user: eventData.user,
        status: eventData.status,
      };

      dispatch({ type: "invite-response", invite: newInvite });
    }
  };

  // Handle FetchInvites
  async function handleGetInvites() {
    try {
      const invites = await getSentInvites(token);
      dispatch({ type: "loading-success", invites });
    } catch (error) {
      Alert.alert(
        "Error",
        `${isProblem(error) ? getProblemMessage(error) : error}`
      );
      dispatch({ type: "loading-error", error: error });
    }
  }

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
          <ActivityIndicator />
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
            Sent Invites
          </Text>
          <FlatList
            data={state.invites.content}
            keyExtractor={(item) => String(item.inviteId)}
            renderItem={({ item }) => (
              <InviteItemComponent isReceived={true} item={item} />
            )}
            ListEmptyComponent={() => (
              <EmptyState
                title="No Invites Sent"
                subtitle="You haven't sent any invites yet"
                page="/(modals)/create-invite"
                titleButton="Send Invite"
              />
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
export default SentInvites;
