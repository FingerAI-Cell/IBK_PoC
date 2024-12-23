export default function MeetingMinutes() {
    return (
      <div>
        <h2 className="text-2xl font-bold mb-4">회의록 자동 작성</h2>
        <p>회의 내용을 자동으로 요약하고 회의록을 생성합니다.</p>
        <ul className="list-disc pl-6 mt-4">
          <li>음성 인식 기반 텍스트 생성</li>
          <li>요약 및 중요 내용 추출</li>
          <li>PDF 또는 Word 형식으로 내보내기</li>
        </ul>
      </div>
    );
  }
  