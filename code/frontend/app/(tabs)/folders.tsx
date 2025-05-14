import { FlatList, RefreshControl, View, Text, Image } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import EmptyState from "@/components/EmptyState";
import SearchInput from "@/components/SearchInput";
import { icons } from "@/constants";
import { useAuthentication } from "@/context/AuthProvider";
import React, { useState } from "react";

const FoldersScreen = () => {
  const { username } = useAuthentication();

  return (
    <SafeAreaView className="bg-primary h-full">
      <FlatList
        data={[]}
        contentContainerStyle={{ flexGrow: 1 }}
        keyExtractor={(item, index) => String(item || index)}
        renderItem={({ item }) => (
          <View>
            <Text>{item}</Text>
          </View>
        )}
        ListHeaderComponent={() => (
          <View className="my-6 px-4 space-y-6">
            <View className="justify-between items-start flex-row mb-6">
              <Text className="text-2xl font-psemibold text-white">
                Folders
              </Text>
              <View className="mt-1.5 flex-row gap-2">
                <Image
                  source={icons.filter_white}
                  className="w-[18px] h-[20px]"
                />
                <Image source={icons.org_white} className="w-[18px] h-[20px]" />
              </View>
            </View>
            <SearchInput />
          </View>
        )}
        ListEmptyComponent={() => (
          <EmptyState
            title="No folders found"
            subtitle="Try creating a new folder or adjust your search"
          />
        )}
      />
    </SafeAreaView>
  );
};

export default FoldersScreen;
