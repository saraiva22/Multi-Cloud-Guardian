type FileOutputModel = {
  fileContent: ArrayBuffer;
  fileName: string;
  mimeType: string;
  encrypted: boolean;
};

export type DownloadOutputModel = {
  file: FileOutputModel;
  fileKeyEncrypted: string | null;
};
