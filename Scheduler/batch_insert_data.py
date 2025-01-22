import psycopg2
from psycopg2.extras import execute_values
from src.database import DBConnection  # DB 연결 클래스 사용
import json
import os

# 테이블별 매핑 정보
TABLE_MAPPINGS = [
    {
        "table": "calcurator",
        "financial_name_column_map": {
            "total_equity": "total_equity",  # 자기자본
            "net_income": "net_income",      # 순이익
            "total_assets": "total_assets",  # 총자산
            "roe": "roe",                    # ROE
            "roa": "roa"                     # ROA
        }
    },
    {
        "table": "financial_condition",
        "financial_name_column_map": {
            "capital_stock": "capital_stock"  # 자본금
        }
    },
    {
        "table": "financial_ratio",
        "financial_name_column_map": {
            "net_capital_ratio": "net_capital_ratio"  # NCR
        }
    },
    {
        "table": "manpower_status",
        "financial_name_column_map": {
            "total_employees": "total_employees"  # 인원수
        }
    },
    {
        "table": "investment_company_announcement",
        "financial_name_column_map": {
            "leverage_ratio": "leverage_ratio"  # 레버리지비율
        }
    },
    {
        "table": "organization_structure",
        "financial_name_column_map": {
            "domestic_locations": "domestic_sales_offices + domestic_branches"  # 지점수(영업점포 포함)
        }
    }
]



def batch_insert_data(db_conn, report_period):
    try:
        with db_conn.cursor() as cur:
            all_data = []  # 최종적으로 삽입할 데이터 저장

            # 1. 각 테이블에서 데이터 수집
            for mapping in TABLE_MAPPINGS:
                table = mapping["table"]
                financial_name_column_map = mapping["financial_name_column_map"]

                print(f"Processing table: {table}")  # 로그 추가

                for financial_name, column in financial_name_column_map.items():
                    print(f"Processing: {financial_name} from {table}")  # 로그 추가
                    select_query = f"""
                        SELECT 
                            %s AS report_period,
                            '{financial_name}' AS financial_name,
                            company_name,
                            {column} AS data,
                            NULL AS ranking,             -- 순위는 나중에 계산 예정
                            NULL AS differ_sign,         -- 전분기 기호 (예: +, -)
                            NULL AS difer_data           -- 전분기 차이
                        FROM {table}
                        WHERE report_period = %s
                        AND company_name IN (
                            SELECT company_name
                            FROM company_info
                        )
                    """
                    # 두 개의 값을 전달 (report_period 두 번 사용)
                    cur.execute(select_query, (report_period, report_period))
                    rows = cur.fetchall()
                    all_data.extend(rows)

            # 2. 중복 데이터 확인 및 삽입/업데이트
            print("Checking for existing data in financial_rank_table...")  # 로그 추가
            cur.execute("""
                SELECT report_period, company_name, financial_name FROM financial_rank_table
                WHERE report_period = %s
                AND company_name IN (
                            SELECT company_name
                            FROM company_info
                )
            """, (report_period,))
            existing_rows = cur.fetchall()
            existing_keys = set((row[0], row[1], row[2]) for row in existing_rows)

            # 중복 데이터 필터링
            filtered_data = [
                row for row in all_data
                if (row[0], row[2], row[1]) not in existing_keys
            ]

            # 데이터 삽입/업데이트
            if filtered_data:
                print(f"Inserting {len(filtered_data)} rows into financial_rank_table...")  # 로그 추가
                insert_query = """
                    INSERT INTO financial_rank_table
                    (report_period, financial_name, company_name, data, ranking, differ_sign, difer_data)
                    VALUES %s
                    ON CONFLICT (report_period, company_name, financial_name)
                    DO UPDATE SET
                        data = EXCLUDED.data,
                        ranking = EXCLUDED.ranking,
                        differ_sign = EXCLUDED.differ_sign,
                        difer_data = EXCLUDED.difer_data;
                """
                execute_values(cur, insert_query, filtered_data)
                db_conn.commit()
                print(f"Successfully inserted/updated {len(filtered_data)} rows.")
            else:
                print("No new data to insert.")

    except Exception as e:
        db_conn.rollback()
        print(f"Error during processing: {e}")
        raise


def main(year=None, quarter=None):
    try:
        # db_config.json 파일 읽기
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
            print("Database connection established.")  # 로그 추가

            # Report period 조합
            report_period = f"{year}-Q{quarter}"

            # Batch insert
            batch_insert_data(db_conn.conn, report_period)
        finally:
            db_conn.close()
            print("Database connection closed.")  # 로그 추가

    except FileNotFoundError as e:
        print(f"Configuration file not found: {e}")
    except Exception as e:
        print(f"Unexpected error: {e}")

if __name__ == "__main__":
    import sys
    print(f"Command line arguments: {sys.argv}")
    year = int(sys.argv[1]) if len(sys.argv) > 1 else None
    quarter = int(sys.argv[2]) if len(sys.argv) > 2 else None
    main(year, quarter)
