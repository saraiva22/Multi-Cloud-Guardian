import {
  View,
  Text,
  FlatList,
  Image,
  ImageBackground,
  TouchableOpacity,
} from "react-native";
import React, { useState } from "react";
import { icons, images } from "../constants";
import { useRouter } from "expo-router";
import EmptyState from "./EmptyState";
import { formatFolderType, formatSize } from "@/services/utils/Function";
import { MaterialIcons } from "@expo/vector-icons";

const FolderItem = ({ activeItem, setActiveItem, item }: any) => {
  const router = useRouter();
  const isActive = activeItem === item.folderId;

  const handlePress = () => {
    setActiveItem(item.folderId);
    router.push(`/folders/${item.folderId}`);
  };

  return (
    <TouchableOpacity
      activeOpacity={0.85}
      onPress={handlePress}
      className={`w-40 h-36 rounded-2xl p-3 mr-4 shadow-lg justify-between ${
        isActive ? "bg-blue-600 border-2 border-white" : "bg-blue-500"
      }`}
    >
      <View className="flex-row justify-between items-center">
        <Image
          source={images.folder}
          className="w-8 h-8"
          resizeMode="contain"
        />
        <Text className="text-white text-xs font-semibold ">
          {formatFolderType(item.type)}
        </Text>
      </View>

      <View>
        <Text
          numberOfLines={1}
          className="text-white text-base font-semibold mt-2"
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

const FolderCard = ({ folders }: any) => {
  const [activeItem, setActiveItem] = useState(folders[0]?.folderId);

  const viewableItemsChanged = ({ viewableItems }) => {
    if (viewableItems.length > 0) {
      setActiveItem(viewableItems[0].key);
    }
  };

  return (
    <FlatList
      data={folders}
      horizontal
      keyExtractor={(item) => item.folderId}
      renderItem={({ item }) => (
        <FolderItem
          activeItem={activeItem}
          setActiveItem={setActiveItem}
          item={item}
        />
      )}
      onViewableItemsChanged={viewableItemsChanged}
      viewabilityConfig={{
        itemVisiblePercentThreshold: 70,
      }}
      contentContainerStyle={{ paddingLeft: 2 }}
      ListEmptyComponent={() => (
        <Text className="text-gray-200 justify-items-center text-base mt-5">
          No folders found
        </Text>
      )}
    />
  );
};

export default FolderCard;
