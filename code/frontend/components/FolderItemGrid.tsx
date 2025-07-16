import { View, Text, TouchableOpacity, Image } from "react-native";
import React, { useRef } from "react";
import { images } from "@/constants";
import { formatFolderType, formatSize } from "@/services/utils/Function";
import { useRouter } from "expo-router";
import { Folder } from "@/domain/storage/Folder";

type Props = {
  item: Folder;
  activeItem?: string;
  setActiveItem?: (id: string) => void;
};

const FolderGridItemComponent = ({ item }: Props) => {
  const router = useRouter();
  const hasNavigated = useRef(false);

  const handlePress = () => {
    if (hasNavigated.current) return;
    hasNavigated.current = true;

    router.push(`/folders/${item.folderId}`);
    setTimeout(() => {
      hasNavigated.current = false;
    }, 2000);
  };

  return (
    <TouchableOpacity
      onPress={handlePress}
      className="w-[48%] h-36 bg-blue-500 rounded-2xl p-3 mb-4 mr-2 shadow-md justify-between"
      activeOpacity={0.85}
    >
      <View className="flex-row justify-between items-center">
        <Image
          source={images.folder}
          className="w-10 h-10"
          resizeMode="contain"
        />
        <Text className="text-white text-xs font-semibold ">
          {formatFolderType(item.type)}
        </Text>
      </View>

      <View className="flex-row justify-between items-center mt-2">
        <Text
          numberOfLines={1}
          className="text-white text-base font-semibold flex-1"
        >
          {item.folderName}
        </Text>
      </View>
      <View>
        <Text className="text-white text-xs opacity-80">
          Used: {item.size > 0 ? formatSize(item.size) : "0 Kb"}
        </Text>
        <Text className="text-white text-xs opacity-80">
          {item.numberFile > 0 ? item.numberFile : 0} files
        </Text>
      </View>
    </TouchableOpacity>
  );
};

export default FolderGridItemComponent;
