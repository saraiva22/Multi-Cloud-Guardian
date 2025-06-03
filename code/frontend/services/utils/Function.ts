import { months } from "@/constants/months";

export const formatSize = (bytes?: number) => {
  if (!bytes) return "0 Kb";
  if (bytes >= 1000 * 1000) return `${(bytes / (1000 * 1000)).toFixed(1)} Mb`;
  if (bytes >= 1000) return `${(bytes / 1000).toFixed(1)} Kb`;
  return `${bytes} B`;
};

export const formatDate = (date: number) => {
  const dateObj = new Date(date * 1000);
  return `${
    months[dateObj.getMonth()]
  } ${dateObj.getDate()}, ${dateObj.getFullYear()}`;
};
