import { useState } from "react";
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  Image,
  Alert,
} from "react-native";

import { icons } from "../constants";
import { router, usePathname } from "expo-router";

type SearchInputProps = {
  title?: string;
  value?: string;
  placeholder?: string;
  handleChangeText?: (text: string) => void;
  otherStyles?: object;
  [key: string]: any;
};

const SearchInput = ({
  title,
  value,
  placeholder,
  handleChangeText,
  otherStyles,
  ...props
}: SearchInputProps) => {
  const pathname = usePathname();
  const [query, setQuery] = useState("");
  return (
    <View
      className="flex-row items-center px-5 py-2 bg-black-100 
    rounded-full border-2 border-black-200 focus:border-secondary"
    >
      <TouchableOpacity
        onPress={() => {
          if (query === "")
            return Alert.alert(
              "Missing Query",
              "Please input something to search results across database"
            );

          if (pathname.startsWith("/search")) router.setParams({ query });
          else router.push(`/search/${query}`);
        }}
      >
        <Image source={icons.search} className="size-5" resizeMode="contain" />
      </TouchableOpacity>

      <TextInput
        className="text-base mt-0.5 text-white flex-1 font-pregular"
        value={query}
        placeholder="Search"
        placeholderTextColor="#CDCDE0"
        onChangeText={(e) => setQuery(e)}
      ></TextInput>
    </View>
  );
};

export default SearchInput;
