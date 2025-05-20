import { StyleSheet, Text, View } from "react-native";
import React from "react";
import { useLocalSearchParams, useRouter } from "expo-router";

const Details = () => {
  const router = useRouter();
  const { fileId } = useLocalSearchParams();

  <View>
    <Text>Details `{fileId}`</Text>
  </View>;
};

export default Details;

const styles = StyleSheet.create({});
