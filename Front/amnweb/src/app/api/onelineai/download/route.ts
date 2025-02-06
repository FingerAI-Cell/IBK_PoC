import { NextRequest, NextResponse } from "next/server";
import path from "path";
import fs from "fs";

// 환경 변수에서 API URL 설정
const baseUrl =
  process.env.NODE_ENV === "production"
    ? process.env.NEXT_PUBLIC_LOCAL_API_URL // 프로덕션 환경
    : process.env.REMOTE_OLAF_URL; // 개발 환경

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const fileName = searchParams.get("file");
    
  console.log("Environment:", process.env.NODE_ENV); // 현재 환경 확인
  console.log("Base URL:", baseUrl); // URL 확인
  console.log("Requested file:", fileName); // 요청된 파일명 확인
  process.stdout.write(`\n🔍 [API 호출됨] 파일명: ${fileName}\n`); // 🚀 강제 출력

  if (!fileName) {
    return NextResponse.json(
      { error: "파일명이 제공되지 않았습니다." },
      { status: 400 }
    );
  }

  try {
    const filePath = path.join(process.cwd(), "public", "static", "documents", "manual", fileName);
    console.log(`filePath:${filePath}`);
    if (!fs.existsSync(filePath)) {
      return NextResponse.json(
        { error: "파일을 찾을 수 없습니다." },
        { status: 404 }
      );
    }

    const file = fs.readFileSync(filePath);
    const encodedFileName = encodeURIComponent(fileName);

    const response = new NextResponse(file);
    response.headers.set("Content-Type", "application/octet-stream");
    
    // 한글 파일명 인코딩 처리
    response.headers.set(
      "Content-Disposition",
      `attachment; filename*=UTF-8''${encodedFileName}`
    );

    return response;
  } catch (error) {
    console.error("파일 다운로드 중 오류 발생:", error);
    return NextResponse.json(
      { error: "파일 다운로드 중 문제가 발생했습니다." },
      { status: 500 }
    );
  }
}