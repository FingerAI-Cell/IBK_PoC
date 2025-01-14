import { NextResponse } from 'next/server';

const API_BASE_URL = process.env.NODE_ENV === 'production' 
  ? process.env.NEXT_PUBLIC_FINGER_URL
  : process.env.NEXT_PUBLIC_EC2_URL;

export async function POST(request: Request) {
  try {
    const body = await request.json();
    
    const response = await fetch(`${API_BASE_URL}/personal/market-summary`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
    
    // 데이터 유효성 검사
    if (!Array.isArray(data)) {
      return NextResponse.json([], { status: 200 });
    }

    return NextResponse.json(data);
  } catch (error) {
    console.error('Market summary API error:', error);
    return NextResponse.json([], { status: 200 }); // 에러 시 빈 배열 반환
  }
} 