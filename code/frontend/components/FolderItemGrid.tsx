import { View, Text, TouchableOpacity, Image } from "react-native";
import React from "react";
import { images } from "@/constants";
import { formatFolderType, formatSize } from "@/services/utils/Function";
import { useRouter } from "expo-router";
import { Folder } from "@/domain/storage/Folder";
import { MaterialIcons } from "@expo/vector-icons";

type Props = {
  item: Folder;
  activeItem?: string;
  setActiveItem?: (id: string) => void;
};

const FolderGridItemComponent = ({ item }: Props) => {
  const router = useRouter();

  const handlePress = () => {
    router.push(`/folders/${item.folderId}`);
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
