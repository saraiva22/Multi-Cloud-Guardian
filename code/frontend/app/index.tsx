import { router } from "expo-router";
import { ScrollView, Text, View, Image } from "react-native";
import { StatusBar } from "expo-status-bar";
import "../global.css";
import { SafeAreaView } from "react-native-safe-area-context";
import { images } from "../constants";
import CustomButton from "../components/CustomButton";

export default function App() {
  return (
    <SafeAreaView className="bg-primary h-full">
      <ScrollView contentContainerStyle={{ flexGrow: 1 }}>
        <View className="w-full justify-center items-center min-h-[85vh] px-4">
          <View className="flex-row items-center mt-5">
            <Image
              source={images.logo}
              className="w-[73px] h-[80px] mr-1"
              resizeMode="contain"
            />
            <Text className="text-3xl text-white font-bold">
              Multi Cloud Guardian
            </Text>
          </View>

          <Image
            source={images.cards}
            className="w-full max-w-[375px] h-[290px] mt-5"
            resizeMode="contain"
          />

          <Text className="text-4xl text-white font-bold text-center mt-5">
            Freedom to Store,{"\n"}
            Power to <Text className="text-secondary-200">Protect</Text>
          </Text>
          <Text className="text-base text-gray-100 text-center mt-5 max-w-[500px] self-center">
            Securely store and manage your files{"\n"}with privacy-first
            encryption
          </Text>
          <View className="w-full mt-10">
            <CustomButton
              title="Login"
              handlePress={() => router.push("/sign-in")}
              containerStyles="w-full mb-4 bg-secondary-200 rounded-lg py-4"
              textStyles="text-black text-center font-bold"
              isLoading={false}
            />
            <CustomButton
              title="Register"
              handlePress={() => router.push("/sign-up")}
              containerStyles="w-full bg-secondary-200 rounded-lg py-4"
              textStyles="text-black text-center font-bold"
              isLoading={false}
            />
          </View>
        </View>
      </ScrollView>
      <StatusBar backgroundColor="#161622" style="light" />
    </SafeAreaView>
  );
}
