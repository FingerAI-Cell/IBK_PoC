import { NextResponse } from 'next/server';
import { apiConfig } from '@/app/config/serviceConfig';

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 5000);

    const response = await fetch(`${apiConfig.baseURL}/api/meetings/logs`, {
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
    console.error('Logs API error:', error);
    return NextResponse.json({ success: false }, { status: 200 });
  }
} 