// ServiceContext.tsx
"use client";

import { createContext, useState, useContext, Dispatch, SetStateAction } from "react";

interface ServiceContextType {
  currentService: string;
  setCurrentService: Dispatch<SetStateAction<string>>;
}

const defaultValue: ServiceContextType = {
  currentService: "general-chat",
  setCurrentService: () => {},
};

const ServiceContext = createContext<ServiceContextType>(defaultValue);

export function ServiceProvider({ children }: { children: React.ReactNode }) {
  const [currentService, setCurrentService] = useState("general-chat");

  return (
    <ServiceContext.Provider value={{ currentService, setCurrentService }}>
      {children}
    </ServiceContext.Provider>
  );
}

export function useService() {
  return useContext(ServiceContext);
}
