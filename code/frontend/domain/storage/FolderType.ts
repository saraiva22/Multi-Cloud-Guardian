/**
 * FolderType
 * @description - Enum that defines the available folder types for the user.
 * PRIVATE: Private folder, accessible only by the user.
 * SHARED: Shared folder, accessible by multiple users.
 */
export enum FolderType {
  PRIVATE,
  SHARED,
}

/**
 * CostTypeLabel
 * @description - Maps each FolderType to its human-readable label.
 */
export const CostTypeLabel = {
  [FolderType.PRIVATE]: "Private",
  [FolderType.SHARED]: "Shared",
};

/**
 * CostTypeValue
 * @description - Maps each FolderType to its corresponding numeric value.
 */
export const CostTypeValue = {
  [FolderType.PRIVATE]: 0,
  [FolderType.SHARED]: 1,
};
