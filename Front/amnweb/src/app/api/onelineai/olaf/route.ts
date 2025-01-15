import { NextRequest, NextResponse } from "next/server";
import {
  CopilotRuntime,
  OpenAIAdapter,
  copilotRuntimeNextJSAppRouterEndpoint,
} from "@copilotkit/runtime";
import OpenAI from "openai";

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization, x-copilotkit-runtime-client-gql-version',
};

const openai = new OpenAI({apiKey: "abcd"});
const serviceAdapter = new OpenAIAdapter({ openai } as any);

// 환경에 따른 URL 설정
const baseUrl = process.env.NODE_ENV === 'production'
  ? process.env.NEXT_PUBLIC_LOCAL_API_URL
  : process.env.REMOTE_OLAF_URL;

const runtime = new CopilotRuntime({
  remoteActions: [
    {
      url: baseUrl+"/olaf",
    },
  ],
});

export async function OPTIONS() {
  return NextResponse.json({}, { headers: corsHeaders });
}

export async function POST(req: NextRequest) {
  console.log(`Current environment: ${process.env.NODE_ENV}`);
  console.log(`Using URL: ${baseUrl}`);

  const { handleRequest } = copilotRuntimeNextJSAppRouterEndpoint({
    runtime,
    serviceAdapter,
    endpoint: "/api/onelineai/olaf",
  });

  const response = await handleRequest(req);
  
  // CORS 헤더 추가
  Object.entries(corsHeaders).forEach(([key, value]) => {
    response.headers.set(key, value);
  });

  return response;
}
