import { View, Text, Image, Platform, useColorScheme } from "react-native";
import { Tabs } from "expo-router";
import { icons } from "../../constants";
import { useSafeAreaInsets } from "react-native-safe-area-context";
type Icon = {
  icon: any;
  color: string;
  name: string;
  focused: boolean;
};

const AvailableRoutes = [
  { key: "home", title: "Home", icon: icons.home },
  { key: "files", title: "Files", icon: icons.files },
  { key: "folders", title: "Folders", icon: icons.folders },
  { key: "settings", title: "Settings", icon: icons.settings },
];

const TabIcon = ({ icon, color, name, focused }: Icon) => {
  return (
    <View className="flex items-center justify-center gap-2">
      <Image
        source={icon}
        resizeMode="contain"
        tintColor={color}
        className="w-[20px] h-[20px]"
      />
      <Text
        ellipsizeMode="tail"
        style={{
          color,
          fontSize: 10,
          fontFamily: focused ? "Poppins-SemiBold" : "Poppins-Regular",
        }}
      >
        {name}
      </Text>
    </View>
  );
};

function TabsLayout() {
  // const colorScheme = useColorScheme();
  // console.log(colorScheme);
  // const tabBarBackground = colorScheme !== "dark" ? "#232533" : "#fff";
  // const tabBarBorder = colorScheme !== "dark" ? "#2C2C38" : "#eee";
  const insets = useSafeAreaInsets();
  if (Platform.OS === "web") return null;

  return (
    <Tabs
      screenOptions={{
        tabBarShowLabel: false,
        tabBarActiveTintColor: "#FFA001",
        tabBarInactiveTintColor: "#CDCDE0",
        tabBarStyle: {
          position: "absolute",
          left: 0,
          right: 0,
          bottom: 0,
          height: 78 + insets.bottom,
          paddingTop: 15,
          paddingBottom: insets.bottom,
          backgroundColor: "#1E1E2D",
          borderTopWidth: 0,
        },

        tabBarItemStyle: {
          width: "auto",
          paddingHorizontal: 8,
        },
      }}
    >
      {AvailableRoutes.map((route, index) => (
        <Tabs.Screen
          key={route.key}
          name={route.key}
          options={{
            headerShown: false,

            tabBarItemStyle: { marginLeft: "auto" },

            tabBarIcon: ({ color, focused }) => (
              <TabIcon
                icon={route.icon}
                color={color}
                name={route.title}
                focused={focused}
              />
            ),
          }}
        />
      ))}
    </Tabs>
  );
}

export default TabsLayout;
