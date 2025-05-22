import { View, Text, Image } from "react-native";
import React from "react";
import { images } from "../constants";

import CustomButton from "./CustomButton";
import { Href, router } from "expo-router";

interface EmptyStateProps {
  title: string;
  subtitle: string;
  page: Href;
  titleButton: string;
}

const EmptyState = ({
  title,
  subtitle,
  page,
  titleButton,
}: EmptyStateProps) => {
  return (
    <View className="flex justify-center items-center px-4">
      <Image
        source={images.empty}
        resizeMode="contain"
        className="w-[270px] h-[216px]"
      />

      <Text className="text-xl text-center font-psemibold text-white mt-2">
        {title}
      </Text>
      <Text className="text-sm font-pmedium text-gray-100">{subtitle}</Text>
      <CustomButton
        title={titleButton}
        handlePress={() => router.push(page)}
        containerStyles="w-full my-5"
        isLoading={false}
        textStyles={""}
        color="bg-secondary"
      />
    </View>
  );
};

export default EmptyState;
