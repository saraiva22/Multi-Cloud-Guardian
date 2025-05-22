import { View, Text, TouchableOpacity, Image } from "react-native";
import React from "react";
import { Link, useRouter } from "expo-router";
import { FileOutputModel } from "@/services/storage/model/FileOutputModel";
import { icons } from "@/constants";

const months = [
  "Jan",
  "Feb",
  "Mar",
  "Apr",
  "May",
  "Jun",
  "Jul",
  "Aug",
  "Sep",
  "Oct",
  "Nov",
  "Dec",
];

const formatSize = (bytes?: number) => {
  if (!bytes) return "";
  if (bytes >= 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} Mb`;
  if (bytes >= 1024) return `${(bytes / 1024).toFixed(1)} Kb`;
  return `${bytes} B`;
};

const formatDate = (date: number) => {
  const dateObj = new Date(date * 1000);
  return `${
    months[dateObj.getMonth()]
  } ${dateObj.getDate()}, ${dateObj.getFullYear()}`;
};
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
