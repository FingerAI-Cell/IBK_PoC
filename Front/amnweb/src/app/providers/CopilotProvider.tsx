"use client";

import { CopilotKit } from "@copilotkit/react-core";
import { useService } from "../context/ServiceContext";
import { serviceConfig } from "../config/serviceConfig";
import { useChat } from "../context/ChatContext";

export function CopilotProvider({ children }: { children: React.ReactNode }) {
  const { currentService } = useService();
  const { isChatActive } = useChat();
  const config = serviceConfig[currentService];

  // isChatActive가 true일 때만 CopilotKit을 렌더링
  if (!isChatActive) {
    return <>{children}</>;
  }

  return (
    <CopilotKit
      runtimeUrl={config.apiEndpoint}
      agent={config.agent}
    >
      {children}
    </CopilotKit>
  );
} 