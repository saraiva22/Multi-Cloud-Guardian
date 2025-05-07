import { createContext, useContext, useEffect, useState } from "react";
import { getValueFor } from "@/services/storage/SecureStorage";

const KEY_NAME = "user_info";
const KEY_MASTER = "key_master";

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

export function AuthProvider({ children }) {
  const [observedUsername, setUsername] = useState(undefined);
  const [observedKeyMaster, setKeyMaster] = useState(undefined);
  const [isLogged, setIsLogged] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getValueFor(KEY_NAME)
      .then((res) => {
        if (res) {
          setIsLogged(true);
          setUsername(res.username);
          console.log("Value  1 ", res.username);
        } else {
          setIsLogged(false);
          setUsername(undefined);
        }
      })
      .catch((error) => {
        console.log(error);
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  useEffect(() => {
    getValueFor(KEY_MASTER)
      .then((res) => {
        if (res) {
          setKeyMaster(res.key);
          console.log("KEYMASTER: ", res.key);
        } else {
          setKeyMaster(undefined);
        }
      })
      .catch((error) => {
        console.log(error);
      });
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
