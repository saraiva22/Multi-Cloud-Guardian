import { View, Text, TouchableOpacity, Image } from "react-native";
import React from "react";
import { Link, useRouter } from "expo-router";
import { icons } from "@/constants";
import {
  formatDate,
  formatFolderType,
  formatSize,
} from "@/services/utils/Function";
import { Folder } from "@/domain/storage/Folder";
import { MaterialCommunityIcons, MaterialIcons } from "@expo/vector-icons";

type Props = {
  item: Folder;
  onPress?: (folderId: number) => void;
  
  selectedFolderId?: number | null;
};

const FolderItemComponent = ({ item, onPress, selectedFolderId }: Props) => {
  const router = useRouter();
  const isSelected = selectedFolderId === item.folderId;

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
      className={`flex-row items-center px-4 py-3 border-b border-b-gray-200 rounded-lg ${
        isSelected ? "bg-gray-700" : ""
      } space-x-4`}
    >
      <Image
        source={icons.folder}
        className="w-8 h-8 mr-5"
        resizeMode="contain"
      />

      <View style={{ flex: 1 }}>
        <Text style={{ color: "white", fontSize: 18 }} numberOfLines={1}>
          {item.folderName}
        </Text>
        <Text style={{ color: "#9CA3AF", fontSize: 14 }}>
          {item.size ? `${formatSize(item.size)}, ` : ""}
          Modified {formatDate(item.createdAt)} ({formatFolderType(item.type)})
        </Text>
      </View>
      {selectedFolderId !== null && (
        <MaterialCommunityIcons
          className="mt-2"
          name={isSelected ? "check-circle" : "checkbox-blank-circle-outline"}
          size={24}
          color={isSelected ? "#FFA001" : "#888"}
          style={{ marginRight: 15 }}
        />
      )}
    </TouchableOpacity>
  );
};

export default FolderItemComponent;
