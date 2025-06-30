import {
  View,
  Text,
  Image,
  TouchableOpacity,
  SafeAreaView,
} from "react-native";
import { useRouter } from "expo-router";
import { images } from "@/constants";
import CustomButton from "@/components/CustomButton";
import { useState } from "react";

const SignUpSuccess = () => {
  const router = useRouter();
  const [loading, setLoading] = useState(false);

  const handleSuccess = () => {
    setLoading(true);
    router.replace("/sign-in");
  };

  return (
    <SafeAreaView className="bg-primary h-full">
      <View className="flex-1 justify-center items-center px-5">
        <Image
          source={images.cloudSuccess}
          resizeMode="contain"
          className="w-[250px] h-[250px] mb-44"
        />

        <Text className="text-white text-2xl font-semibold mb-20">
          Registration Complete
        </Text>

        <CustomButton
          title="Done"
          handlePress={handleSuccess}
          isLoading={loading}
          containerStyles="w-full mb-4 bg-secondary-200 rounded-lg py-4"
          textStyles="text-white text-center font-bold"
          color="bg-green-500"
        />
      </View>
    </SafeAreaView>
  );
};

export default SignUpSuccess;
