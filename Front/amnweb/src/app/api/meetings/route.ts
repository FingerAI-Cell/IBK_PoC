import { NextResponse } from 'next/server';

const API_BASE_URL = process.env.NODE_ENV === 'production' 
  ? process.env.NEXT_PUBLIC_FINGER_URL
  : process.env.NEXT_PUBLIC_EC2_URL;


export async function GET() {
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 5000); // 5초 타임아웃
// 이 부분 수정중
    const response = await fetch(`${API_BASE_URL}/api/meetings/`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
      signal: controller.signal
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      console.error(`API Response Status: ${response.status}`);
      console.error(`API Response URL: ${API_BASE_URL}/api/meetings/`);
      throw new Error('HTTP error! status: ' + response.status);
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Meetings API error:', error);
    return NextResponse.json([], { status: 200 });
  }
} 