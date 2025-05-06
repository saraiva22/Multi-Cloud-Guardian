import pbkdf2 from "pbkdf2";
import { Buffer } from "buffer";
import * as Crypto from "expo-crypto";
import CryptoJS from "crypto-js";
import * as FileSystem from "expo-file-system";

const ALGORITHM = "sha256";
const IV_LENGTH_BYTES = 12;
const AES_LENGTH_BYTES = 32;
const SALT_LENGTH_BYTES = 16;

// Generates a 256-bits MASTER KEY
export const generateMasterKey = async (
  salt: ArrayBuffer,
  password: string,
  iterations = 20000,
  keyLength = 32,
  digest = ALGORITHM
): Promise<string> => {
  const saltBuffer = Buffer.from(new Uint8Array(salt));

  const key = pbkdf2.pbkdf2Sync(
    password,
    saltBuffer,
    iterations,
    keyLength,
    digest
  );
  return Buffer.from(key).toString("hex");
};

// Generates a random IV of 12 Bytes
export const generateIV = async (): Promise<ArrayBuffer> => {
  return await generateRandomValue(IV_LENGTH_BYTES);
};

// Generates a Key of 32 Bytes
export const generationKeyAES = async (): Promise<ArrayBuffer> => {
  return await generateRandomValue(AES_LENGTH_BYTES);
};

// Generates a salt of 16 Bytes
export const generateRandomSalt = async (): Promise<ArrayBuffer> => {
  return await generateRandomValue(SALT_LENGTH_BYTES);
};

export const generateRandomValue = async (
  size: number
): Promise<ArrayBuffer> => {
  const byteArray = new Uint8Array(size);
  Crypto.getRandomValues(byteArray);
  return byteArray.buffer;
};

export const encryptData = async (
  data: ArrayBuffer,
  key: ArrayBuffer,
  iv: ArrayBuffer
): Promise<ArrayBuffer> => {
  try {
    // Encrypted (message: {}, key: {}, cfg:{})
    const encrypted = CryptoJS.AES.encrypt(
      toWordArray(data),
      toWordArray(key),
      {
        iv: toWordArray(iv),
        mode: CryptoJS.mode.CBC,
        padding: CryptoJS.pad.Pkcs7,
      }
    );

    return toUint8Array(encrypted.ciphertext).buffer;
  } catch (error) {
    console.log(error);
    throw error;
  }
};

export const decryptData = async (
  data: ArrayBuffer,
  key: ArrayBuffer,
  iv: ArrayBuffer
): Promise<ArrayBuffer> => {
  try {
    // Decrypted (ciphertext: {}, key: {}, cfg:{})
    const decrypted = CryptoJS.AES.decrypt(
      CryptoJS.lib.CipherParams.create({
        ciphertext: toWordArray(data),
      }),
      toWordArray(key),
      {
        iv: toWordArray(iv),
        mode: CryptoJS.mode.CBC,
        padding: CryptoJS.pad.Pkcs7,
      }
    );

    return toUint8Array(decrypted).buffer;
  } catch (error) {
    console.log("Error:", error);
    throw error;
  }
};

export const readFileAsArrayBuffer = async (
  uri: string
): Promise<ArrayBuffer> => {
  try {
    const base64 = await FileSystem.readAsStringAsync(uri, {
      encoding: FileSystem.EncodingType.Base64,
    });
    const binary = Buffer.from(base64, "base64");
    return binary.buffer;
  } catch (error) {
    console.error("Error read file", error);
    throw error;
  }
};

export function createHmacSHA256(data: Uint8Array, key: Uint8Array): string {
  const wordData = CryptoJS.lib.WordArray.create(data);
  const wordKey = CryptoJS.lib.WordArray.create(key);

  const hmac = CryptoJS.HmacSHA256(wordData, wordKey);
  return CryptoJS.enc.Hex.stringify(hmac);
}

// Utils

// Uint8Array → WordArray
function toWordArray(data: ArrayBuffer): CryptoJS.lib.WordArray {
  return CryptoJS.lib.WordArray.create(new Uint8Array(data));
}

// WordArray → Uint8Array
function toUint8Array(
  wordArray: CryptoJS.lib.WordArray
): Uint8Array<ArrayBuffer> {
  const { words, sigBytes } = wordArray;
  const result = new Uint8Array(sigBytes);
  for (let i = 0; i < sigBytes; i++) {
    result[i] = (words[i >>> 2] >>> (24 - (i % 4) * 8)) & 0xff;
  }
  return result;
}
