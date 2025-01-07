import { NextRequest } from "next/server";
import {
  CopilotRuntime,
  OpenAIAdapter,
  copilotRuntimeNextJSAppRouterEndpoint,
} from "@copilotkit/runtime";
import OpenAI from "openai";

// OpenAIAdapterConfig 인터페이스 정의
interface OpenAIAdapterConfig {
  openai: OpenAI;
}

const openai = new OpenAI({ apiKey: "abcd" });
const serviceAdapter = new OpenAIAdapter({ openai } as OpenAIAdapterConfig);

const runtime = new CopilotRuntime({
  remoteActions: [
    {
      url: process.env.REMOTE_OLAF_URL || "http://localhost:8000/olaf",
    },
  ],
});

export const POST = async (req: NextRequest) => {
  const { handleRequest } = copilotRuntimeNextJSAppRouterEndpoint({
    runtime,
    serviceAdapter,
    endpoint: "/api/onelineai/olaf",
  });

  return handleRequest(req);
};
