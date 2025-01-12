import { readdir, stat } from 'fs/promises';
import { join } from 'path';
import path from 'path';

export async function GET() {
  try {
    // AMNWEB/Scheduler 경로로 이동 (현재 위치에서 ../../.. 올라가기)
    const schedulerPath = path.join(process.cwd(), '..', '..', 'Scheduler');
    
    // 각 카테고리 디렉토리 읽기
    const categories = [
      'financial_condition',
      'financial_ratio',
      'investment_company_announcement',
      'manpower_status',
      'organization_structure'
    ];

    const allFiles = [];
    
    for (const category of categories) {
      const dirPath = join(schedulerPath, category);
      const files = await readdir(dirPath);
      
      const fileDetails = await Promise.all(
        files.map(async (fileName) => {
          const filePath = join(dirPath, fileName);
          const stats = await stat(filePath);
          
          // 파일명에서 년도와 분기 추출 (예: 2024Q1_financial_condition.xlsx)
          const yearQuarter = fileName.match(/(\d{4})Q(\d)/);
          
          return {
            id: `${category}_${fileName}`,
            fileName: fileName,
            updateTime: stats.mtime.toLocaleString(),
            category: getCategoryKorean(category),
            filePath: `/api/financial-documents/download?path=${encodeURIComponent(filePath)}`,
            yearQuarter: yearQuarter ? `${yearQuarter[1]}년 ${yearQuarter[2]}분기` : '',
            timestamp: stats.mtime.getTime()
          };
        })
      );
      
      allFiles.push(...fileDetails);
    }
    
    // 분기 정렬을 위한 함수
    const sortByQuarter = (a: any, b: any) => {
      // 파일명에서 년도와 분기 추출 (예: 2024Q1_financial_condition.xlsx)
      const getYearQuarter = (fileName: string) => {
        const match = fileName.match(/(\d{4})Q(\d)/);
        if (match) {
          return {
            year: parseInt(match[1]),
            quarter: parseInt(match[2])
          };
        }
        return { year: 0, quarter: 0 };
      };

      const aYQ = getYearQuarter(a.fileName);
      const bYQ = getYearQuarter(b.fileName);

      // 년도 비교
      if (aYQ.year !== bYQ.year) {
        return bYQ.year - aYQ.year; // 내림차순
      }
      // 같은 년도면 분기 비교
      return bYQ.quarter - aYQ.quarter; // 내림차순
    };

    // 분기 기준으로 정렬
    allFiles.sort(sortByQuarter);

    return Response.json(allFiles);
  } catch (error) {
    console.error('파일 목록 조회 실패:', error);
    return Response.json({ error: '파일 목록을 불러오는데 실패했습니다.' }, { status: 500 });
  }
}

function getCategoryKorean(category: string): string {
  const categoryMap: { [key: string]: string } = {
    'manpower_status': '인력현황',
    'financial_condition': '주요재무현황',
    'financial_ratio': '주요재무비율',
    'investment_company_announcement': '금융투자회사공시검색',
    'organization_structure': '조직기구현황'
  };
  return categoryMap[category] || category;
} 