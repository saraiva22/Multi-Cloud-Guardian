import { StyleSheet, Text, View } from "react-native";
import React, { useEffect, useState } from "react";
import { useLocalSearchParams, useRouter } from "expo-router";
import { getFile } from "@/services/storage/StorageService";
import { FileOutputModel } from "@/services/storage/model/FileOutputModel";

const FileInfo = ({ fileId, size, createdAt, encryption }: FileOutputModel) => (
  <View>
    <Text className="text-black-100">File Name: {fileId}</Text>
    <Text className="text-black-100">File Size: {size} bytes</Text>
    <Text className="text-black-100">
      Created At: {new Date(createdAt).toLocaleString()}
    </Text>
    <Text className="text-black-100">
      Encrypted: {encryption ? "Yes" : "No"}
    </Text>
  </View>
);

const FileDetails = () => {
  const { fileId } = useLocalSearchParams();
  const [details, setDetails] = useState<FileOutputModel | null>(null);

  useEffect(() => {
    const fetchFileDetails = async () => {
      try {
        const details = await getFile(fileId.toString());
        setDetails(details);
      } catch (error) {
        console.error("Error fetching file details:", error);
      }
    };
    fetchFileDetails();
  }, [fileId]);

  return (
    <View>
      <Text className="text-black-100">File {fileId}</Text>
      {details && <FileInfo {...details} />}
    </View>
  );
};

export default FileDetails;

const styles = StyleSheet.create({});
