// ServiceContext.tsx
"use client";

import { createContext, useState, useContext, useEffect } from "react";

interface ServiceContextType {
  currentService: string;
  setCurrentService: (service: string) => void;
  pageState: 'select' | 'chat' | 'admin';
  setPageState: (state: 'select' | 'chat' | 'admin') => void;
  handleMyServices: () => void;
}

const defaultValue: ServiceContextType = {
  currentService: "general-chat",
  setCurrentService: () => {},
  pageState: "select",
  setPageState: () => {},
  handleMyServices: () => {},
};

const ServiceContext = createContext<ServiceContextType>(defaultValue);

export function ServiceProvider({ children }: { children: React.ReactNode }) {
  const [currentService, setCurrentService] = useState("general-chat");
  const [pageState, setPageState] = useState<'select' | 'chat' | 'admin'>('select');

  const handleServiceChange = (service: string) => {
    console.log('Service Change:', {
      current: currentService,
      new: service,
      currentPageState: pageState
    });
    
    setCurrentService(service);
    setPageState('select');
  };

  useEffect(() => {
    console.log('State Updated:', {
      service: currentService,
      pageState: pageState
    });
  }, [currentService, pageState]);

  const handleMyServices = () => {
    setCurrentService("general-chat");
    setPageState('select');
  };

  return (
    <ServiceContext.Provider 
      value={{ 
        currentService, 
        setCurrentService: handleServiceChange,
        pageState,
        setPageState,
        handleMyServices
      }}
    >
      {children}
    </ServiceContext.Provider>
  );
}

export function useService() {
  return useContext(ServiceContext);
}
