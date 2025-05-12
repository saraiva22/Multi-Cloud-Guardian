import { Stack } from "expo-router";

function ModalLayout() {
  return (
    <Stack screenOptions={{ presentation: "modal", headerShown: false }} />
  );
}

export default ModalLayout;
