import { createReadStream } from 'fs';
import { stat } from 'fs/promises';
import { NextRequest } from 'next/server';
import path from 'path';

export async function GET(request: NextRequest) {
  try {
    const searchParams = request.nextUrl.searchParams;
    const filePath = searchParams.get('path');

    if (!filePath) {
      return new Response('파일 경로가 필요합니다.', { status: 400 });
    }

    // 파일 존재 여부 확인
    const stats = await stat(filePath);
    if (!stats.isFile()) {
      return new Response('파일을 찾을 수 없습니다.', { status: 404 });
    }

    // 파일명만 추출 (경로 제외)
    const fileName = path.basename(filePath);

    // 파일 스트림 생성
    const fileStream = createReadStream(filePath);

    // 응답 헤더 설정
    const headers = new Headers();
    headers.set('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
    headers.set('Content-Disposition', `attachment; filename=${encodeURIComponent(fileName)}`);

    return new Response(fileStream as any, {
      headers,
    });
  } catch (error) {
    console.error('파일 다운로드 실패:', error);
    return new Response('파일 다운로드에 실패했습니다.', { status: 500 });
  }
} 