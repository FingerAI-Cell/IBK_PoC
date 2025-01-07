import { NextRequest, NextResponse } from "next/server";
import path from "path";
import fs from "fs";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const fileName = searchParams.get("file");

  if (!fileName) {
    return NextResponse.json(
      { error: "파일명이 제공되지 않았습니다." },
      { status: 400 }
    );
  }

  try {
    const filePath = path.join(process.cwd(), "public", "static", "documents", "manual", fileName);

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