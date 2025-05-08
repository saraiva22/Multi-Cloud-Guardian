/**
 * PerformanceType
 * @description - This enum is used to define the performance type of a user.
 */

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
