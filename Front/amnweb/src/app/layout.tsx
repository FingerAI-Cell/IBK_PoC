import "./globals.css";
import { ServiceProvider } from "./context/ServiceContext";
import ClientLayout from "./ClientLayout";

export default function RootLayout() {
  return (
    <html lang="en">
      <body className="flex min-h-screen bg-gray-100">
        <div id="modal-root" />
        <ServiceProvider>
          <ClientLayout />
        </ServiceProvider>
      </body>
    </html>
  );
}