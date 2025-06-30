import EventSource from "react-native-sse";
import { apiRoutes, PREFIX_API } from "../utils/HttpService";
import { cache } from "react";

let listener: EventSource | null = null;

const url = PREFIX_API + apiRoutes.NOTIFICATIONS;

export function initializeSSE(): EventSource | null {
  if (!listener) {
    try {
      listener = new EventSource(url);
    } catch (error) {
      listener?.close();
      listener = null;
    }
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
