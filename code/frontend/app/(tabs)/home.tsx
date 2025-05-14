import { FlatList, RefreshControl, View, Text, Image } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import EmptyState from "@/components/EmptyState";
import SearchInput from "@/components/SearchInput";
import { icons, images } from "@/constants";
import { useAuthentication } from "@/context/AuthProvider";
import React, { useState } from "react";

const HomeScreen = () => {
  const { username } = useAuthentication();

  const [refreshing, setRefreshing] = useState(false);

  // const onRefresh = async () => {
  //   setRefreshing(true);
  //   // await refetch();
  //   setRefreshing(false);
  // };

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
            <View className="flex-row justify-between items-center mb-5">
              <View className="flex-row items-center space-x-10">
                <Image
                  source={icons.profile}
                  className="w-[32] h-[32] mr-2"
                  resizeMode="contain"
                />
                <Text className="text-xl font-psemibold text-white">
                  Hello, {username}
                </Text>
              </View>
              <View className="mt-1.5">
                <Image
                  source={icons.notificiation_black}
                  className="w-[24] h-[24]"
                  resizeMode="contain"
                />
              </View>
            </View>

            <SearchInput />
          </View>
        )}
        ListEmptyComponent={() => (
          <EmptyState
            title="No Files Found"
            subtitle="Be the first one to upload a file"
          />
        )}
        // refreshControl={
        //   <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
        // }
      />
    </SafeAreaView>
  );
};

export default HomeScreen;
