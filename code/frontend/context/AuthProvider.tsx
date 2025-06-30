import { createContext, useContext, useEffect, useState } from "react";
import { getValueFor, removeValueFor } from "@/services/storage/SecureStorage";
import {
  getSSE,
  initializeSSE,
  setSSE,
} from "@/services/notifications/SSEManager";

export const KEY_NAME = "user_info";
export const KEY_MASTER = "key_master-";
export const TOKEN = "token";


const AuthContext = createContext({
  username: undefined,
  token: "",
  keyMaster: undefined,
  isLogged: false,
  loading: false,
  setUsername: (_) => {},
  setToken: (_) => {},
  setKeyMaster: (_) => {},
  setIsLogged: (_) => {},
  setLoading: (_) => {},
});

export function AuthProvider({ children }: any) {
  const [observedUsername, setUsername] = useState(undefined);
  const [observedKeyMaster, setKeyMaster] = useState(undefined);
  const [observedToken, setToken] = useState(null);
  const [isLogged, setIsLogged] = useState(false);
  const [loading, setLoading] = useState(true);
  const eventSource = getSSE();

  useEffect(() => {
    const loadUserData = async () => {
      try {
        const res = await getValueFor(KEY_NAME);
        if (res) {
          setIsLogged(true);
          setUsername(res.username);
          const masterKey = await getValueFor(`${KEY_MASTER}${res.username}`);
          const myToken = await getValueFor(`${TOKEN}${res.username}`);
          if (masterKey) {
            setKeyMaster(masterKey);
          }
          if (myToken) {
            setToken(myToken.token);
          }
          if (!eventSource) {
            const newEventSource = initializeSSE();
            if (newEventSource) {
              setSSE(newEventSource);
            }
          }
        } else {
          setIsLogged(false);
          setUsername(undefined);
          setToken(null);
        }
      } catch (error) {
        console.log(error);
      } finally {
        setLoading(false);
      }
    };

    loadUserData();
  }, []);

  const value = {
    username: observedUsername,
    token: observedToken,
    keyMaster: observedKeyMaster,
    isLogged: isLogged,
    loading: loading,
    setUsername: setUsername,
    setToken: setToken,
    setKeyMaster: setKeyMaster,
    setIsLogged: setIsLogged,
    setLoading: setLoading,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
export function useAuthentication() {
  const {
    username,
    setUsername,
    token,
    setToken,
    keyMaster,
    setKeyMaster,
    isLogged,
    setIsLogged,
    loading,
    setLoading,
  } = useContext(AuthContext);

  return {
    username,
    setUsername,
    token,
    setToken,
    keyMaster,
    setKeyMaster,
    isLogged,
    setIsLogged,
    loading,
    setLoading,
  };
}
