import os
import shutil
import psycopg2
import json
from openpyxl import load_workbook
from src.database import DBConnection  # DB 연결 클래스 사용

# 재무 항목 매핑
COLUMN_MAPPING = {
    "total_equity": ("B", "C", "D"),
    "net_income": ("E", "F", "G"),
    "roe": ("H", "I", "J"),
    "roa": ("K", "L", "M"),
    "net_capital_ratio": ("N", "O", "P"),
    "leverage_ratio": ("Q", "R", "S"),
    "total_employees": ("T", "U", "V"),
    "domestic_locations": ("W", "X", "Y"),
    "total_assets": ("Z", "AA", "AB"),
    "capital_stock": ("AC", "AD", "AE"),
}

TEMPLATE_PATH = "./src/financial_template.xlsx"
OUTPUT_DIR = "./rankings"


def fetch_financial_data(db_conn, report_period):
    """DB에서 재무 데이터를 가져옴."""
    query = """
        SELECT financial_name, company_name, data, difer_data
        FROM financial_rank_table
        WHERE report_period = %s
        ORDER BY financial_name, ranking ASC
    """
    with db_conn.cursor() as cur:  # db_conn.conn -> db_conn
        cur.execute(query, (report_period,))
        return cur.fetchall()

def create_excel_file(year, quarter, financial_data):
    """엑셀 템플릿을 복사하고 데이터를 삽입."""
    output_path = os.path.join(OUTPUT_DIR, f"Rankings_{year}_Q{quarter}.xlsx")
    
    # 기존 파일이 존재하면 삭제
    if os.path.exists(output_path):
        os.remove(output_path)
    
    shutil.copy(TEMPLATE_PATH, output_path)
    workbook = load_workbook(output_path)
    sheet = workbook.active

    # 2행 우측 상단의 '기준 정보' 업데이트
    title_cell = "AE2"  # '2024년 3분기 기준'이 위치하는 셀
    sheet[title_cell] = f"{year}년 {quarter}분기 기준"

    # 데이터 삽입
    for field, (col_company, col_value, col_diff) in COLUMN_MAPPING.items():
        field_data = [d for d in financial_data if d[0] == field]
        for row, data in enumerate(field_data, start=5):  # 데이터는 5행부터 시작
            company_name = data[1]
            value = data[2]
            # difer_data가 NULL이면 "X", 아니면 int 변환
            if data[3] is None:
                differ_data = "X"
                direction = "X"
            else:
                differ_data = int(data[3])
                direction = "▲" if differ_data > 0 else ("▼" if differ_data < 0 else "-")
            
            # 억 단위로 변환 및 반올림 (소수점 1자리)
            if field in ["total_equity", "net_income", "total_assets", "capital_stock"]:
                value = round(value / 100000000, 1)
            if field in ["roa","roe"]:
                value = round(value / 1, 2)

            # 회사명, 값, 변동 삽입
            sheet[f"{col_company}{row}"] = company_name
            sheet[f"{col_value}{row}"] = value
            if differ_data in ["X", 0]:
                sheet[f"{col_diff}{row}"] = f"{direction}"
            else:      
                sheet[f"{col_diff}{row}"] = f"{direction}{abs(differ_data)}"

    workbook.save(output_path)
    print(f"Excel file created: {output_path}")

def main(year, quarter):
    # 디렉토리 생성
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    try:
        # DB 연결 설정
        config_path = os.path.join('./config', 'db_config.json')
        try:
            with open(config_path, 'r', encoding='cp949') as f:
                db_config = json.load(f)
        except UnicodeDecodeError:
            with open(config_path, 'r', encoding='utf-8') as f:
                db_config = json.load(f)

        print("DB 설정:", db_config)  # 설정 내용 확인

        db_conn = DBConnection(db_config)
        try:
            db_conn.connect()
            print("Database connection established.")

            # Report period
            report_period = f"{year}-Q{quarter}"
            # 데이터 조회
            financial_data = fetch_financial_data(db_conn.conn, report_period)

            if not financial_data:
                print(f"No data found for {report_period}")
                return

            # 엑셀 생성
            create_excel_file(year, quarter, financial_data)
        finally:
            db_conn.close()
            print("Database connection closed.")

    except FileNotFoundError as e:
        print(f"Configuration file not found: {e}")
    except Exception as e:
        print(f"Unexpected error: {e}")

    

if __name__ == "__main__":
    import sys
    year = int(sys.argv[1]) if len(sys.argv) > 1 else None
    quarter = int(sys.argv[2]) if len(sys.argv) > 2 else None
    main(year, quarter)
