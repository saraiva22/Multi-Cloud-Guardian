import { FlatList, RefreshControl, View, Text, Image } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import EmptyState from "@/components/EmptyState";
import SearchInput from "@/components/SearchInput";
import { icons } from "@/constants";
import { useAuthentication } from "@/context/AuthProvider";
import React, { useState } from "react";

const Files = () => {
  const { username } = useAuthentication();

  return (
    <SafeAreaView className="bg-primary h-full">
      <FlatList
        data={[]}
        keyExtractor={(item, index) => String(item || index)}
        renderItem={({ item }) => (
          <View>
            <Text>{item}</Text>
          </View>
        )}
        ListHeaderComponent={() => (
          <View className="my-6 px-4 space-y-6">
            <View className="justify-between items-start flex-row mb-6">
              <View>
                <Text className="text-2xl font-psemibold text-white">
                  Files
                </Text>
              </View>

              <View className="mt-1.5">
                <Image
                  source={icons.filter_white}
                  className="w-[18px] h-[20px]"
                  resizeMode="contain"
                />
              </View>
            </View>
            <SearchInput />
          </View>
        )}
        ListEmptyComponent={() => (
          <EmptyState
            title="No Files"
            subtitle="Be the first one to upload a file"
          />
        )}
      />
    </SafeAreaView>
  );
};

export default Files;
