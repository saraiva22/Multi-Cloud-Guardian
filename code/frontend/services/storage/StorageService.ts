import { MEDIA_TYPE_PROBLEM, Problem } from "../media/Problem";
import {
  createHmacSHA256,
  decryptData,
  encryptData,
  generateIV,
  generationKeyAES,
  readFileAsArrayBuffer,
} from "../security/SecurityService";
import httpServiceInit, { apiRoutes, PREFIX_API } from "../utils/HttpService";
import { UploadOutput } from "./model/UploadFileOutputModel";
import * as FileSystem from "expo-file-system";
import { Buffer } from "buffer";
import { FilesListOutputModel } from "./model/FilesListOutputModel";
import { FileOutputModel } from "./model/FileOutputModel";
import { FoldersListOutputModel } from "./model/FoldersListOutputModel";
import { DownloadOutputModel } from "./model/DownloadOutputModel";
import * as MediaLibrary from "expo-media-library";
import * as Sharing from "expo-sharing";
import { Alert, Platform } from "react-native";
import { PageResult } from "@/domain/utils/PageResult";
import { FolderType } from "@/domain/storage/FolderType";
import { FileType } from "@/domain/storage/FileType";

const httpService = httpServiceInit();

export async function uploadFile(
  file: any,
  fileName: string,
  encryption: boolean,
  keyMaster: any
): Promise<UploadOutput> {
  const path = PREFIX_API + apiRoutes.UPLOAD_FILE;

  const formData = new FormData();

  if (encryption) {
    // Generate File Key and IV per file
    const fileKey = await generationKeyAES();
    const fileIV = await generateIV();

    // Encrypt the file data (uri → encrypted ArrayBuffer)
    const imageBuffer = await readFileAsArrayBuffer(file.uri);
    const encryptedFileData = await encryptData(imageBuffer, fileKey, fileIV);

    // Concatenate IV + encrypted file data
    const fileEncryptedWithIV = Buffer.concat([
      Buffer.from(new Uint8Array(fileIV)),
      Buffer.from(new Uint8Array(encryptedFileData)),
    ]);

    // Generate HMAC for file data - GCM mode is not available
    const signature = createHmacSHA256(
      new Uint8Array(fileEncryptedWithIV),
      new Uint8Array(fileKey)
    );

    // [signature (32 bytes)] + [IV (12 bytes)] + [encrypted data]
    const finalEncryptedFile = Buffer.concat([
      Buffer.from(signature, "hex"), // 32 bytes - HMAC
      Buffer.from(new Uint8Array(fileIV)), // 12 bytes - IV
      Buffer.from(new Uint8Array(encryptedFileData)), // encrypted data
    ]);

    // Save the temporary file for upload
    const encryptedFilePath = `${FileSystem.cacheDirectory}${file.name}.enc`;

    await FileSystem.writeAsStringAsync(
      encryptedFilePath,
      finalEncryptedFile.toString("base64"),
      { encoding: FileSystem.EncodingType.Base64 }
    );

    // Generate IV for encrypting the file's AES key
    const keyIV = await generateIV();

    // Encrypt the file's AES key with the masterKey
    const encryptedFileKey = await encryptData(fileKey, keyMaster, keyIV);

    // Concatenate IV + encrypted AES key
    const keyWithIV = Buffer.concat([
      Buffer.from(new Uint8Array(keyIV)),
      Buffer.from(new Uint8Array(encryptedFileKey)),
    ]);

    // Create FormData for the upload
    formData.append("file", {
      uri: encryptedFilePath,
      name: fileName,
      type: file.type || "application/octet-stream",
    });

    formData.append("encryption", encryption.toString());
    formData.append("encryptedKey", keyWithIV.toString("base64"));
  } else {
    formData.append("file", {
      uri: file.uri,
      name: fileName,
      type: file.type || "application/octet-stream",
    });
    formData.append("encryption", encryption.toString());
    formData.append("encryptedKey", "");
  }

  // Send the request
  const options = {
    method: "POST",
    body: formData,
    headers: {
      Accept: "application/json",
    },
  };
  console.log("OPTION ", options);

  const response = await fetch(path, options);

  if (!response.ok) {
    if (response.headers.get("Content-Type")?.includes(MEDIA_TYPE_PROBLEM)) {
      const res = await response.json();
      throw res as Problem;
    } else throw new Error(`HTTP error! Status: ${response.status}`);
  }

  const contentTypes = response.headers.get("Content-Type");
  if (!contentTypes || !contentTypes.includes("application/json")) {
    return {} as UploadOutput;
  }

  return (await response.json()) as UploadOutput;
}

export async function downloadFile(
  fileId: string
): Promise<DownloadOutputModel> {
  const path = PREFIX_API + apiRoutes.DOWNLOAD_FILE.replace(":id", fileId);
  return await httpService.get<DownloadOutputModel>(path);
}

