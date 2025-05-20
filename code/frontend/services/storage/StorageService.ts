import { MEDIA_TYPE_PROBLEM, Problem } from "../media/Problem";
import {
  createHmacSHA256,
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

export async function getFiles(): Promise<FilesListOutputModel> {
  const path = PREFIX_API + apiRoutes.GET_FILES;
  return await httpService.get<FilesListOutputModel>(path);
}

export async function getFile(fileId: number): Promise<FileOutputModel> {
  const path =
    PREFIX_API + apiRoutes.GET_FILE_BY_ID.replace(":id", String(fileId));
  return await httpService.get<FileOutputModel>(path);
}

export async function getFolders(): Promise<FoldersListOutputModel> {
  const path = PREFIX_API + apiRoutes.GET_FOLDERS;
  return await httpService.get<FoldersListOutputModel>(path);
}
