import { NextResponse } from 'next/server';
import { apiConfig } from '@/app/config/serviceConfig';

export async function POST(request: Request) {
  try {
    const body = await request.json();
    
    const response = await fetch(`${apiConfig.baseURL}/api/meetings/summarize`, {
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
    return NextResponse.json(data);
  } catch (error) {
    console.error('Summarize API error:', error);
    return NextResponse.json({ success: false }, { status: 200 });
  }
} 