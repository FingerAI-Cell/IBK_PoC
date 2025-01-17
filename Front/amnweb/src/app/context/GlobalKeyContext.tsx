import React, { useState, createContext, useContext } from "react";

// 글로벌 키 관리 Context
const GlobalKeyContext = createContext<{
  globalKey: string | null;
  setGlobalKey: React.Dispatch<React.SetStateAction<string | null>>;
}>({
  globalKey: null,
  setGlobalKey: () => {},
});

export const useGlobalKey = () => useContext(GlobalKeyContext);

export const GlobalKeyProvider: React.FC = ({ children }) => {
  const [globalKey, setGlobalKey] = useState<string | null>(null);

  return (
    <GlobalKeyContext.Provider value={{ globalKey, setGlobalKey }}>
      {children}
    </GlobalKeyContext.Provider>
  );
};
