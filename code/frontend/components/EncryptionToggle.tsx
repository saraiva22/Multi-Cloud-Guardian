import React from "react";
import { View, Text, Switch } from "react-native";

type EncryptionToggleProps = {
  value: boolean;
  onChange: (value: boolean) => void;
  label?: string;
  otherStyles?: string;
};

const EncryptionToggle = ({
  value,
  onChange,
  label = "Do you want your file to be encrypted?",
  otherStyles = "",
}: EncryptionToggleProps) => {
  return (
    <View className={`flex-row justify-between items-center ${otherStyles}`}>
      <Text className="text-base text-gray-100 font-pmedium">{label}</Text>
      <Switch
        value={value}
        onValueChange={onChange}
        trackColor={{ false: "#ccc", true: "#4ADE80" }}
        thumbColor={value ? "#15803D" : "#f4f3f4"}
      />
    </View>
  );
};

export default EncryptionToggle;
