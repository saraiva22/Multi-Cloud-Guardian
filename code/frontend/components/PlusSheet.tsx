import React, { useCallback, forwardRef } from "react";
import { View, Text, StyleSheet, TouchableOpacity, Image } from "react-native";
import BottomSheet, { BottomSheetView } from "@gorhom/bottom-sheet";
import { icons } from "@/constants";
import { router } from "expo-router";

// Action
const actions = [
  {
    icon: icons.files,
    label: "Upload File",
    onPress: () => router.push("/(modals)/create-file"),
  },
  {
    icon: icons.folders,
    label: "Create Folder",
    onPress: () => router.push("/(modals)/create-folder"),
  },
];

const PlusSheet = forwardRef<BottomSheet, any>((_, ref) => {
  const handleAction = (action: () => void) => {
    if (ref && typeof ref !== "function" && ref.current) {
      ref.current.close();
    }

    // Small delay to allow the bottom sheet to close smoothly before navigation
    setTimeout(() => {
      action();
    }, 200);
  };

  return (
    <BottomSheet
      ref={ref}
      index={-1} // Start closed (hidden state)
      enablePanDownToClose={true}
      snapPoints={["25%"]}
      backgroundStyle={styles.sheetBackground}
      handleIndicatorStyle={styles.handleIndicator}
    >
      <BottomSheetView style={styles.contentContainer}>
        <Text style={styles.title}>What do you want to create?</Text>
        <View style={styles.actionsRow}>
          {actions.map((action, idx) => (
            <TouchableOpacity
              key={idx}
              style={styles.actionBtn}
              onPress={() => handleAction(action.onPress)}
              activeOpacity={0.7}
            >
              <View style={styles.iconCircle}>
                <Image source={action.icon} style={styles.icon} />
              </View>
              <Text style={styles.actionLabel}>{action.label}</Text>
            </TouchableOpacity>
          ))}
        </View>
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
    alignItems: "center",
    justifyContent: "center",
  },
  title: {
    color: "#fff",
    fontSize: 18,
    fontWeight: "bold",
    marginBottom: 24,
  },
  actionsRow: {
    flexDirection: "row",
    justifyContent: "center",
    gap: 32,
  },
  actionBtn: {
    alignItems: "center",
    marginHorizontal: 16,
  },
  iconCircle: {
    backgroundColor: "#FFA001",
    borderRadius: 32,
    width: 56,
    height: 56,
    justifyContent: "center",
    alignItems: "center",
    marginBottom: 8,
  },
  icon: {
    width: 28,
    height: 28,
    tintColor: "white",
  },
  actionLabel: {
    color: "#fff",
    fontSize: 14,
    fontWeight: "600",
    textAlign: "center",
  },
});

export default PlusSheet;
