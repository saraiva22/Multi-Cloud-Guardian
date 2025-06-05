import { View, Text, TouchableOpacity, Image } from "react-native";
import React from "react";
import { Link, useRouter } from "expo-router";
import { icons } from "@/constants";
import { formatDate, formatSize } from "@/services/utils/Function";
import { FileType } from "@/domain/storage/FileType";

const FileItemComponent = ({ item }: { item: FileType }) => {
  const router = useRouter();

  return (
    <Link href={`/files/${item.fileId}`} asChild>
      <TouchableOpacity
        onPress={() => router.push(`/files/${item.fileId}`)}
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
    </Link>
  );
};

export default FileItemComponent;
