import React, { forwardRef, useState } from "react";
import { View, Text, TouchableOpacity, StyleSheet } from "react-native";
import BottomSheet, { BottomSheetView } from "@gorhom/bottom-sheet";

export type SortOption = {
  label: string;
  sortBy: string;
};

export const sortOptions: SortOption[] = [
  { label: "Last modified ↓", sortBy: "created_desc" },
  { label: "Last modified ↑", sortBy: "created_asc" },
  { label: "Name A - Z", sortBy: "name_asc" },
  { label: "Name Z - A", sortBy: "name_desc" },
  { label: "Size ↓", sortBy: "size_desc" },
  { label: "Size ↑", sortBy: "size_asc" },
];

interface Props {
  onSortChange: (option: SortOption) => void;
}

const SortSelector = forwardRef<BottomSheet, Props>(({ onSortChange }, ref) => {
  const [selectedSort, setSelectedSort] = useState<SortOption>(sortOptions[0]);
  const handleAction = (option: SortOption) => {
    setSelectedSort(option);

    if (ref && typeof ref !== "function" && ref.current) {
      ref.current.close();
    }

    setTimeout(() => {
      onSortChange(option);
    }, 200);
  };

  return (
    <BottomSheet
      ref={ref}
      index={-1}
      enablePanDownToClose={true}
      snapPoints={["55%"]}
      backgroundStyle={styles.sheetBackground}
      handleIndicatorStyle={styles.handleIndicator}
    >
      <BottomSheetView style={styles.contentContainer}>
        <Text style={styles.title}>Sort</Text>
        {sortOptions.map((option, idx) => {
          const isSelected = selectedSort.sortBy === option.sortBy;
          return (
            <TouchableOpacity
              key={idx}
              style={styles.optionContainer}
              onPress={() => handleAction(option)}
              activeOpacity={0.7}
            >
              <View
                style={[styles.radioCircle, isSelected && styles.selectedRadio]}
              >
                {isSelected && <View style={styles.selectedDot} />}
              </View>
              <Text style={styles.optionLabel}>{option.label}</Text>
            </TouchableOpacity>
          );
        })}
      </BottomSheetView>
    </BottomSheet>
  );
});

const styles = StyleSheet.create({
  sheetBackground: {
    backgroundColor: "#232533",
  },
  handleIndicator: {
    backgroundColor: "#FFA001",
    width: 40,
    height: 5,
    borderRadius: 5,
  },
  contentContainer: {
    flex: 1,
    padding: 24,
  },
  title: {
    color: "#fff",
    fontSize: 18,
    fontWeight: "bold",
    marginBottom: 16,
  },
  optionContainer: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: "#444",
  },
  radioCircle: {
    height: 20,
    width: 20,
    borderRadius: 10,
    borderWidth: 2,
    borderColor: "#FFA001",
    alignItems: "center",
    justifyContent: "center",
    marginRight: 12,
  },
  selectedRadio: {
    borderColor: "#FFA001",
  },
  selectedDot: {
    height: 10,
    width: 10,
    borderRadius: 5,
    backgroundColor: "#FFA001",
  },
  optionLabel: {
    color: "#fff",
    fontSize: 16,
  },
});

export default SortSelector;
