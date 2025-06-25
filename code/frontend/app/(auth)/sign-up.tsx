import { View, Image, Text, ScrollView, Alert } from "react-native";
import { useEffect, useReducer, useState } from "react";
import { SafeAreaView } from "react-native-safe-area-context";
import { images } from "../../constants";
import FormField from "../../components/FormField";
import CustomButtom from "../../components/CustomButton";
import { Link, router } from "expo-router";
import {
  generateMasterKey,
  generateRandomSalt,
  generateRandomNumber,
  convertArrayBufferToString,
} from "../../services/security/SecurityService";
import { COST_ARRAY, CostType } from "@/domain/preferences/CostType";
import SliderState from "@/components/SliderState";
import {
  LOCATION_ARRAY,
  LocationType,
} from "@/domain/preferences/LocationType";
import {
  getProblemMessage,
  isProblem,
  Problem,
} from "@/services/media/Problem";
import { register } from "@/services/users/UserService";
import { save } from "@/services/storage/SecureStorage";
import {
  MAX_ITERATIONS,
  MIN_ITERATIONS,
} from "@/domain/credentials/Credentials";
import { useAuthentication } from "@/context/AuthProvider";

const KEY_NAME = "user_info";
const KEY_MASTER = "key_master-";

type State =
  | {
      tag: "editing";
      error?: Problem | string;
      inputs: {
        username: string;
        email: string;
        password: string;
        cost: CostType;
        location: LocationType;
      };
    }
  | {
      tag: "submitting";
      username: string;
      email: string;
      cost: CostType;
      location: LocationType;
    }
  | { tag: "redirect" };

type Action =
  | { type: "edit"; inputName: string; inputValue: string | number }
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
          error: state.error,
          inputs: { ...state.inputs, [action.inputName]: action.inputValue },
        };
      } else if (action.type === "submit") {
        return {
          tag: "submitting",
          username: state.inputs.username,
          email: state.inputs.email,
          cost: state.inputs.cost,
          location: state.inputs.location,
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
            username: state.username,
            email: state.email,
            password: "",
            cost: CostType.LOW,
            location: LocationType.NORTH_AMERICA,
          },
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
  inputs: {
    username: "",
    email: "",
    password: "",
    cost: CostType.LOW,
    location: LocationType.NORTH_AMERICA,
  },
};

const SignUp = () => {
  const [state, dispatch] = useReducer(reducer, firstState);
  const { setKeyMaster } = useAuthentication();

  useEffect(() => {
    if (state.tag === "redirect") {
      router.replace("/(modals)/signup-success");
    }
  }, [state.tag]);

  function handleChange(inputName: string, inputValue: string | number) {
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
    const email = state.inputs.email;
    const password = state.inputs.password;
    const cost = state.inputs.cost;
    const location = state.inputs.location;

    if (!username?.trim() || !password?.trim() || !email?.trim()) {
      Alert.alert("Error", "Please fill in all fields");
      dispatch({ type: "error", message: "Invalid username or password" });
      return;
    }

    try {
      const saltArrayBuffer = await generateRandomSalt();
      const salt = convertArrayBufferToString(saltArrayBuffer);

      const iterations = generateRandomNumber(MIN_ITERATIONS, MAX_ITERATIONS);
      await register(
        username,
        email,
        password,
        salt,
        iterations,
        cost,
        location
      );

      await save(KEY_NAME, JSON.stringify({ username: username }));
      const masterKey = await generateMasterKey(
        saltArrayBuffer,
        password,
        iterations
      );
      await save(
        `${KEY_MASTER}${username}`,
        JSON.stringify({ masterKey: masterKey })
      );
      setKeyMaster(masterKey);
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
  const email =
    state.tag === "submitting" && state.email
      ? state.email
      : state.inputs?.email || "";
  const password =
    state.tag === "submitting" ? "" : state.inputs?.password || "";

  const cost =
    state.tag === "submitting" && state.cost
      ? state.cost
      : state.inputs?.cost || 0;

  const location =
    state.tag === "submitting" && state.location
      ? state.location
      : state.inputs?.location || 0;
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
              Register
            </Text>
          </View>
          <FormField
            title="Username"
            value={username}
            handleChangeText={(text) => handleChange("username", text)}
            otherStyles="mt-5"
            placeholder={"At least 5 characters"}
          />
          <FormField
            title="Email"
            value={email}
            handleChangeText={(text) => handleChange("email", text)}
            otherStyles="mt-5"
            keyboardType="email-address"
            placeholder={"Enter a valid email"}
          />
          <FormField
            title="Password"
            value={password}
            handleChangeText={(text) => handleChange("password", text)}
            otherStyles="mt-5"
            placeholder={"Min 8 chars, 1 upper, 1 lower, 1 special"}
          />
          <SliderState
            title="Location"
            value={location}
            handleChange={(value) => handleChange("location", value)}
            otherStyles="mt-5"
            state={LOCATION_ARRAY}
          />

          <SliderState
            title="Cost"
            value={cost}
            handleChange={(value) => handleChange("cost", value)}
            otherStyles="mt-5"
            state={COST_ARRAY}
          />

          <CustomButtom
            title="Sign In"
            handlePress={handleSubmit}
            containerStyles="mt-5"
            isLoading={state.tag === "submitting"}
            textStyles={""}
            color="bg-secondary"
          />

          <View className="flex justify-center pt-5 flex-row gap-2">
            <Text className="text-lg text-gray-100 font-pregular">
              Already have an account?
            </Text>
            <Link
              href="/sign-in"
              className="text-lg font-psemibold text-secondary"
            >
              Login
            </Link>
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

export default SignUp;
