import { NextResponse } from 'next/server';
import { apiConfig } from '@/app/config/serviceConfig';

export async function GET() {
  try {
    const response = await fetch(`${apiConfig.baseURL}/api/meetings`, {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      }
    });

    if (!response.ok) {
      console.error(`API Response Status: ${response.status}`);
      console.error(`API Response URL: ${apiConfig.baseURL}/api/meetings`);
      throw new Error('HTTP error! status: ' + response.status);
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Meetings API error:', error);
    return NextResponse.json([], { status: 200 });
  }
} 