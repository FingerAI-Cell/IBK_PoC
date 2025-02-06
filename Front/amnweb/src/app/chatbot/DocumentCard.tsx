import React from 'react';

export interface DocumentMetadata {
  page_number: number;
  page_chunk_number: number;
  file_name: string;
  file_url: string;
  main_topic: string;
  sub_topic: string;
  keywords: string[];
}

interface DocumentItemProps {
  metadata: DocumentMetadata;
}

export const DocumentCard: React.FC<DocumentItemProps> = ({ metadata }) => {
  // 키워드를 해시태그로 변환하는 함수
  function convertHashtags(keywords: string[]) {
    return keywords.map(keyword => `#${keyword}`).join(" ");
  }

  // 파일 다운로드 핸들러
  const handleDownload = async () => {
    // 환경 변수에 따라 URL 생성

    // 절대 경로로 요청 URL 생성
    const processedFileName = metadata.file_name.replace(/,/g, "%2C").replace(/&/g, "%26");
    console.log(`processedFileName:${processedFileName}`);
    const downloadUrl = `/api/onelineai/download?file=${encodeURIComponent(processedFileName)}`;
    console.log(`downloadUrl:${downloadUrl}`);
    const link = document.createElement("a");
    link.href = downloadUrl;
    link.download = metadata.file_name; // 다운로드 파일명 설정
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div>
      <div className="border rounded-lg p-4 shadow-md my-4">
        {/* 파일 정보 */}
        <h3 className="text-lg font-semibold">
          파일명: {metadata.file_name}
        </h3>
        <p className="text-sm text-blue-500">
          {metadata.main_topic} - {metadata.sub_topic} [{convertHashtags(metadata.keywords)}]
        </p>
        <p className="text-sm text-gray-600">
          페이지 번호: {metadata.page_number}
        </p>
        <p className="text-sm text-gray-600">
          페이지 내 파트 번호: {metadata.page_chunk_number}
        </p>

        {/* 문서 열기 및 다운로드 버튼 */}
        <div className="mt-4 flex gap-4">
          <a
            href={metadata.file_url}
            target="_blank"
            rel="noopener noreferrer"
            className="text-blue-700 underline"
          >
            문서 열기
          </a>
          <button
            onClick={handleDownload}
            className="bg-blue-500 text-white px-4 py-2 rounded-lg shadow hover:bg-blue-600"
          >
            문서 다운로드
          </button>
        </div>
      </div>

      {/* 주요 주제 및 부 주제 */}
      <div className="text-sm text-gray-700">
        <p>
          <strong>📄 주요 주제:</strong> {metadata.main_topic}
        </p>
        <p>
          <strong>📑 부 주제:</strong> {metadata.sub_topic}
        </p>
      </div>

      {/* 키워드 목록 */}
      <div className="flex flex-wrap gap-2 mt-2">
        {metadata.keywords.map((keyword, index) => (
          <span
            key={index}
            className="text-xs bg-gray-100 border rounded px-2 py-1"
          >
            {keyword}
          </span>
        ))}
      </div>

      {/* 페이지 및 청크 정보 */}
      <div className="text-sm text-gray-500 mt-2">
        <p>
          📄 페이지: {metadata.page_number} (Chunk {metadata.page_chunk_number})
        </p>
      </div>
    </div>
  );
};