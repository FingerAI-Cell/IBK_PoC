import { NextRequest, NextResponse } from 'next/server';

export async function GET(
  request: NextRequest,
  { params }: { params: { date: string } }
) {
  try {
    const isDevelopment = process.env.NODE_ENV === 'development';
    const isProduction = process.env.NODE_ENV === 'production';
    
    let baseUrl: string;
    
    if (isDevelopment) {
      baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL!; // 외부 API
    } else if (isProduction) {
      baseUrl = process.env.NEXT_PUBLIC_LOCAL_API_URL!; // 로컬 API
    } else {
      throw new Error('알 수 없는 환경입니다.');
    }

    if (!baseUrl) {
      throw new Error('API Base URL이 설정되지 않았습니다.');
    }

    console.log(`Current environment: ${process.env.NODE_ENV}`);
    console.log(`Using API URL: ${baseUrl}`);

    const response = await fetch(
      `${baseUrl}/admin/filing/${params.date}`,
      {
        headers: {
          'Content-Type': 'application/json',
        },
      }
    );

    console.log(`Response status: ${response.status}`);

    if (response.status === 404) {
      console.log('404 상태 코드: 요청하신 데이터를 찾을 수 없습니다.');
      return NextResponse.json(
        { error: '요청하신 데이터를 찾을 수 없습니다.' },
        { status: 404 }
      );
    }

    if (!response.ok) {
      throw new Error(`API 요청 실패: ${response.status}`);
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('파일링 데이터 조회 실패:', error);
    return NextResponse.json(
      { error: '서버 내부 오류가 발생했습니다.' },
      { status: 500 }
    );
  }
} 