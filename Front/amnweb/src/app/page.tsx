"use client";

import { useService } from "./context/ServiceContext";

export default function Home() {
  const { currentService } = useService();

  return (
    <div className="min-h-screen bg-gray-100 p-6">
      <div className="bg-white p-6 rounded shadow">
        {currentService === "default" ? (
          <p></p>
        ) : (
          <p>{currentService} </p>
        )}
      </div>
    </div>
  );
}
