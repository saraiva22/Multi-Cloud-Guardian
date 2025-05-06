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
import { logout } from "@/services/users/UserService";
import { router } from "expo-router";

const KEY_NAME = "user_info";
const KEY_MASTER = "key_master-";

const Settings = () => {
  const { username, setUsername, setIsLogged } = useAuthentication();

  const goLogout = async () => {
    const value = username;
    console.log(value);
    try {
      await logout();
    } catch (error) {
      console.log(error);
    } finally {
      setUsername(null);
      setIsLogged(false);
      removeValueFor(KEY_NAME);
      removeValueFor(`${KEY_MASTER}${value}`);
      router.replace("/sign-in");
    }
  };

  return (
    <SafeAreaView className="bg-primary h-full">
      <View className="w-full flex justify-center items-center mt-6 mb-12 px-4">
        <TouchableOpacity
          onPress={goLogout}
          className="flex w-full items-end mb-10"
        >
          <Image
            source={icons.logout}
            resizeMode="contain"
            className="w-6 h-6"
          />
        </TouchableOpacity>

        <View className="w-16 h-16 border border-secondary rounded-lg flex justify-center items-center">
          <Image
            source={icons.logo}
            className="w-[90%] h-[90%] rounded-lg"
            resizeMode="cover"
          />
        </View>
      </View>
    </SafeAreaView>
  );
};

export default Settings;
