// ServiceContext.tsx
"use client";

import { createContext, useState, useContext, useEffect } from "react";

interface ReportData {
  totalAmount: number;
  totalAmountDifference: number;
  totalAmountDifferencePercentage: number;
  investedAmount: number;
  portfolioData: {
    stocks: number;
    bondsFunds: number;
    derivatives: number;
    trust: number;
    others: number;
  };
  investmentDetails: {
    name: string;
    value: string;
    amount: string;
    dayBefore: string;
  }[];
  date: string;
  content: string;
}

interface ServiceContextType {
  currentService: string;
  setCurrentService: (service: string) => void;
  pageState: 'select' | 'chat' | 'admin';
  setPageState: (state: 'select' | 'chat' | 'admin') => void;
  handleMyServices: () => void;
  reportDate: string | null;
  reportData: ReportData | null;
  setReportDate: (date: string, data?: ReportData) => void;
  isSidebarOpen: boolean;
  toggleSidebar: () => void;
}

const defaultValue: ServiceContextType = {
  currentService: "general-chat",
  setCurrentService: () => {},
  pageState: "select",
  setPageState: () => {},
  handleMyServices: () => {},
  reportDate: null,
  reportData: null,
  setReportDate: () => {},
  isSidebarOpen: true,
  toggleSidebar: () => {},
};

const ServiceContext = createContext<ServiceContextType>(defaultValue);

export function ServiceProvider({ children }: { children: React.ReactNode }) {
  const [currentService, setCurrentService] = useState("general-chat");
  const [pageState, setPageState] = useState<'select' | 'chat' | 'admin'>('select');
  const [reportDate, setReportDate] = useState<string | null>(null);
  const [reportData, setReportData] = useState<ReportData | null>(null);
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);

  useEffect(() => {
    const handleResize = () => {
      setIsSidebarOpen(window.innerWidth >= 1400);
    };

    handleResize();

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

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

  const handleSetReportDate = (date: string, data?: ReportData) => {
    setReportDate(date);
    setReportData(data || null);
  };

  const toggleSidebar = () => {
    console.log('ServiceContext - Before Toggle:', isSidebarOpen);
    setIsSidebarOpen(!isSidebarOpen);
    console.log('ServiceContext - After Toggle:', !isSidebarOpen);
  };

  return (
    <ServiceContext.Provider 
      value={{ 
        currentService, 
        setCurrentService: handleServiceChange,
        pageState,
        setPageState,
        handleMyServices,
        reportDate,
        reportData,
        setReportDate: handleSetReportDate,
        isSidebarOpen,
        toggleSidebar,
      }}
    >
      {children}
    </ServiceContext.Provider>
  );
}

export function useService() {
  return useContext(ServiceContext);
}
