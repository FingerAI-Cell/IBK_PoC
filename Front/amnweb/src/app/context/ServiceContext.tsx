// ServiceContext.tsx
"use client";

import { createContext, useState, useContext, useEffect } from "react";

interface ReportData {
  totalAmount: number;
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
};

const ServiceContext = createContext<ServiceContextType>(defaultValue);

export function ServiceProvider({ children }: { children: React.ReactNode }) {
  const [currentService, setCurrentService] = useState("general-chat");
  const [pageState, setPageState] = useState<'select' | 'chat' | 'admin'>('select');
  const [reportDate, setReportDate] = useState<string | null>(null);
  const [reportData, setReportData] = useState<ReportData | null>(null);

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
      }}
    >
      {children}
    </ServiceContext.Provider>
  );
}

export function useService() {
  return useContext(ServiceContext);
}
