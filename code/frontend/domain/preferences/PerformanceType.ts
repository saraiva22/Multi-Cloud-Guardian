export enum PerformanceType {
  LOW,
  MEDIUM,
  HIGH,
}

export const PerformanceTypeLabel = {
  [PerformanceType.LOW]: "Low",
  [PerformanceType.MEDIUM]: "Medium",
  [PerformanceType.HIGH]: "High",
};
export const PerformanceTypeValue = {
  [PerformanceType.LOW]: 0,
  [PerformanceType.MEDIUM]: 1,
  [PerformanceType.HIGH]: 2,
};
