import { StyleSheet, Text, View } from "react-native";
import React from "react";
import { useLocalSearchParams, useRouter } from "expo-router";

const FolderDetails = () => {
  const { folderId } = useLocalSearchParams();

  return (
    <View>
      <Text className="text-black-100">Folder {folderId}</Text>
    </View>
  );
};

export default FolderDetails;

const styles = StyleSheet.create({});
