import { View, Text, TouchableOpacity, Image } from "react-native";
import React, { useRef } from "react";
import { Link, useLocalSearchParams, useRouter } from "expo-router";
import { icons } from "@/constants";
import { formatDate, formatSize } from "@/services/utils/Function";
import { File } from "@/domain/storage/File";
import { MaterialIcons } from "@expo/vector-icons";
import BottomSheet from "@gorhom/bottom-sheet";

type Props = {
  item: File;
  onMovePress: (file: File) => void;
  owner?: string;
};

const FileItemComponent = ({ item, onMovePress, owner }: Props) => {
  const router = useRouter();
  const { folderId } = useLocalSearchParams();

  const handlePress = () => {
    if (folderId) {
      router.push({
        pathname: "/folders/[folderId]/files/[fileId]",
        params: {
          folderId: folderId.toString(),
          fileId: item.fileId,
          owner: owner?.toString(),
        },
      });
    } else {
      router.push({
        pathname: "/files/[fileId]",
        params: { fileId: item.fileId },
      });
    }
  };

  return (
    <View
      style={{
        flexDirection: "row",
        alignItems: "center",
        paddingVertical: 12,
        paddingHorizontal: 16,
        borderBottomWidth: 1,
        borderBottomColor: "#eee",
      }}
    >
      <TouchableOpacity
        style={{ flex: 1, flexDirection: "row", alignItems: "center" }}
        onPress={handlePress}
        activeOpacity={0.85}
      >
        <Image
          source={
            item.contentType.startsWith("image")
              ? icons.image_icon
              : icons.document
          }
          className="w-7 h-7 mr-5"
          resizeMode="contain"
        />

        <View style={{ flex: 1 }}>
          <Text className="text-white text-xl" numberOfLines={1}>
            {item.name}
          </Text>
          <Text className="text-gray-400 text-base">
            {item.size ? `${formatSize(item.size)}, ` : ""}
            created on {formatDate(item.createdAt)}
          </Text>
        </View>
        <TouchableOpacity
          onPress={() => onMovePress(item)}
          style={{ marginLeft: 8 }}
          hitSlop={{ top: 10, bottom: 10, left: 10, right: 10 }}
        >
          <MaterialIcons name="more-vert" size={22} color="white" />
        </TouchableOpacity>
      </TouchableOpacity>
    </View>
  );
};

export default FileItemComponent;
