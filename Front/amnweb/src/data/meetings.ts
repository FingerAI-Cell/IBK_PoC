// export interface Meeting {
//   confId: number;
//   title: string;
//   startTime: string;
//   endTime: string;
//   duration: string;
//   participants: string[];
//   summary: string;
//   content: DialogueContent[];
// }

// export interface DialogueContent {
//   speaker: string;
//   text: string;
// }

// export const meetings: Meeting[] = [
//   {
//     confId: 1,
//     title: "회의 ID 1번",
//     startTime: "2024-12-12T14:00:00",
//     endTime: "2024-12-12T14:25:00",
//     duration: "25분",
//     participants: ["SPEAKER_00", "SPEAKER_01", "SPEAKER_02", "SPEAKER_03"],
//     summary: JSON.stringify({
//       topic: "생성형 AI 기술 동향과 적용 사례",
//       SPEAKER_00: { 
//         details: "문서 처리 성능 개선 방안과 ETL4 LLM 솔루션 소개. 다큐멘트 인텔리전스 기능 고도화 계획 공유."
//       },
//       SPEAKER_01: {
//         details: "2024년 AI 기술 전망 - 시퀀셜 에이전트, 멀티 에이전트, 멀티모달 LLM 발전 방향 논의."
//       }
//     }),
//     content: [
//       {
//         speaker: "SPEAKER_01",
//         text: "복잡한 테이블 같은 경우는 인식률이 좀 떨어지는 문제들이 있었어요..."
//       },
//       // ... seminar_20241212_1.json의 내용
//     ]
//   },
//   {
//     confId: 2,
//     title: "회의 ID 2번",
//     startTime: "2024-12-20T10:30:00",
//     endTime: "2024-12-20T10:45:00",
//     duration: "15분",
//     participants: ["SPEAKER_00", "SPEAKER_01", "SPEAKER_02"],
//     summary: JSON.stringify({
//       topic: "음성인식(STT) 시스템 구축 검토",
//       SPEAKER_00: {
//         details: "서버 구성 및 모델 셋업 상태 점검. 레그 연동 작업 진행 중."
//       },
//       SPEAKER_01: {
//         details: "모바일 앱 연동 방안 및 번역 기능 추가 가능성 검토."
//       }
//     }),
//     content: [
//       {
//         speaker: "SPEAKER_00",
//         text: "안녕하세요. 확인했는데 저희 지금 모델은 셋업이 됐고..."
//       },
//       // ... stt_ibk-poc-call-meeting_20241220.json의 내용
//     ]
//   },
//   {
//     confId: 3,
//     title: "회의 ID 3번",
//     startTime: "2024-12-27T10:07:15",
//     endTime: "2024-12-27T10:52:15",
//     duration: "45분",
//     participants: ["SPEAKER_00", "SPEAKER_01", "SPEAKER_02", "SPEAKER_03", "SPEAKER_04", "SPEAKER_05", "SPEAKER_06", "SPEAKER_07"],
//     summary: JSON.stringify({
//       topic: "LLMOpenAI 모델 gen_config 속성 에러 분석",
//       SPEAKER_00: {
//         details: "LLM 모델 초기화 과정에서 발생하는 gen_config 속성 관련 에러 원인 파악"
//       },
//       SPEAKER_01: {
//         details: "기존 코드와의 호환성 문제 및 버전 업그레이드에 따른 영향도 검토"
//       },
//       SPEAKER_02: {
//         details: "임시 해결방안으로 이전 버전 사용 제안 및 장기적인 개선 방안 논의"
//       }
//     }),
//     content: [
//       {
//         speaker: "SPEAKER_00",
//         text: "안녕하세요. 오늘은 LLM 모델에서 발생하는 gen_config 속성 에러에 대해 논의하도록 하겠습니다."
//       },
//       {
//         speaker: "SPEAKER_01",
//         text: "네, 현재 발생하는 에러는 'LLMOpenAI' 객체에서 gen_config 속성을 찾을 수 없다는 내용입니다."
//       },
//       {
//         speaker: "SPEAKER_02",
//         text: "이 문제는 최근 업데이트된 버전에서 API 인터페이스가 변경되면서 발생한 것으로 보입니다."
//       },
//       // ... 중간 대화 생략 ...
//       {
//         speaker: "SPEAKER_03",
//         text: "당장은 이전 버전으로 롤백하고, 새 버전 호환성 테스트를 진행하는 것이 좋을 것 같습니다."
//       },
//       {
//         speaker: "SPEAKER_04",
//         text: "동의합니다. 버전 롤백과 함께 새로운 API 스펙 검토도 필요해 보입니다."
//       }
//     ]
//   },
//   // ... 나머지 3개 회의 데이터도 동일한 형식으로 구성
// ]; 