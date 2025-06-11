import FileDetailsScreen from "@/components/FileDetailsScreen";
import {
  deleteFileInFolder,
  downloadFileInFolder,
  getFileInFolder,
} from "@/services/storage/StorageService";
import { useLocalSearchParams } from "expo-router";

const FileDetailsInFolder = () => {
  const { folderId, fileId } = useLocalSearchParams();

  return (
    <FileDetailsScreen
      fileId={fileId.toString()}
      getFileFunc={(id) => getFileInFolder(folderId.toString(), id)}
      deleteFunc={(id) => deleteFileInFolder(folderId.toString(), id)}
      downloadFunc={(id) => downloadFileInFolder(folderId.toString(), id)}
    />
  );
};

export default FileDetailsInFolder;
