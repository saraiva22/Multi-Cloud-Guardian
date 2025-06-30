import { colors, icons } from "@/constants";
import { Invite } from "@/domain/storage/Invite";
import { InviteStatusType } from "@/domain/storage/InviteStatusType";
import { formatInviteStatus } from "@/services/utils/Function";
import { TouchableOpacity, View, Text, Image } from "react-native";

type Props = {
  isReceived: boolean;
  item: Invite;
  onPress?: (status: InviteStatusType) => void;
};

const statusColors = {
  PENDING: colors.PENDING,
  ACCEPT: colors.ACCEPT,
  REJECT: colors.REJECT,
};

const InviteItemComponente = ({ isReceived, item, onPress }: Props) => {
  const statusColor = statusColors[item.status];

  const isPending = item.status === InviteStatusType.PENDING;

  return (
    <View className="w-[95%] self-center my-3">
      <View className="bg-[#292a3a] rounded-3xl flex-row items-center px-5 py-4 shadow-md">
        <View className="w-12 h-12 rounded-full bg-gray-500 items-center justify-center mr-4">
          <Text className="text-white font-bold text-xl">
            {item.user.username[0]?.toUpperCase()}
          </Text>
        </View>

        <View className="flex-1">
          <Text className="text-white font-semibold text-base">
            Invitation from:{" "}
            <Text className="font-bold">{item.user.username}</Text>
          </Text>
          <Text className="text-gray-300 text-base mt-0.5">
            Folder: <Text className="text-white">{item.folderName}</Text>
          </Text>
          <Text className="mt-1 text-base" style={{ color: statusColor }}>
            Status: {formatInviteStatus(item.status)}
          </Text>
        </View>

        {isReceived && isPending && onPress && (
          <View className="flex-row ml-3">
            <TouchableOpacity
              onPress={() => onPress(InviteStatusType.ACCEPT)}
              className="w-10 h-10 rounded-full bg-green-500 items-center justify-center mx-1"
              activeOpacity={0.7}
            >
              <Image
                source={icons.accept}
                style={{ width: 24, height: 24, tintColor: "#fff" }}
              />
            </TouchableOpacity>

            <TouchableOpacity
              onPress={() => onPress(InviteStatusType.REJECT)}
              className="w-10 h-10 rounded-full bg-red-500 items-center justify-center mx-1"
              activeOpacity={0.7}
            >
              <Image
                source={icons.reject}
                style={{ width: 24, height: 24, tintColor: "#fff" }}
              />
            </TouchableOpacity>
          </View>
        )}
      </View>
      {!isPending && (
        <View
          className="absolute inset-0 bg-transparent"
          pointerEvents="none"
        />
      )}
    </View>
  );
};

export default InviteItemComponente;
