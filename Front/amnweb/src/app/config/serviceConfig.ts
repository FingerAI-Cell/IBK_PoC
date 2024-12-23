interface ServiceConfig {
  apiEndpoint: string;
  title: string;
  defaultMessage: string;
  historyKey: string;
}

export const serviceConfig: { [key: string]: ServiceConfig } = {
  'general-chat': {
    apiEndpoint: '/api/chat/general',
    title: '일반 채팅',
    defaultMessage: '무엇을 도와드릴까요?',
    historyKey: 'chat-history-general'
  },
  'overseas-loan': {
    apiEndpoint: '/api/chat/overseas',
    title: '해외주식',
    defaultMessage: '해외주식 관련 문의사항을 입력해주세요.',
    historyKey: 'chat-history-overseas'
  },
  'financial-statements': {
    apiEndpoint: '/api/chat/finance',
    title: '재무제표',
    defaultMessage: '재무제표 관련 문의사항을 입력해주세요.',
    historyKey: 'chat-history-finance'
  },
  'branch-manual': {
    apiEndpoint: '/api/chat/branch',
    title: '영업점 매뉴얼',
    defaultMessage: '영업점 매뉴얼 관련 문의사항을 입력해주세요.',
    historyKey: 'chat-history-branch'
  },
  'meeting-minutes': {
    apiEndpoint: '/api/chat/meeting',
    title: '회의록',
    defaultMessage: '회의록 관련 문의사항을 입력해주세요.',
    historyKey: 'chat-history-meeting'
  },
  'investment-report': {
    apiEndpoint: '/api/chat/investment',
    title: '개인투자정보',
    defaultMessage: '투자 정보 관련 문의사항을 입력해주세요.',
    historyKey: 'chat-history-investment'
  }
};

export type ServiceType = keyof typeof serviceConfig; 