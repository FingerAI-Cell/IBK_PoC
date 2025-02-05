import { NextRequest, NextResponse } from 'next/server';

export async function GET(request: NextRequest) {
  try {
    // URL에서 쿼리 매개변수 추출
    const { searchParams } = new URL(request.url);
    const from_date = searchParams.get('from_date');
    const to_date = searchParams.get('to_date');

    // 필수 매개변수 검증
    if (!from_date || !to_date) {
      return NextResponse.json(
        { error: 'from_date와 to_date 매개변수는 필수입니다.' },
        { status: 400 }
      );
    }

    const environment = process.env.NODE_ENV;
    const baseUrl =
      environment === 'development'
        ? process.env.NEXT_PUBLIC_API_BASE_URL
        : environment === 'production'
        ? process.env.NEXT_PUBLIC_LOCAL_API_URL
        : null;

    if (!baseUrl) {
      throw new Error('API Base URL이 설정되지 않았습니다.');
    }

    console.log(`Environment: ${environment}`);
    console.log(`Using Base URL: ${baseUrl}`);

    const apiUrl = `${baseUrl}/admin/filing/overview/range?from_date=${from_date}&to_date=${to_date}`;
    console.log(`Fetching API URL: ${apiUrl}`);

    // 외부 API 호출
    const response = await fetch(apiUrl, {
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      const errorMessage =
        response.status === 404
          ? '요청하신 데이터를 찾을 수 없습니다.'
          : `API 요청 실패: 상태 코드 ${response.status}`;
      console.error(errorMessage);

      return NextResponse.json(
        { error: errorMessage },
        { status: response.status }
      );
    }

    const data = await response.json();
    console.log('API 응답 데이터:', data);

    return NextResponse.json(data);
  } catch (error: any) {
    console.error('파일링 데이터 조회 실패:', error.message);

    return NextResponse.json(
      { error: '서버 내부 오류가 발생했습니다.' },
      { status: 500 }
    );
  }
}
