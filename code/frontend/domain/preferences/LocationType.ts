/**
 * LocationType
 * @description - This enum is used to define the location type of a user.
 */
export enum LocationType {
  NORTH_AMERICA,
  SOUTH_AMERICA,
  EUROPE,
  OTHERS,
}

/**
 * LocationTypeLabel
 * @description - This object is used to define the label of each location type.
 */

export const LocationTypeLabel = {
  [LocationType.NORTH_AMERICA]: "North America",
  [LocationType.SOUTH_AMERICA]: "South America",
  [LocationType.EUROPE]: "Europe",
  [LocationType.OTHERS]: "Others",
};

/**
 * LocationTypeValue
 * @description - This object is used to define the value of each location type.
 */
export const LocationTypeValue = {
  [LocationType.NORTH_AMERICA]: 0,
  [LocationType.SOUTH_AMERICA]: 1,
  [LocationType.EUROPE]: 2,
  [LocationType.OTHERS]: 3,
};
