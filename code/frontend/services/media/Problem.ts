export type Problem = {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance?: string;
};

export const MEDIA_TYPE_PROBLEM = "application/problem+json";

export function isProblem(object: any): object is Problem {
  return (
    typeof object === "object" &&
    object !== null &&
    "detail" in object &&
    "type" in object &&
    "title" in object &&
    "status" in object &&
    typeof object.status === "number"
  );
}

export function getProblemMessage(problem: Problem): string {
  return problem.detail || problem.title;
}
