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
import { Folder } from "@/domain/storage/Folder";
import { File } from "@/domain/storage/File";
import { FolderOutputModel } from "./model/FolderOutputModel";
import { FolderType } from "@/domain/storage/FolderType";
import { RegisterOutput } from "../users/models/RegisterOutputModel";
import { Invite } from "@/domain/storage/Invite";
import { InviteStatusType } from "@/domain/storage/InviteStatusType";

const httpService = httpServiceInit();

export async function uploadFile(
  file: any,
  fileName: string,
  encryption: boolean,
  keyMaster: any,
  parentFolderId?: string
): Promise<UploadOutput> {
  const apiRoute = parentFolderId
    ? apiRoutes.UPLOAD_FILE_IN_FOLDER.replace(":id", parentFolderId)
    : apiRoutes.UPLOAD_FILE;

  const path = PREFIX_API + apiRoute;

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

export async function downloadFileInFolder(
  folderId: string,
  fileId: string
): Promise<DownloadOutputModel> {
  const path =
    PREFIX_API +
    apiRoutes.DOWNLOAD_FILE_IN_FOLDER.replace(":folderId", folderId).replace(
      ":fileId",
      fileId
    );
  return await httpService.get<DownloadOutputModel>(path);
}

export async function deleteFile(fileId: string): Promise<void> {
  const path = PREFIX_API + apiRoutes.DELETE_FILE.replace(":id", fileId);
  return await httpService.delete<void>(path);
}

export async function deleteFileInFolder(
  folderId: string,
  fileId: string
): Promise<void> {
  const path =
    PREFIX_API +
    apiRoutes.DELETE_FILE_IN_FOLDER.replace(":folderId", folderId).replace(
      ":fileId",
      fileId
    );
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

export async function getFile(fileId: string): Promise<FileOutputModel> {
  const path = PREFIX_API + apiRoutes.GET_FILE_BY_ID.replace(":id", fileId);
  return await httpService.get<FileOutputModel>(path);
}

export async function getFileInFolder(
  folderId: string,
  fileId: string
): Promise<FileOutputModel> {
  const path =
    PREFIX_API +
    apiRoutes.GET_FILE_IN_FOLDER.replace(":folderId", folderId).replace(
      ":fileId",
      fileId
    );
  return await httpService.get<FileOutputModel>(path);
}

export async function getFiles(
  sortBy: string = "created_at",
  page: number = 0,
  size: number = 10
): Promise<PageResult<File>> {
  const path = PREFIX_API + apiRoutes.GET_FILES;
  return await httpService.get<PageResult<File>>(path, {
    sort: sortBy,
    page: String(page),
    size: String(size),
  });
}

export async function getFilesInFolder(
  fileId: string,
  sortBy: string = "created_at",
  page: number = 0,
  size: number = 10
): Promise<PageResult<File>> {
  const path =
    PREFIX_API + apiRoutes.GET_FILES_IN_FOLDER.replace(":id", fileId);
  return await httpService.get<PageResult<File>>(path, {
    sort: sortBy,
    page: String(page),
    size: String(size),
  });
}

export async function getFolder(folderId: string): Promise<FolderOutputModel> {
  const path = PREFIX_API + apiRoutes.GET_FOLDER_BY_ID.replace(":id", folderId);
  return await httpService.get<FolderOutputModel>(path);
}

export async function getFolders(
  sortBy: string = "created_at",
  shared: boolean = false,
  page: number = 0,
  size: number = 10
): Promise<PageResult<Folder>> {
  const path = PREFIX_API + apiRoutes.GET_FOLDERS;
  return await httpService.get<PageResult<Folder>>(path, {
    sort: sortBy,
    shared: String(shared),
    page: String(page),
    size: String(size),
  });
}

export async function getFoldersInFolder(
  folderId: string,
  sortBy: string = "created_at",
  page: number = 0,
  size: number = 10
): Promise<PageResult<Folder>> {
  const path =
    PREFIX_API + apiRoutes.GET_FOLDERS_IN_FOLDER.replace(":id", folderId);
  return await httpService.get<PageResult<Folder>>(path, {
    sort: sortBy,
    page: String(page),
    size: String(size),
  });
}
export async function createFolder(
  folderName: string,
  folderType: FolderType
): Promise<RegisterOutput> {
  const path = PREFIX_API + apiRoutes.CREATE_FOLDER;
  return await httpService.post<RegisterOutput>(
    path,
    JSON.stringify({
      folderName: folderName,
      folderType: folderType,
    })
  );
}

export async function createSubFolder(
  folderId: string,
  folderName: string,
  folderType: FolderType
): Promise<RegisterOutput> {
  const path =
    PREFIX_API + apiRoutes.CREATE_FOLDER_IN_FOLDER.replace(":id", folderId);
  return await httpService.post<RegisterOutput>(
    path,
    JSON.stringify({
      folderName: folderName,
      folderType: folderType,
    })
  );
}

export async function deleteFolder(folderId: string): Promise<void> {
  const path = PREFIX_API + apiRoutes.DELETE_FOLDER.replace(":id", folderId);
  return await httpService.delete<void>(path);
}

export async function getReceivedInvites(
  sortBy: string = "created_at",
  page: number = 0,
  size: number = 10
): Promise<PageResult<Invite>> {
  const path = PREFIX_API + apiRoutes.RECEIVED_FOLDER_INVITES;
  return await httpService.get<PageResult<Invite>>(path, {
    sort: sortBy,
    page: String(page),
    size: String(size),
  });
}

export async function getSentInvites(
  sortBy: string = "created_at",
  page: number = 0,
  size: number = 10
): Promise<PageResult<Invite>> {
  const path = PREFIX_API + apiRoutes.SENT_FOLDER_INVITES;
  return await httpService.get<PageResult<Invite>>(path, {
    sort: sortBy,
    page: String(page),
    size: String(size),
  });
}

export async function validateFolderInvite(
  folderId: string,
  inviteId: string,
  status: InviteStatusType
): Promise<void> {
  const path =
    PREFIX_API +
    apiRoutes.VALIDATE_FOLDER_INVITE.replace(":folderId", folderId).replace(
      ":inviteId",
      inviteId
    );

  return await httpService.post<void>(
    path,
    JSON.stringify({
      inviteStatus: status,
    })
  );
}

// Utils

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

    if (calculatedSignature !== signatureFromFile.toString("hex")) {
      console.log("ERROR, invalidSignature");
      throw new Error("Invalid signature! The file may have been corrupted");
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
