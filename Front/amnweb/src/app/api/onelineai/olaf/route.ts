import { NextRequest, NextResponse } from "next/server";
import {
  CopilotRuntime,
  OpenAIAdapter,
  copilotRuntimeNextJSAppRouterEndpoint,
} from "@copilotkit/runtime";
import OpenAI from "openai";

interface OpenAIAdapterConfig {
  openai: OpenAI;
}

// 서비스별 설정을 위한 인터페이스
interface ServiceContext {
  service: string;
  agent: string;
}

const openai = new OpenAI({ apiKey: "abcd" });
const serviceAdapter = new OpenAIAdapter({ openai } as OpenAIAdapterConfig);

const runtime = new CopilotRuntime({
  remoteActions: [
    {
      url: `${process.env.REMOTE_OLAF_URL || "http://localhost:8000"}api/onelineai/olaf`,
    },
  ],
});


export const POST = async (req: NextRequest) => {
  console.log("POST handler triggered");
  console.log("Incoming Request:", req);

  // CORS 헤더 설정
  const corsHeaders = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
  };

  // OPTIONS 요청 처리
  if (req.method === 'OPTIONS') {
    return NextResponse.json({}, { headers: corsHeaders });
  }

  const { handleRequest } = copilotRuntimeNextJSAppRouterEndpoint({
    runtime,
    serviceAdapter,
    endpoint: "/api/onelineai/olaf",
  });

  const response = await handleRequest(req);
  
  // 응답에 CORS 헤더 추가
  Object.entries(corsHeaders).forEach(([key, value]) => {
    response.headers.set(key, value);
  });

  return response;
};

// OPTIONS 메서드 핸들러 추가
export const OPTIONS = async (req: NextRequest) => {
  return NextResponse.json({}, {
    headers: {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    },
  });
};
