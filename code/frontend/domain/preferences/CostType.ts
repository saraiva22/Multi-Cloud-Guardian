/**
 * CostType
 * @description - This enum is used to define the cost type of a user.
 */

import { icons } from "@/constants";

export enum CostType {
  LOW,
  MEDIUM,
  HIGH,
}

/**
 * CostTypeLabel
 * @description - This object is used to define the label of each cost type.
 */
export const CostTypeLabel = {
  [CostType.LOW]: "Low",
  [CostType.MEDIUM]: "Medium",
  [CostType.HIGH]: "High",
};

/**
 * CostTypeValue
 * @description - This object is used to define the value of each cost type.
 */
export const CostTypeValue = {
  [CostType.LOW]: 0,
  [CostType.MEDIUM]: 1,
  [CostType.HIGH]: 2,
};

/**
 * CostArray
 * @description - This array is used to define the cost types and their corresponding icons.
 */

export const COST_ARRAY = [
  { label: CostType.LOW, icon: icons.low },
  { label: CostType.MEDIUM, icon: icons.medium },
  { label: CostType.HIGH, icon: icons.high },
];

/**
 * CostTypeInfo
 * @description - This object is used to define the label and icon of each cost type.
 */
export const CostTypeInfo = {
  LOW: { label: "Low", icon: icons.low },
  MEDIUM: { label: "Medium", icon: icons.medium },
  HIGH: { label: "High", icon: icons.high },
} as const;
