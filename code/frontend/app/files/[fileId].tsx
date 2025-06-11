import React from "react";
import { useLocalSearchParams } from "expo-router";
import {
  deleteFile,
  downloadFile,
  getFile,
} from "@/services/storage/StorageService";

import FileDetailsScreen from "@/components/FileDetailsScreen";

const FileDetails = () => {
  const { fileId } = useLocalSearchParams();

  return (
    <FileDetailsScreen
      fileId={fileId.toString()}
      getFileFunc={getFile}
      deleteFunc={deleteFile}
      downloadFunc={downloadFile}
    />
  );
};

export default FileDetails;
