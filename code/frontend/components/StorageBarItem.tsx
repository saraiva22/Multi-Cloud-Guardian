import { formatSize } from "@/services/utils/Function";
import { View, Text } from "react-native";

interface StorageBarItemProps {
  label: string;
  value: number;
  total: number;
  color: string;
}

const StorageBarItem = ({
  label,
  value,
  total,
  color,
}: StorageBarItemProps) => {
  const percentage = total > 0 ? value / total : 0;

  return (
    <View className="mb-6">
      <View className="flex-row items-center">
        <View
          className="w-3 h-3 rounded-full mr-2.5"
          style={{ backgroundColor: color }}
        />
        <View className="w-28 justify-center">
          <Text className="text-xl text-white font-semibold">{label}</Text>
        </View>
        <View className="flex-1 h-1 bg-zinc-200 rounded ml-2.5 overflow-hidden">
          <View
            className="h-full rounded"
            style={{
              width: `${percentage * 100}%`,
              backgroundColor: color,
            }}
          />
        </View>
      </View>
      <Text className="text-base text-gray-50 mt-1 ml-6">
        {formatSize(value)}
      </Text>
    </View>
  );
};

export default StorageBarItem;
