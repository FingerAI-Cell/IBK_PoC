"use client";

import { useService } from "./context/ServiceContext";

export default function Home() {
  const { currentService } = useService();

  return (
    <div className="min-h-screen bg-gray-100 p-6">
      <div className="bg-white p-6 rounded shadow">
        {currentService === "default" ? (
          <p>서비스를 선택해주세요.</p>
        ) : (
          <p>{currentService} 콘텐츠</p>
        )}
      </div>
    </div>
  );
}
