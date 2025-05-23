import { SafeAreaView, StyleSheet, Text, View } from "react-native";
import React from "react";
import { useLocalSearchParams, useRouter } from "expo-router";

const FolderDetails = () => {
  const { folderId } = useLocalSearchParams();

  return (
    <SafeAreaView>
      <Text className="text-black-100">Folder {folderId}</Text>
    </SafeAreaView>
  );
};

export default FolderDetails;

const styles = StyleSheet.create({});
