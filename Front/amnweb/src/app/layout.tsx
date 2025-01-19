"use client";

import "./globals.css";
import { ServiceProvider } from "./context/ServiceContext";
import { ChatProvider } from "./context/ChatContext";
import { CopilotProvider } from "./providers/CopilotProvider";
import ClientLayout from "./ClientLayout";

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="ko">
      <body className="flex min-h-screen bg-gray-100">
        <div id="modal-root" />
        <ServiceProvider>
          <ChatProvider>
            <CopilotProvider>
              <ClientLayout>
                {children}
              </ClientLayout>
            </CopilotProvider>
          </ChatProvider>
        </ServiceProvider>
      </body>
    </html>
  );
}