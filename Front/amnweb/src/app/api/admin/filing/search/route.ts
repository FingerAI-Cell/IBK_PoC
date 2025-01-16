import { NextRequest, NextResponse } from 'next/server';

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const fromDate = searchParams.get('from_date');
    const toDate = searchParams.get('to_date');

    if (!fromDate || !toDate) {
      return NextResponse.json(
        { error: '날짜 파라미터가 필요합니다.' },
        { status: 400 }
      );
    }

    const isDevelopment = process.env.NODE_ENV === 'development';
    const isProduction = process.env.NODE_ENV === 'production';
    
    let baseUrl: string;
    
    if (isDevelopment) {
      baseUrl = process.env.NEXT_PUBLIC_EC2_URL!;
    } else if (isProduction) {
      baseUrl = process.env.NEXT_PUBLIC_LOCAL_API_URL!;
    } else {
      throw new Error('알 수 없는 환경입니다.');
    }

    if (!baseUrl) {
      throw new Error('API Base URL이 설정되지 않았습니다.');
    }

    const response = await fetch(
      `${baseUrl}/filing/search/by-tickers?from_date=${fromDate}&to_date=${toDate}`,
      {
        headers: {
          'Content-Type': 'application/json',
        },
      }
    );

    if (response.status === 404) {
      return NextResponse.json(
        { error: '요청하신 데이터를 찾을 수 없습니다.' },
        { status: 404 }
      );
    }

    if (!response.ok) {
      throw new Error(`API 요청 실패: ${response.status}`);
    }

    const data = await response.json();

    // 데이터를 기존 형식에 맞게 변환
    const transformedData = {
      summary: [
        data.reduce((acc: any, item: any) => {
          if (!acc[item.ticker]) {
            acc[item.ticker] = [];
          }

          const existingEntry = acc[item.ticker].find(
            (entry: any) => entry.form === item.form && entry.date === item.filing_date
          );

          if (existingEntry) {
            existingEntry.matching_sections[item.section_name] = {
              keywords_found: {
                bankruptcy: item.bankruptcy ? 'O' : 'X',
                merger: item.merger ? 'O' : 'X',
                // 필요한 다른 키워드들도 추가
              },
              text_ko_summary: item.summary
            };
          } else {
            acc[item.ticker].push({
              form: item.form,
              date: item.filing_date,
              matching_sections: {
                [item.section_name]: {
                  keywords_found: {
                    bankruptcy: item.bankruptcy ? 'O' : 'X',
                    merger: item.merger ? 'O' : 'X',
                    // 필요한 다른 키워드들도 추가
                  },
                  text_ko_summary: item.summary
                }
              }
            });
          }

          return acc;
        }, {})
      ]
    };

    return NextResponse.json(transformedData);
  } catch (error) {
    console.error('파일링 검색 데이터 조회 실패:', error);
    return NextResponse.json(
      { error: '서버 내부 오류가 발생했습니다.' },
      { status: 500 }
    );
  }
} 