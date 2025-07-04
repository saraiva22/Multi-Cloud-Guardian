import { View, TextInput, Image } from "react-native";

import { icons } from "../constants";

type SearchInputProps = {
  value: string;
  placeholder?: string;
  onChangeText: (text: string) => void;
};

const SearchInput = ({
  value,
  placeholder,
  onChangeText,
}: SearchInputProps) => {
  return (
    <View
      className="flex-row items-center px-5 py-2 bg-black-100 
    rounded-full border-2 border-black-200 focus:border-secondary"
    >
      <Image source={icons.search} className="size-5" resizeMode="contain" />

      <TextInput
        className="text-base ml-2 text-white flex-1 font-pregular"
        value={value}
        placeholder={`Search ${placeholder ? placeholder : ""}`}
        placeholderTextColor="#CDCDE0"
        onChangeText={onChangeText}
      ></TextInput>
    </View>
  );
};

export default SearchInput;
