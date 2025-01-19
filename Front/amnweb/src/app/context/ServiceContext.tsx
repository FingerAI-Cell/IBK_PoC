"use client";

import { createContext, useState, useContext, useEffect } from "react";

interface ServiceContextType {
  currentService: string;
  setCurrentService: (service: string) => void;
  pageState: "select" | "chat" | "admin";
  setPageState: (state: "select" | "chat" | "admin") => void;
  isSidebarOpen: boolean;
  toggleSidebar: () => void;
  handleMyServices: () => void;
}

const ServiceContext = createContext<ServiceContextType | undefined>(undefined);

export function ServiceProvider({ children }: { children: React.ReactNode }) {
  const [currentService, setCurrentService] = useState("general-chat");
  const [pageState, setPageState] = useState<"select" | "chat" | "admin">("select");
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);

  useEffect(() => {
    const handleResize = () => {
      setIsSidebarOpen(window.innerWidth >= 1400);
    };

    handleResize();

    window.addEventListener("resize", handleResize);
    return () => window.removeEventListener("resize", handleResize);
  }, []);

  const handleServiceChange = (service: string) => {
    setCurrentService(service);
    setPageState("select");
  };

  const handleMyServices = () => {
    setCurrentService("general-chat");
    setPageState("select");
  };

  const toggleSidebar = () => {
    setIsSidebarOpen((prev) => !prev);
  };

  return (
    <ServiceContext.Provider
      value={{
        currentService,
        setCurrentService: handleServiceChange,
        pageState,
        setPageState,
        isSidebarOpen,
        toggleSidebar,
        handleMyServices,
      }}
    >
      {children}
    </ServiceContext.Provider>
  );
}

export function useService() {
  const context = useContext(ServiceContext);
  if (context === undefined) {
    throw new Error("useService must be used within a ServiceProvider");
  }
  return context;
}
