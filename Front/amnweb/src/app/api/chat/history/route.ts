import { NextResponse } from 'next/server';

const API_BASE_URL = process.env.NODE_ENV === 'production' 
  ? process.env.NEXT_PUBLIC_LOCAL_API_URL
  : process.env.NEXT_PUBLIC_API_BASE_URL ;

export async function GET(request: Request) {
  const { searchParams } = new URL(request.url);
  const dateFrom = searchParams.get('date_from');
  const dateTo = searchParams.get('date_to');

  try {
    const response = await fetch(
      `${API_BASE_URL}/chat/history?date_from=${dateFrom}&date_to=${dateTo}`,
      {
        headers: {
          'accept': 'application/json'
        }
      }
    );

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    return NextResponse.json({ error: '데이터 조회 실패' }, { status: 500 });
  }
} 