export async function deleteFile(fileId: string): Promise<void> {
  const path = PREFIX_API + apiRoutes.DELETE_FILE.replace(":id", fileId);
  return await httpService.delete<void>(path);
}

export async function generateTemporaryUrl(
  fileId: string,
  minutes: number
): Promise<FileOutputModel> {
  const path = PREFIX_API + apiRoutes.CREATE_TEMP_URL.replace(":id", fileId);
  return await httpService.post<FileOutputModel>(
    path,
    JSON.stringify({
      expiresIn: minutes,
    })
  );
}

export async function processAndSaveDownloadedFile(
  downloadFile: DownloadOutputModel,
  keyMaster: any
): Promise<any> {
  const fileContent = downloadFile.file.fileContent;
  const fileEncrypted = downloadFile.file.encrypted;
  const encryptedKeyBase64 = downloadFile.fileKeyEncrypted;
  const fileName = downloadFile.file.fileName;
  const mimeType = downloadFile.file.mimeType;

  let finalData: Uint8Array;

  if (fileEncrypted) {
    //  Extract IV and encrypted data from the file
    const fileBuffer = Buffer.from(fileContent, "base64");

    // Extract signature (first 32 bytes for SHA-256)
    const signatureFromFile = fileBuffer.slice(0, 32);

    // Extrair IV + encrypted data
    const fileIV = fileBuffer.slice(32, 44); // 12 bytes after the signature
    const encryptedFile = fileBuffer.slice(44); // File Content

    // Decrypt the file's AES key using the masterKey
    const encryptedKeyBuffer = Buffer.from(encryptedKeyBase64, "base64");
    const keyIV = encryptedKeyBuffer.slice(0, 12);
    const encryptedFileKey = encryptedKeyBuffer.slice(12);

    const fileKey = await decryptData(encryptedFileKey, keyMaster, keyIV);

    // Recreate the content that was signed (IV + encrypted)
    const fileEncryptedWithIV = Buffer.concat([fileIV, encryptedFile]);

    const calculatedSignature = createHmacSHA256(
      new Uint8Array(fileEncryptedWithIV),
      new Uint8Array(fileKey)
    );

    console.log("CALCULATE ", calculatedSignature);
    console.log("SIGNATURE ", signatureFromFile.toString("hex"));

    if (calculatedSignature !== signatureFromFile.toString("hex")) {
      console.log("ERROR, invalidSignature");
      throw new Error("Invalid signature! The file may have been corrupted");
    } else {
      console.log("SIGNATURE CORRECT!!!");
    }
    // Decrypt the file content
    const decryptedFile = await decryptData(encryptedFile, fileKey, fileIV);

    finalData = new Uint8Array(decryptedFile);
  } else {
    // If the file is not encrypted
    finalData = new Uint8Array(Buffer.from(fileContent, "base64"));
  }

  // Save the file locally
  await saveFileLocally(finalData, fileName);
}

export async function getFiles(): Promise<PageResult<FileType>> {
  const path = PREFIX_API + apiRoutes.GET_FILES;
  return await httpService.get<PageResult<FileType>>(path);
}

export async function getFile(fileId: string): Promise<FileOutputModel> {
  const path = PREFIX_API + apiRoutes.GET_FILE_BY_ID.replace(":id", fileId);
  return await httpService.get<FileOutputModel>(path);
}

export async function getFolders(): Promise<PageResult<FolderType>> {
  const path = PREFIX_API + apiRoutes.GET_FOLDERS;
  return await httpService.get<PageResult<FolderType>>(path);
}

// Utils

async function saveFileLocally(data: Uint8Array, fileName: string) {
  try {
    const fileUri = `${FileSystem.documentDirectory}${fileName}`;

    // Write the content to the file
    await FileSystem.writeAsStringAsync(
      fileUri,
      Buffer.from(data).toString("base64"),
      {
        encoding: FileSystem.EncodingType.Base64,
      }
    );

    if (Platform.OS === "android") {
      // Save to gallery or downloads folder
      const permission = await MediaLibrary.requestPermissionsAsync();
      if (permission.granted) {
        const asset = await MediaLibrary.createAssetAsync(fileUri);
        await MediaLibrary.createAlbumAsync("Download", asset, false);
        return Alert.alert("Save", "Success Download");
      } else {
        return Alert.alert("Error", "Permissions not granted to save files");
      }
    } else {
      // iOS: open direct sharing (Files, AirDrop, etc.)
      if (await Sharing.isAvailableAsync()) {
        await Sharing.shareAsync(fileUri);
      } else {
        Alert.alert("Error", "Sharing not available");
      }
    }
  } catch (error) {
    console.error("Error saving the file:", error);
    Alert.alert("Error", "Could not save the file");
  }
}
