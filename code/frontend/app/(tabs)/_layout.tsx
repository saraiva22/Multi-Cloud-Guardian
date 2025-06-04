import { View, Text, Image, TouchableOpacity } from "react-native";
import { Tabs } from "expo-router";
import { icons } from "../../constants";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import BottomSheet from "@gorhom/bottom-sheet";
import PlusSheet from "@/components/PlusSheet";
import { useRef } from "react";
import { GestureHandlerRootView } from "react-native-gesture-handler";

const AvailableRoutes = [
  { key: "home", title: "Home", icon: icons.home },
  { key: "files", title: "Files", icon: icons.files },
  { key: "plus", title: "Plus", icon: icons.plus },
  { key: "folders", title: "Folders", icon: icons.folders },
  { key: "profile", title: "Profile", icon: icons.profileTab },
];

type Icon = {
  icon: any;
  color: string;
  name: string;
  focused: boolean;
};

const TabIcon = ({ icon, color, name, focused }: Icon) => {
  return (
    <View className="flex flex- w-full items-center justify-center gap-2 h-full">
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
          fontSize: 8,
          fontFamily: focused ? "Poppins-Bold" : "Poppins-Regular",
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
  const bottomSheetRef = useRef<BottomSheet>(null);

  const openBottomSheet = () => {
    bottomSheetRef.current?.expand();
  };

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <Tabs
        screenOptions={{
          tabBarShowLabel: false,
          tabBarActiveTintColor: "#FFA001",
          tabBarInactiveTintColor: "#CDCDE0",
          tabBarItemStyle: {
            width: "100%",
            height: "100%",
            justifyContent: "center",
            alignItems: "center",
          },
          tabBarStyle: {
            backgroundColor: "#1E1E2D",
            position: "absolute",
            left: 0,
            right: 0,
            bottom: 0,
            height: 78 + insets.bottom,
            paddingTop: 15,
            paddingBottom: insets.bottom,
            borderTopWidth: 0,
          },
        }}
        backBehavior="history"
      >
        {AvailableRoutes.map((route) => {
          const isPlus = route.key === "plus";
          if (isPlus) {
            return (
              <Tabs.Screen
                key={route.key}
                name={route.key}
                options={{
                  headerShown: false,
                  tabBarButton: (props) => (
                    <TouchableOpacity
                      {...props}
                      onPress={openBottomSheet}
                      style={{
                        top: -45,
                        justifyContent: "center",
                        alignItems: "center",
                        ...props.style,
                      }}
                    >
                      <View
                        style={{
                          width: 60,
                          height: 60,
                          borderRadius: 35,
                          backgroundColor: "#FFA001",
                          justifyContent: "center",
                          alignItems: "center",
                        }}
                      >
                        <Image
                          source={route.icon}
                          resizeMode="contain"
                          style={{
                            width: 24,
                            height: 24,
                            tintColor: "white",
                          }}
                        />
                      </View>
                    </TouchableOpacity>
                  ),
                }}
              />
            );
          }
          return (
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
          );
        })}
      </Tabs>
      <PlusSheet ref={bottomSheetRef} />
    </GestureHandlerRootView>
  );
}

export default TabsLayout;
