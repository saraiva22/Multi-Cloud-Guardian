import {
  View,
  Text,
  Button,
  SafeAreaViewBase,
  SafeAreaView,
  FlatList,
  TouchableOpacity,
  Image,
} from "react-native";
import { icons } from "@/constants";
import { useAuthentication } from "@/context/AuthProvider";
import { removeValueFor } from "@/services/storage/SecureStorage";
import { getUserByUsername, logout } from "@/services/users/UserService";
import { router } from "expo-router";
import React from "react";
import CustomButtom from "../../components/CustomButton";

const KEY_NAME = "user_info";
const ProfileScreen = () => {
  const { username, setUsername, setIsLogged } = useAuthentication();

  async function goLogout() {
    try {
      await logout();
    } catch (error) {
      console.log(error);
    } finally {
      setUsername(null);
      setIsLogged(false);
      removeValueFor(KEY_NAME);
      router.replace("/sign-in");
    }
  }

  const handlePreferences = async () => {
    if (username) {
      router.push("/(modals)/preferences");
    }
  };

  const handleStorageDetails = async () => {
    if (username) {
      router.push("/(modals)/storage-details");
    }
  };
  return (
    <SafeAreaView className="bg-primary h-full">
      <View className="items-center mt-16">
        <Text className="text-[26px] font-semibold color-white mb-10">
          Profile
        </Text>
      </View>
      <View className="flex-row items-center space-x-10 px-4 mb-10">
        <Image
          source={icons.profile}
          className="w-20 h-20 rounded-full"
          resizeMode="cover"
        />
        <View className="justify-center ml-7">
          <Text className="text-[20px] font-semibold text-white mb-1">
            {username}
          </Text>
        </View>
      </View>

      <View className="space-y-5 px-4">
        <TouchableOpacity className="flex-row items-center justify-between py-3 bg-primary rounded-lg">
          <View className="flex-row items-center space-x-4">
            <Image
              source={icons.add_friend}
              className="w-6 h-6"
              style={{ tintColor: "#FFFFFF" }}
            />
            <Text className="text-[18px] text-white ml-7">Add Friend</Text>
          </View>
          <Text className="text-[22px] text-gray-200">{">"}</Text>
        </TouchableOpacity>

        <TouchableOpacity
          onPress={handleStorageDetails}
          className="flex-row items-center justify-between py-3 bg-primary rounded-lg"
        >
          <View className="flex-row items-center space-x-4">
            <Image
              source={icons.storage}
              className="w-6 h-6"
              style={{ tintColor: "#FFFFFF" }}
            />
            <Text className="text-[18px] text-white ml-7">Storage Details</Text>
          </View>
          <Text className="text-[22px] text-gray-200">{">"}</Text>
        </TouchableOpacity>

        <TouchableOpacity
          onPress={handlePreferences}
          className="flex-row items-center justify-between py-3 bg-primary rounded-lg"
        >
          <View className="flex-row items-center space-x-4">
            <Image
              source={icons.settings}
              className="w-6 h-6"
              style={{ tintColor: "#FFFFFF" }}
            />
            <Text className="text-[18px] text-white ml-7">Preferences</Text>
          </View>
          <Text className="text-[22px] text-gray-200">{">"}</Text>
        </TouchableOpacity>
      </View>

      <CustomButtom
        title="Logout"
        handlePress={goLogout}
        containerStyles="mt-48"
        isLoading={false}
        textStyles={""}
        color="bg-secondary rounded-full  mx-4 mb-4 h-[50px] w-[80%] self-center"
      />
    </SafeAreaView>
  );
};

export default ProfileScreen;
