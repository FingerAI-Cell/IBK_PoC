"use client";

import { CopilotKit } from "@copilotkit/react-core";
import "@copilotkit/react-ui/styles.css";
import { Chat } from "./chat";
import { useState } from "react";

export default function Page() {
  const [selectedAgent, setSelectedAgent] = useState<"olaf" | "summary">("olaf");

  const agentConfig = {
    olaf: {
      runtimeUrl: "/api/onelineai/olaf",
      agent: "olaf_ibk_poc_agent"
    },
    summary: {
      runtimeUrl: "/api/onelineai/olaf",
      agent: "olaf_ibk_poc_summary_agent"
    }
  };

  return (
    <main className="flex flex-col items-center min-h-screen w-full overflow-hidden bg-white">
      <div className="flex items-center space-x-4 my-4">
        <button
          className={`px-4 py-2 rounded-lg ${
            selectedAgent === "olaf"
              ? "bg-blue-500 text-white"
              : "bg-gray-200 text-gray-700"
          }`}
          onClick={() => setSelectedAgent("olaf")}
        >
          업무 챗봇
        </button>
        <button
          className={`px-4 py-2 rounded-lg ${
            selectedAgent === "summary"
              ? "bg-blue-500 text-white"
              : "bg-gray-200 text-gray-700"
          }`}
          onClick={() => setSelectedAgent("summary")}
        >
          회의록 요약
        </button>
      </div>

      <CopilotKit
        key={selectedAgent}
        runtimeUrl={agentConfig[selectedAgent].runtimeUrl}
        agent={agentConfig[selectedAgent].agent}
        showDevConsole={false}
      >
        <Chat agent={agentConfig[selectedAgent].agent} />
      </CopilotKit>
    </main>
  );
}
