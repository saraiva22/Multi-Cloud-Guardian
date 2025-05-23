import { View, Text, TouchableOpacity, Image } from "react-native";
import React from "react";
import { Link, useRouter } from "expo-router";
import { FileOutputModel } from "@/services/storage/model/FileOutputModel";
import { icons } from "@/constants";
import { formatDate, formatSize } from "@/services/utils/Function";


const FileItemComponent = ({ item }: { item: FileOutputModel }) => {
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
          source={icons.image_icon}
          style={{
            width: 32,
            height: 32,
            borderRadius: 6,
            marginRight: 12,
          }}
        />

        <View style={{ flex: 1 }}>
          <Text className="text-white" numberOfLines={1}>
            {item.name}
          </Text>
          <Text className="text-gray-400 text-xs">
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
