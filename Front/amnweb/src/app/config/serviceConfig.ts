interface ServiceConfig {
  apiEndpoint: string;
  title: string;
  defaultMessage: string;
  historyKey: string;
  greeting: string;
  agent?: string;
  useCopilot?: boolean;
}

export const apiConfig = {
  baseURL: 'https://ibkpoc.fingerservice.co.kr'  // 스프링 서버 주소
  // baseURL: 'http://ibkpass.fingerservice.co.kr:8080'
};

export const serviceConfig: { [key: string]: ServiceConfig } = {
  'general-chat': {
    apiEndpoint: '/api/onelineai/olaf',
    title: '일반 채팅',
    defaultMessage: '궁금하신 내용을 입력해주세요.',
    historyKey: 'chat-history-general',
    greeting: '금융 도우미에 오신 것을 환영합니다!',
    agent: 'olaf_ibk_poc_agent',
    useCopilot: true
  },
  'overseas-loan': {
    apiEndpoint: '/api/chat/overseas',
    title: '해외주식',
    defaultMessage: '해외주식에 대해 질문해주세요.',
    historyKey: 'chat-history-overseas',
    greeting: '해외주식 도우미입니다.'
  },
  'financial-statements': {
    apiEndpoint: '/api/chat/finance',
    title: '재무제표',
    defaultMessage: '재무제표에 대해 질문해주세요.',
    historyKey: 'chat-history-finance',
    greeting: '재무제표 도우미입니다.'
  },
  'branch-manual': {
    apiEndpoint: '/api/onelineai/olaf',
    title: '영업점 매뉴얼',
    defaultMessage: '영업점 매뉴얼에 대해 질문해주세요.',
    historyKey: 'chat-history-branch',
    greeting: '영업점 매뉴얼 도우미입니다.',
    agent: 'olaf_ibk_poc_agent',
    useCopilot: true
  },
  'meeting-minutes': {
    apiEndpoint: `${apiConfig.baseURL}/api/meetings`,
    title: '회의록',
    defaultMessage: '회의록에 대해 질문해주세요.',
    historyKey: 'chat-history-meeting',
    greeting: '회의록 도우미입니다.'
  },
  'investment-report': {
    apiEndpoint: '/api/chat/investment',
    title: '개인투자정보',
    defaultMessage: '개인투자정보에 대해 질문해주세요.',
    historyKey: 'chat-history-investment',
    greeting: '개인투자정보 도우미입니다.'
  }
};

export type ServiceType = keyof typeof serviceConfig; 