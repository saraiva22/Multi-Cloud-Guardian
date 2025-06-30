import EventSource from "react-native-sse";
import { apiRoutes, PREFIX_API } from "../utils/HttpService";

let listener: EventSource | null = null;

const url = PREFIX_API + apiRoutes.NOTIFICATIONS;

export function initializeSSE(): EventSource {
  if (!listener) {
    listener = new EventSource(url);

    listener.addEventListener("error", (event) => {
      console.error("Error Event Source", event);
      listener?.close();
      listener = null;
    });
  }
  return listener;
}

export function getSSE(): EventSource | null {
  return listener;
}

export function setSSE(newSSE: EventSource) {
  listener = newSSE;
}

export function closeSSE() {
  if (listener) {
    listener.removeAllEventListeners();
    listener.close();
    listener = null;
  }
}
