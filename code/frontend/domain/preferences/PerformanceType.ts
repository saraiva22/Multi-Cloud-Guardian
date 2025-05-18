/**
 * PerformanceType
 * @description - This enum is used to define the performance type of a user.
 */

import { icons } from "@/constants";

export enum PerformanceType {
  LOW,
  MEDIUM,
  HIGH,
}

/**
 * PerformanceTypeLabel
 * @description - This object is used to define the label of each performance type.
 */
export const PerformanceTypeLabel = {
  [PerformanceType.LOW]: "Low",
  [PerformanceType.MEDIUM]: "Medium",
  [PerformanceType.HIGH]: "High",
};

/**
 * PerformanceTypeValue
 * @description - This object is used to define the value of each performance type.
 */
export const PerformanceTypeValue = {
  [PerformanceType.LOW]: 0,
  [PerformanceType.MEDIUM]: 1,
  [PerformanceType.HIGH]: 2,
};

/**
 * PerformanceArray
 * @description - This array is used to define the performance types and their corresponding icons.
 */

export const PERFORMANCE_ARRAY = [
  { label: PerformanceType.LOW, icon: icons.low },
  { label: PerformanceType.MEDIUM, icon: icons.medium },
  { label: PerformanceType.HIGH, icon: icons.high },
];

/**
 * PerformanceTypeInfo
 * @description - This object is used to define the label and icon of each performance type.
 */
export const PerformanceTypeInfo = {
  LOW: { label: "Low", icon: icons.low },
  MEDIUM: { label: "Medium", icon: icons.medium },
  HIGH: { label: "High", icon: icons.high },
} as const;
