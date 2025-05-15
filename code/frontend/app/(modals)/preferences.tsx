import { View, Text, Image, useColorScheme } from "react-native";
import React from "react";
import { useAuthentication } from "@/context/AuthProvider";
import { icons } from "@/constants";

const Preferences = () => {
  const { username } = useAuthentication();
  // const { colorScheme } = useColorScheme();

  return (
    <View className="flex-1 bg-white dark:bg-neutral-900 px-6 py-8">
      {/* Header */}
      <Text className="text-xl font-semibold text-neutral-900 dark:text-white mb-6 text-center">
        Preferences
      </Text>

      {/* Icon */}
      <View className="items-center mb-8">
        <View className="bg-neutral-100 dark:bg-neutral-800 rounded-full p-5 mb-2">
          {/* Usa aqui o teu ícone SVG ou imagem */}
          <Image
            source={icons.preferences} // substitui pelo teu ícone
            className="w-16 h-16"
            resizeMode="contain"
          />
        </View>
      </View>

      {/* Location Card */}
      <View className="flex-row items-center justify-between bg-white dark:bg-neutral-800 border border-blue-400 dark:border-blue-300 rounded-xl px-5 py-4 mb-4 shadow-md">
        <View>
          <Text className="text-base font-semibold text-neutral-900 dark:text-white">
            Location
          </Text>
          <Text className="text-sm text-neutral-500 dark:text-neutral-400">
            Europe
          </Text>
        </View>
        <Image
          source={icons.europe} // substitui pelo teu ícone da bandeira
          className="w-10 h-7"
          resizeMode="contain"
        />
      </View>

      {/* Performance Card */}
      <View className="flex-row items-center justify-between bg-white dark:bg-neutral-800 rounded-xl px-5 py-4 shadow-md">
        <View>
          <Text className="text-base font-semibold text-neutral-900 dark:text-white">
            Performance
          </Text>
          <Text className="text-sm text-neutral-500 dark:text-neutral-400">
            Medium
          </Text>
        </View>
        <Image
          source={icons.medium} // substitui pelo teu ícone
          className="w-10 h-7"
          resizeMode="contain"
        />
      </View>
    </View>
  );
};

export default Preferences;
