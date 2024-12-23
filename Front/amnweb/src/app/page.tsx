"use client";

import { useState } from "react";

export default function Home() {
  const [currentService, setCurrentService] = useState("default");

  return (
    <div className="min-h-screen bg-gray-100 p-6">
      {/* 서비스 전환 버튼 */}
      <nav className="space-x-4 mb-4">
        <button
          className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
          onClick={() => setCurrentService("service1")}
        >
          Service 1
        </button>
        <button
          className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
          onClick={() => setCurrentService("service2")}
        >
          Service 2
        </button>
      </nav>

      {/* 동적 콘텐츠 */}
      <div className="bg-white p-6 rounded shadow">
        {currentService === "default" ? (
          <p>Select a service to continue.</p>
        ) : currentService === "service1" ? (
          <p>Service 1 Content</p>
        ) : currentService === "service2" ? (
          <p>Service 2 Content</p>
        ) : null}
      </div>
    </div>
  );
}
