import React from "react";
import { View, Text, Image, FlatList, StyleSheet } from "react-native";
import icons from "@/constants/icons";
import { UserInfo } from "@/domain/user/UserInfo";

const MemberCard = ({ members }: { members: Array<UserInfo> }) => {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>
        {members.length} Member{members.length !== 1 ? "s" : ""} in this Folder
      </Text>
      <FlatList
        data={members}
        keyExtractor={(item) => item.username}
        horizontal
        renderItem={({ item }) => (
          <View style={styles.card}>
            <View style={styles.avatarWrapper}>
              <Image
                source={icons.profile}
                style={styles.profileIcon}
                resizeMode="cover"
              />
            </View>
            <Text style={styles.username} numberOfLines={1}>
              {item.username}
            </Text>
          </View>
        )}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    paddingHorizontal: 16,
    paddingTop: 24,
    flex: 1,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    shadowOffset: { width: 0, height: -2 },
    shadowOpacity: 0.15,
    shadowRadius: 8,
    elevation: 8,
  },
  title: {
    color: "#FFF",
    fontSize: 20,
    fontWeight: "bold",
    textAlign: "center",
    marginBottom: 20,
    letterSpacing: 0.5,
  },
  listContent: {
    paddingBottom: 16,
    alignItems: "center",
  },
  card: {
    backgroundColor: "#23232b",
    borderRadius: 18,
    alignItems: "center",
    margin: 8,
    paddingVertical: 14,
    paddingHorizontal: 10,
    width: 100,
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.14,
    shadowRadius: 10,
    elevation: 6,
  },
  avatarWrapper: {
    backgroundColor: "#31313b",
    borderRadius: 40,
    padding: 4,
    marginBottom: 10,
    borderWidth: 2,
    borderColor: "#3b82f6",
    shadowColor: "#3b82f6",
    shadowOpacity: 0.2,
    shadowRadius: 8,
    elevation: 4,
  },
  profileIcon: {
    width: 56,
    height: 56,
    borderRadius: 28,
  },
  username: {
    fontSize: 11,
    fontWeight: "500",
    color: "#e5e7eb",
    textAlign: "center",
    marginTop: 2,
    maxWidth: 80,
  },
});

export default MemberCard;
