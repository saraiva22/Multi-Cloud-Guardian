import { View, Image, Text, ScrollView, Alert } from "react-native";
import { useState } from "react";
import { SafeAreaView } from "react-native-safe-area-context";
import { images } from "../../constants";
import CustomButtom from "../../components/CustomButton";
import { Link, router } from "expo-router";
import FormField from "@/components/FormField";

const SignIn = () => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [form, setForm] = useState({
    username: "",
    password: "",
  });
  return (
    <SafeAreaView className="bg-primary h-full">
      <ScrollView>
        <View className="w-full justify-center min-h-[83h] px-4 my-6">
          <View className="relative flex-row items-center justify-center mt-10 mb-6 h-[50px]">
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
            value={form.username}
            handleChangeText={() => console.log()}
            otherStyles="mt-7"
            keyboardType="email-address"
            placeholder={""}
          />
          <FormField
            title="Password"
            value={form.password}
            handleChangeText={(e) => setForm({ ...form, password: e })}
            otherStyles="mt-7"
            placeholder={""}
          />

          <CustomButtom
            title="Sign In"
            handlePress={() => console.log("Sign In")}
            containerStyles="mt-7"
            isLoading={isSubmitting}
            textStyles={""}
          />

          <View className="flex justify-center pt-5 flex-row gap-2">
            <Text className="text-lg text-gray-100 font-pregular">
              Don't have an account?
            </Text>
            <Link
              href="/sign-up"
              className="text-lg font-psemibold text-secondary"
            >
              Signup
            </Link>
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

export default SignIn;
