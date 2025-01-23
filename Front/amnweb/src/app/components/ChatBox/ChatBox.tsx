"use client";

import { useRef} from "react";
import { CopilotKit } from "@copilotkit/react-core";
import "@copilotkit/react-ui/styles.css";
import styles from "./ChatBox.module.css";
// import { InputProps } from "@copilotkit/react-ui";
import { useService } from "@/app/context/ServiceContext";
import Chat from "../Chat/chat";
import { serviceConfig } from "@/app/config/serviceConfig";

export default function ChatBox() {
  const currentService = useService().currentService;
  const chatContainerRef = useRef<HTMLDivElement>(null);
  const currentConfig = serviceConfig[currentService];
  console.log("ddd");
  return (
    <CopilotKit 
      runtimeUrl={currentConfig.apiEndpoint}
      agent={currentConfig.agent}
      showDevConsole={false}
    >
      <div ref={chatContainerRef} className={styles.chatWrapper}>
        <div className={styles.container}>
          <Chat/>
        </div>
      </div>
    </CopilotKit>
  );
}
