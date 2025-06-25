import {
  LocationType,
  LocationTypeLabel,
} from "@/domain/preferences/LocationType";
import { PerformanceType, CostTypeLabel } from "@/domain/preferences/CostType";
import Slider from "@react-native-community/slider";
import { View, Text, Image } from "react-native";

type State = {
  label: LocationType | PerformanceType;
  icon: any;
};

type Props = {
  title: string;
  value: number;
  handleChange: (value: number) => void;
  state: State[];
  otherStyles?: string;
};

const SliderState = ({
  title,
  value,
  handleChange,
  state,
  otherStyles,
}: Props) => {
  const getDisplayLabel = (stateItem: State) => {
    if (title === "Location") {
      return LocationTypeLabel[stateItem.label as LocationType];
    } else {
      return CostTypeLabel[stateItem.label as PerformanceType];
    }
  };
  return (
    <View className={`space-y-2 ${otherStyles}`}>
      <Text className="text-base text-gray-100 font-pmedium mb-1">
        {`${title}:`} {value !== undefined && getDisplayLabel(state[value])}
      </Text>

      <View className="w-full ">
        <Slider
          value={value}
          onValueChange={handleChange}
          minimumValue={0}
          maximumValue={state.length - 1}
          step={1}
          minimumTrackTintColor="#4F46E5"
          maximumTrackTintColor="#3F3F46"
          thumbTintColor="#4F46E5"
        />

        <View className="flex-row justify-between mt-3">
          {state.map((state, index) => (
            <View
              key={index}
              className={`items-center ${
                index === value ? "opacity-100" : "opacity-50"
              }`}
            >
              <Image source={state.icon} className="w-[48px] h-[48px]" />
            </View>
          ))}
        </View>
      </View>
    </View>
  );
};

export default SliderState;
