import { icons } from "@/constants";

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

/**
 * LOCATION_ARRAY
 * @description - This array is used to define the location types and their corresponding icons.
 */
export const LOCATION_ARRAY = [
  { label: LocationType.NORTH_AMERICA, icon: icons.northAmerica },
  { label: LocationType.SOUTH_AMERICA, icon: icons.southAmerica },
  { label: LocationType.EUROPE, icon: icons.europe },
  { label: LocationType.OTHERS, icon: icons.others },
];

/**
 * LocationTypeInfo
 * @description - This object is used to define the label and icon of each location type.
 */

export const LocationTypeInfo = {
  NORTH_AMERICA: { label: "North America", icon: icons.northAmerica },
  SOUTH_AMERICA: { label: "South America", icon: icons.southAmerica },
  EUROPE: { label: "Europe", icon: icons.europe },
  OTHERS: { label: "Others", icon: icons.others },
} as const;


