import { NextRequest } from "next/server";
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
      url: process.env.REMOTE_OLAF_URL || "http://localhost:8000/olaf",
    },
  ],
});

export const POST = async (req: NextRequest) => {
  const { handleRequest } = copilotRuntimeNextJSAppRouterEndpoint({
    runtime,
    serviceAdapter,
    endpoint: "/api/onelineai/olaf",
    // @ts-expect-error - copilotkit의 타입 정의에는 없지만 실제로 지원되는 옵션
    preprocessRequest: async (request: Request) => {
      console.log("preprocessRequest triggered");
      const body = await request.json();
      console.log("Request Body:", body);
      const serviceContext: ServiceContext = {
        service: body.service || 'general-chat',
        agent: body.agent || 'olaf_ibk_poc_agent'
      };
      
      // 로깅 추가 (필요시)
      console.log('Service Context:', serviceContext);
      // 현재 요청의 URL 로깅
      console.log('Incoming Request URL:', request.url);
      // 원본 요청에 서비스 컨텍스트 추가
      return new Request(request.url, {
        method: request.method,
        headers: request.headers,
        body: JSON.stringify({
          ...body,
          context: {
            ...body.context,
            service: serviceContext.service,
            agent: serviceContext.agent
          }
        })
      });
    },
    postprocessResponse: async (response: Response) => {
      return response;
    }
  });

  return handleRequest(req);
};
