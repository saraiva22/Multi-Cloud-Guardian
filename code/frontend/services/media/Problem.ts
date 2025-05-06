export type Problem = {
  type: string;
  title: string;
  status: number;
  details: string;
  instance?: string;
};

export const MEDIA_TYPE_PROBLEM = "application/problem+json";

export function isProblem(object: object): boolean {
  return (
    typeof object === "object" &&
    object !== null &&
    "type" in object &&
    "title" in object &&
    "status" in object &&
    typeof object.status === "number"
  );
}
