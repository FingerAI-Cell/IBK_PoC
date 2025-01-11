"use client";

import "./globals.css";
import Header from "./components/Header/Header";
import Sidebar from "./components/Sidebar/Sidebar";
import { ServiceProvider, useService } from "./context/ServiceContext";
import MainContent from "./components/MainContent/MainContent";

function LayoutContent({ children }: { children: React.ReactNode }) {
  const { currentService, setCurrentService } = useService();
  
  return (
    <div className="flex min-h-screen bg-gray-100">
      <Sidebar 
        currentService={currentService}
        selectService={setCurrentService}
      />
      <div className="flex-grow flex flex-col ml-64">
        <Header />
        <main className="p-6 mt-16">
          <MainContent />
          {children}
        </main>
      </div>
    </div>
  );
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <ServiceProvider>
        <body>
          <LayoutContent>{children}</LayoutContent>
        </body>
      </ServiceProvider>
    </html>
  );
}