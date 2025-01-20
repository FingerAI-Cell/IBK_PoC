import { NextResponse } from 'next/server';

const API_BASE_URL = process.env.NODE_ENV === 'production' 
  ? process.env.NEXT_PUBLIC_FINGER_URL
  : process.env.NEXT_PUBLIC_MEETING_URL_DEV;

export async function POST(request: Request) {
  try {
    const { meetingId } = await request.json();
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 5000);

    const response = await fetch(`${API_BASE_URL}/api/meetings/${meetingId}/summary`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
      signal: controller.signal
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      throw new Error('HTTP error! status: ' + response.status);
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Summary API error:', error);
    return NextResponse.json({ success: false, error: '요약 데이터를 불러오는데 실패했습니다.' });
  }
} 