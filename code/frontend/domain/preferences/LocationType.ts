export enum LocationType {
  NORTH_AMERICA,
  SOUTH_AMERICA,
  EUROPE,
  OTHERS,
}

export const LocationTypeLabel = {
  [LocationType.NORTH_AMERICA]: "North America",
  [LocationType.SOUTH_AMERICA]: "South America",
  [LocationType.EUROPE]: "Europe",
  [LocationType.OTHERS]: "Others",
};
export const LocationTypeValue = {
  [LocationType.NORTH_AMERICA]: 0,
  [LocationType.SOUTH_AMERICA]: 1,
  [LocationType.EUROPE]: 2,
  [LocationType.OTHERS]: 3,
};
