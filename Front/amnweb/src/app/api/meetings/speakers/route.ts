import { NextResponse } from 'next/server';

const API_BASE_URL = process.env.NODE_ENV === 'production' 
  ? process.env.NEXT_PUBLIC_FINGER_URL
  : process.env.NEXT_PUBLIC_MEETING_URL_DEV;

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 5000);

    const response = await fetch(`${API_BASE_URL}/api/meetings/speakers`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
      signal: controller.signal
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      throw new Error('HTTP error! status: ' + response.status);
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Speakers API error:', error);
    return NextResponse.json({ success: false }, { status: 200 });
  }
} 