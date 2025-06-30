import { createContext, useContext, useEffect, useState } from "react";
import { getValueFor, removeValueFor } from "@/services/storage/SecureStorage";
import {
  getSSE,
  initializeSSE,
  setSSE,
} from "@/services/notifications/SSEManager";
import { apiRoutes, PREFIX_API } from "@/services/utils/HttpService";

const KEY_NAME = "user_info";
const KEY_MASTER = "key_master-";

type State = {
  username: string | undefined;
  keyMaster: string | undefined;
  isLogged: boolean;
  loading: boolean;
  setUsername: (username: string | undefined) => void;
  setKeyMaster: (keyMaster: string | undefined) => void;
  setIsLogged: (isLogged: boolean) => void;
  setLoading: (loading: boolean) => void;
};

const AuthContext = createContext({
  username: undefined,
  keyMaster: undefined,
  isLogged: false,
  loading: false,
  setUsername: (_) => {},
  setKeyMaster: (_) => {},
  setIsLogged: (_) => {},
  setLoading: (_) => {},
});

export function AuthProvider({ children }: any) {
  const [observedUsername, setUsername] = useState(undefined);
  const [observedKeyMaster, setKeyMaster] = useState(undefined);
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
          if (masterKey) {
            setKeyMaster(masterKey);
          }
          if (!eventSource) {
            const newEventSource = initializeSSE();
            setSSE(newEventSource);
          }
        } else {
          setIsLogged(false);
          setUsername(undefined);
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
    keyMaster: observedKeyMaster,
    isLogged: isLogged,
    loading: loading,
    setUsername: setUsername,
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
    keyMaster,
    setKeyMaster,
    isLogged,
    setIsLogged,
    loading,
    setLoading,
  };
}
