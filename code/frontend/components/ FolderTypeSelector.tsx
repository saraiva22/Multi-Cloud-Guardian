import React from "react";
import { View, Text, TouchableOpacity, StyleSheet } from "react-native";
import { FolderType } from "@/domain/storage/FolderType";

type FolderTypeSelectorProps = {
  title: string;
  value: FolderType;
  onChange: (value: FolderType) => void;
};

const FolderTypeSelector = ({
  title,
  value,
  onChange,
}: FolderTypeSelectorProps) => {
  const renderOption = (type: FolderType, text: string) => (
    <TouchableOpacity
      key={type}
      style={styles.option}
      onPress={() => onChange(type)}
      accessibilityRole="radio"
      accessibilityState={{ selected: value === type }}
      activeOpacity={0.7}
    >
      <View style={[styles.radio, value === type && styles.selectedRadio]}>
        {value === type && <View style={styles.radioDot} />}
      </View>
      <Text style={styles.label}>{text}</Text>
    </TouchableOpacity>
  );

  return (
    <View style={styles.container}>
      <Text className="text-base text-gray-100 font-pmedium mb-5">{title}</Text>
      <View style={styles.optionsRow}>
        {renderOption(FolderType.PRIVATE, "Private")}
        {renderOption(FolderType.SHARED, "Shared")}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    marginVertical: 2,
  },
  optionsRow: {
    flexDirection: "row",
    alignItems: "center",
  },
  option: {
    flexDirection: "row",
    alignItems: "center",
    marginRight: 32,
  },
  radio: {
    width: 20,
    height: 20,
    borderRadius: 10,
    borderWidth: 2,
    borderColor: "#888",
    backgroundColor: "#23232a",
    justifyContent: "center",
    alignItems: "center",
    marginRight: 8,
  },
  selectedRadio: {
    borderColor: "#4ADE80",
  },
  radioDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: "#4ADE80",
  },
  label: {
    fontSize: 15,
    color: "#fff",
    fontWeight: "500",
  },
});

export default FolderTypeSelector;
