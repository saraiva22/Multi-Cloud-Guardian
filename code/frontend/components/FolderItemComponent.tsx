import { View, Text, TouchableOpacity, Image } from "react-native";
import React from "react";
import { Link, useRouter } from "expo-router";
import { icons } from "@/constants";
import { formatDate, formatSize } from "@/services/utils/Function";
import { FolderType } from "@/domain/storage/FolderType";

type Props = {
  item: FolderType;
  onPress?: (folderId: number) => void;
};

const FolderItemComponent = ({ item, onPress }: Props) => {
  const router = useRouter();

  const handlePress = () => {
    if (onPress) {
      onPress(item.folderId);
    } else {
      router.push(`/folders/${item.folderId}`);
    }
  };

  return (
    <TouchableOpacity
      onPress={handlePress}
      style={{
        flexDirection: "row",
        alignItems: "center",
        paddingVertical: 12,
        paddingHorizontal: 16,
        borderBottomWidth: 1,
        borderBottomColor: "#eee",
      }}
    >
      <Image
        source={icons.folder}
        className="w-7 h-7 mr-5"
        resizeMode="contain"
      />

      <View style={{ flex: 1 }}>
        <Text className="text-white text-xl" numberOfLines={1}>
          {item.folderName}
        </Text>
        <Text className="text-gray-400 text-base">
          {item.size ? `${formatSize(item.size)}, ` : ""}
          Modified {formatDate(item.createdAt)}
        </Text>
      </View>
      <TouchableOpacity>
        <Image
          source={icons.dots_vertical}
          style={{
            width: 24,
            height: 24,
            tintColor: "#666",
          }}
          resizeMode="contain"
        />
      </TouchableOpacity>
    </TouchableOpacity>
  );
};

export default FolderItemComponent;
