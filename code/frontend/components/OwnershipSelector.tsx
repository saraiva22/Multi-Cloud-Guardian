import React, { forwardRef, useState } from "react";
import { View, Text, TouchableOpacity, StyleSheet } from "react-native";
import BottomSheet, { BottomSheetView } from "@gorhom/bottom-sheet";
import { OwnershipFilter } from "@/domain/storage/OwnershipFilter";

export type OwnershipOption = {
  label: string;
  value: OwnershipFilter;
};

export const ownershipOptions: OwnershipOption[] = [
  { label: "Member", value: OwnershipFilter.MEMBER },
  { label: "Owner", value: OwnershipFilter.OWNER },
];

interface Props {
  onOwnershipChange: (option: OwnershipOption) => void;
}

const OwnershipSelector = forwardRef<BottomSheet, Props>(
  ({ onOwnershipChange }, ref) => {
    const [selectedOption, setSelectedOption] = useState<OwnershipOption>(
      ownershipOptions[0]
    );

    const handleAction = (option: OwnershipOption) => {
      setSelectedOption(option);
      if (ref && typeof ref !== "function" && ref.current) {
        ref.current.close();
      }
      setTimeout(() => {
        onOwnershipChange(option);
      }, 200);
    };

    return (
      <BottomSheet
        ref={ref}
        index={-1}
        enablePanDownToClose={true}
        snapPoints={["40%"]}
        backgroundStyle={styles.sheetBackground}
        handleIndicatorStyle={styles.handleIndicator}
      >
        <BottomSheetView style={styles.contentContainer}>
          <Text style={styles.title}>Filter Ownership</Text>
          {ownershipOptions.map((option, idx) => {
            const isSelected = selectedOption.value === option.value;
            return (
              <TouchableOpacity
                key={idx}
                style={styles.optionContainer}
                onPress={() => handleAction(option)}
                activeOpacity={0.7}
              >
                <View
                  style={[
                    styles.radioCircle,
                    isSelected && styles.selectedRadio,
                  ]}
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
  }
);

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

export default OwnershipSelector;
