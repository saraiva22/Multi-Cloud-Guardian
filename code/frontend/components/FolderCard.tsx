import {
  View,
  Text,
  FlatList,
  Image,
  ImageBackground,
  TouchableOpacity,
} from "react-native";
import React, { useState } from "react";
import * as Animatable from "react-native-animatable";

import { icons, images } from "../constants";
import { useRouter } from "expo-router";

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
        <Text className="text-white text-3xl font-bold bottom-safe-offset-5">
          …
        </Text>
      </View>

      {/* Middle - Folder Name */}
      <View>
        <Text
          numberOfLines={1}
          className="text-white text-base font-semibold mt-2"
        >
          {item.folderName}
        </Text>
      </View>

      <View>
        {/* Size */}
        {item.size > 0 && (
          <Text className="text-white text-xs opacity-80">
            Used: {item.size} Mb
          </Text>
        )}
        {/* Bottom - File count */}
        {item.numberFile > 0 && (
          <Text className="text-white text-xs opacity-80">
            {item.numberFile} files
          </Text>
        )}
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
      contentOffset={{ x: 170 }}
    />
  );
};

export default FolderCard;
