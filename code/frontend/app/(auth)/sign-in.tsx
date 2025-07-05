import {
  View,
  Image,
  Text,
  ScrollView,
  Alert,
  ActivityIndicator,
} from "react-native";
import { useEffect, useReducer, useState } from "react";
import { SafeAreaView } from "react-native-safe-area-context";
import { images } from "@/constants";
import CustomButtom from "@/components/CustomButton";
import { Link, router } from "expo-router";
import FormField from "@/components/FormField";
import {
  getProblemMessage,
  isProblem,
  Problem,
} from "@/services/media/Problem";
import {
  KEY_MASTER,
  KEY_NAME,
  TOKEN,
  useAuthentication,
} from "@/context/AuthProvider";
import { getCredentials, login } from "@/services/users/UserService";
import {
  convertStringToArrayBuffer,
  generateMasterKey,
} from "@/services/security/SecurityService";
import { getValueFor, save } from "@/services/storage/SecureStorage";
import { initializeSSE } from "@/services/notifications/SSEManager";

type State =
  | {
      tag: "editing";
      error?: Problem | string;
      inputs: { username: string; password: string };
    }
  | { tag: "submitting"; username: string }
  | { tag: "redirect" };

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
        return { tag: "submitting", username: state.inputs.username };
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
          inputs: { username: state.username, password: "" },
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
  inputs: { username: "", password: "" },
};
const SignIn = () => {
  const [state, dispatch] = useReducer(reducer, firstState);
  const { setUsername, setToken, setKeyMaster } = useAuthentication();

  useEffect(() => {
    if (state.tag === "redirect") {
      router.replace("/home");
    }
  }, [state.tag]);
  function handleChange(inputName: string, inputValue: string) {
    dispatch({
      type: "edit",
      inputName,
      inputValue,
    });
  }

  async function handleSubmit() {
    if (state.tag !== "editing") {
      return;
    }
    dispatch({ type: "submit" });

    const username = state.inputs.username;
    const password = state.inputs.password;

    try {
      if (!username?.trim() || !password?.trim()) {
        Alert.alert("Error", "Please fill in all fields");
        dispatch({ type: "error", message: "Invalid username or password" });
        return;
      }

      const result = await login(username, password);
      if (!result) {
        dispatch({ type: "error", message: "Invalid username or password" });
        return;
      }
      await save(
        `${TOKEN}${username}`,
        JSON.stringify({ token: result.token })
      );
      setToken(result.token);
      await save(KEY_NAME, JSON.stringify({ username: username }));
      const isMasterKey = await getValueFor(`${KEY_MASTER}${username}`);
      if (isMasterKey === null) {
        const credentials = await getCredentials(result.token);
        const salt = convertStringToArrayBuffer(credentials.salt);
        const masterKey = await generateMasterKey(
          salt,
          password,
          credentials.iterations
        );
        await save(
          `${KEY_MASTER}${username}`,
          JSON.stringify({ masterKey: masterKey })
        );
        setKeyMaster(masterKey);
      }

      setUsername(username);
      initializeSSE();
      dispatch({ type: "success" });
    } catch (error) {
      Alert.alert(
        "Error",
        `${isProblem(error) ? getProblemMessage(error) : error}`
      );
      dispatch({ type: "error", message: error });
    }
  }

  const username =
    state.tag === "submitting" && state.username
      ? state.username
      : state.inputs?.username || "";
  const password =
    state.tag === "submitting" ? "" : state.inputs?.password || "";

  // Render UI
  switch (state.tag) {
    case "submitting":
      return (
        <SafeAreaView className="bg-primary flex-1 justify-center items-center">
          <ActivityIndicator size="large" color="#fff" />
          <Text className="mt-4 text-white text-lg font-semibold">
            Logging in...
          </Text>
        </SafeAreaView>
      );

    case "redirect":
      return (
        <SafeAreaView className="bg-primary flex-1 justify-center items-center">
          <ActivityIndicator size="large" color="#fff" />
          <Text className="mt-4 text-white text-lg font-semibold">
            Redirecting to your dashboard...
          </Text>
        </SafeAreaView>
      );
    case "editing":
      return (
        <SafeAreaView className="bg-primary h-full">
          <ScrollView>
            <View className="w-full justify-center min-h-[83h] px-4 my-6">
              <View className="relative flex-row items-center justify-center mt-2 mb-2 h-[50px]">
                <Image
                  source={images.logo}
                  resizeMode="contain"
                  className="absolute left-0 w-[72px] h-[78px]"
                />

                <Text className="text-[24px] text-white font-psemibold text-center">
                  Login
                </Text>
              </View>

              <FormField
                title="Username"
                value={username}
                handleChangeText={(text) => handleChange("username", text)}
                otherStyles="mt-7"
                keyboardType="email-address"
                placeholder={"Enter a valid username"}
              />
              <FormField
                title="Password"
                value={password}
                handleChangeText={(text) => handleChange("password", text)}
                otherStyles="mt-7"
                placeholder={"Enter a valid password"}
              />

              <CustomButtom
                title="Sign In"
                handlePress={handleSubmit}
                containerStyles="mt-7"
                isLoading={false}
                textStyles={""}
                color="bg-secondary"
              />

              <View className="flex justify-center pt-5 flex-row gap-2">
                <Text className="text-lg text-gray-100 font-pregular">
                  Don't have an account?
                </Text>
                <Link
                  href="/sign-up"
                  className="text-lg font-psemibold text-secondary"
                >
                  Register
                </Link>
              </View>
            </View>
          </ScrollView>
        </SafeAreaView>
      );
  }
};

export default SignIn;
