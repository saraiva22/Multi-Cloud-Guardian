import { View, Text, Image, Platform } from "react-native";
import { Tabs } from "expo-router";
import { icons } from "../../constants";

type Icon = {
  icon: any;
  color: string;
  name: string;
  focused: boolean;
};

const OptionsTabs = {
  home: "Home",
  files: "Files",
  folders: "Folders",
  settings: "Settings",
};

const TabIcon = ({ icon, color, name, focused }: Icon) => {
  return (
    <View className="flex items.center justify-center gap-2">
      <Image
        source={icon}
        resizeMode="contain"
        tintColor={color}
        className="w-6 h-6"
      />
      <Text
        className={`${focused ? "font-psemibold" : "font-pregular"} text-xs`}
        style={{ color: color }}
      >
        {name}
      </Text>
    </View>
  );
};

function TabsLayout() {
  if (Platform.OS !== "web") {
    return (
      <>
        <Tabs
          screenOptions={{
            tabBarShowLabel: false,
            tabBarActiveTintColor: "#FFA001",
            tabBarInactiveTintColor: "#CDCDE0",
            tabBarStyle: {
              backgroundColor: "#161622",
              borderTopWidth: 1,
              borderTopColor: "#232533",
              height: 84,
            },
          }}
        >
          <Tabs.Screen
            name="home"
            options={{
              title: OptionsTabs.home,
              headerShown: false,
              tabBarIcon: ({ color, focused }) => (
                <TabIcon
                  icon={icons.home}
                  color={color}
                  name={OptionsTabs.home}
                  focused={focused}
                />
              ),
            }}
          />
          <Tabs.Screen
            name="files"
            options={{
              title: OptionsTabs.files,
              headerShown: false,
              tabBarIcon: ({ color, focused }) => (
                <TabIcon
                  icon={icons.files}
                  color={color}
                  name={OptionsTabs.files}
                  focused={focused}
                />
              ),
            }}
          />

          <Tabs.Screen
            name="folders"
            options={{
              title: OptionsTabs.folders,
              headerShown: false,
              tabBarIcon: ({ color, focused }) => (
                <TabIcon
                  icon={icons.folders}
                  color={color}
                  name={OptionsTabs.folders}
                  focused={focused}
                />
              ),
            }}
          />
          <Tabs.Screen
            name="settings"
            options={{
              title: OptionsTabs.settings,
              headerShown: false,
              tabBarIcon: ({ color, focused }) => (
                <TabIcon
                  icon={icons.settings}
                  color={color}
                  name={OptionsTabs.settings}
                  focused={focused}
                />
              ),
            }}
          />
        </Tabs>
      </>
    );
  }
}

export default TabsLayout;
