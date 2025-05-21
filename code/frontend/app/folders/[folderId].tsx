import { StyleSheet, Text, View } from "react-native";
import React from "react";
import { useLocalSearchParams, useRouter } from "expo-router";

const Details = () => {
  const { folderId } = useLocalSearchParams();

  <View>
    <Text className="text-bold color-black-100">Details `{folderId}`</Text>
  </View>;
};

export default Details;

const styles = StyleSheet.create({});
