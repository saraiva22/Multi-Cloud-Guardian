import { Problem } from "@/services/media/Problem";
import { useEffect, useReducer } from "react";

// The State
type State =
  | { tag: "begin" }
  | { tag: "loading"; url: string }
  | { tag: "loaded"; payload: any; url: string }
  | { tag: "error"; error: Error | Problem; url: string };

// The Action
type Action =
  | { type: "start-loading"; url: string }
  | { type: "loading-success"; payload: any; url: string }
  | { type: "loading-error"; error: Error | Problem };

// The Logger
function logUnexpectedAction(state: State, action: Action) {
  console.log(`Unexpected action '${action.type} on state '${state.tag}'`);
  return state;
}

// The reducer
function reducer(state: State, action: Action): State {
  switch (action.type) {
    case "start-loading":
      return { tag: "loading", url: action.url };
    case "loading-success":
      if (state.tag !== "loading") {
        return logUnexpectedAction(state, action);
      }
      return { tag: "loaded", payload: action.payload, url: action.url };
    case "loading-error":
      if (state.tag !== "loading") {
        return logUnexpectedAction(state, action);
      }
      return { tag: "error", error: action.error, url: state.url };
  }
}

const firstState: State = { tag: "begin" };

type UseFetchResult = State;

export function useFetch(url: string): UseFetchResult {
  const [state, dispatch] = useReducer(reducer, firstState);

  useEffect(() => {
    if (!url) {
      return;
    }

    let cancelled = false;
    const abortController = new AbortController();
    async function doFetch() {
      dispatch({ type: "start-loading", url: url });
      try {
        const response = await fetch(url, { signal: abortController.signal });
        const json = await response.json();
        if (!cancelled) {
          dispatch({ type: "loading-success", payload: json, url: url });
        }
      } catch (error) {
        if (!cancelled) {
          dispatch({ type: "loading-error", error: error });
        }
      }
    }
    doFetch();
    return () => {
      cancelled = true;
      abortController.abort();
    };
  }, [url]);
  return state;
}
