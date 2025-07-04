import FileDetailsScreen from "@/components/FileDetailsScreen";
import { useAuthentication } from "@/context/AuthProvider";
import {
  deleteFileInFolder,
  downloadFileInFolder,
  getFileInFolder,
} from "@/services/storage/StorageService";
import { useLocalSearchParams } from "expo-router";

const FileDetailsInFolder = () => {
  const { folderId, fileId, owner } = useLocalSearchParams();
  const { token } = useAuthentication();
  console.log("OWNER", owner);

  return (
    <FileDetailsScreen
      fileId={fileId.toString()}
      owner={owner.toString()}
      getFileFunc={(id) => getFileInFolder(folderId.toString(), id, token)}
      deleteFunc={(id) => deleteFileInFolder(folderId.toString(), id, token)}
      downloadFunc={(id) =>
        downloadFileInFolder(folderId.toString(), id, token)
      }
    />
  );
};

export default FileDetailsInFolder;
