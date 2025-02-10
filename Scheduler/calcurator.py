import psycopg2
from psycopg2.extras import execute_values
from src.database import DBConnection  # DB 연결 클래스 사용
import json
import os
import batch_insert_data

def batch_insert_calcurator_data(db_conn, year, quarter):
    print(f"batch_insert_calcurator_data called for year={year}, quarter={quarter}")
    try:
        with db_conn.cursor() as cur:
            report_period = f"{year}-Q{quarter}"
            prev_report_period = f"{year - 1}-Q4"
            q1_report_period = f"{year}-Q1"  # 올해 1분기 데이터 가져오기
            quarter_num = quarter

            query = """
                SELECT
                    c.company_name,
                    fc.total_assets AS current_total_assets,
                    fc.total_equity AS current_total_equity,
                    fc.net_income AS current_net_income,
                    fc.fiscal_month AS current_fiscal_month,
                    q1_fc.net_income AS current_q1_net_income,
                    prev_fc.total_assets AS prev_total_assets,
                    prev_fc.total_equity AS prev_total_equity,
                    prev_fc.net_income AS prev_net_income
                FROM company_info c
                LEFT JOIN financial_condition fc
                    ON c.company_name = fc.company_name
                    AND fc.report_period = %s
                LEFT JOIN financial_condition q1_fc
                    ON c.company_name = q1_fc.company_name
                    AND q1_fc.report_period = %s
                LEFT JOIN financial_condition prev_fc
                    ON c.company_name = prev_fc.company_name
                    AND prev_fc.report_period = %s
            """
            cur.execute(query, (report_period, q1_report_period, prev_report_period))
            rows = cur.fetchall()

            all_data = []

            for row in rows:
                (
                    company_name,
                    current_total_assets,
                    current_total_equity,
                    current_net_income,
                    current_fiscal_month,
                    current_q1_net_income,
                    prev_total_assets,
                    prev_total_equity,
                    prev_net_income
                ) = row

                if current_total_assets is None or current_total_equity is None or current_net_income is None:
                    continue

                net_income = current_net_income

                # 3월 결산 여부에 따른 계산 로직 변경
                if current_fiscal_month == "3월":
                    if quarter == 1 and prev_net_income is not None:
                        net_income -= prev_net_income
                    elif quarter in [2, 3, 4] and current_q1_net_income is not None and prev_net_income is not None:
                        net_income = current_net_income + current_q1_net_income - prev_net_income
                else:
                    net_income = current_net_income

                all_data.append((
                    report_period, company_name, quarter_num, current_fiscal_month,
                    current_total_equity, current_total_assets,
                    prev_total_equity, prev_total_assets,
                    net_income, current_net_income, prev_net_income,
                    current_q1_net_income, None, None
                ))

            # 중복 데이터 제거
            cur.execute("""
                SELECT report_period, company_name FROM calcurator
                WHERE report_period = %s
            """, (report_period,))
            existing_rows = cur.fetchall()
            existing_keys = set((row[0], row[1]) for row in existing_rows)

            filtered_data = [
                row for row in all_data
                if (row[0], row[1]) not in existing_keys
            ]

            if filtered_data:
                print(f"Inserting {len(filtered_data)} rows into calcurator table...")
                insert_query = """
                    INSERT INTO calcurator
                    (report_period, company_name, quarter_num, fiscal_month, total_equity, total_assets, prev_total_equity,
                     prev_total_assets, net_income, current_net_income, prev_net_income, current_q1_net_income, roa, roe)
                    VALUES %s
                    ON CONFLICT (report_period, company_name) 
                    DO UPDATE SET
                        quarter_num = EXCLUDED.quarter_num,
                        fiscal_month = EXCLUDED.fiscal_month,
                        total_equity = EXCLUDED.total_equity,
                        total_assets = EXCLUDED.total_assets,
                        prev_total_equity = EXCLUDED.prev_total_equity,
                        prev_total_assets = EXCLUDED.prev_total_assets,
                        net_income = EXCLUDED.net_income,
                        current_net_income = EXCLUDED.current_net_income,
                        prev_net_income = EXCLUDED.prev_net_income,
                        current_q1_net_income = EXCLUDED.current_q1_net_income,
                        roa = EXCLUDED.roa,
                        roe = EXCLUDED.roe;
                """
                execute_values(cur, insert_query, filtered_data)
                db_conn.commit()
                print(f"Successfully inserted or updated {len(filtered_data)} rows.")
            else:
                print("No data to insert.")

    except Exception as e:
        db_conn.rollback()
        print(f"Error during data insertion: {e}")
        raise


def update_roa_roe(db_conn, year, quarter):
    try:
        with db_conn.cursor() as cur:
            # 분기 및 report_period 생성
            report_period = f"{year}-Q{quarter}"

            # 필요한 데이터 조회
            cur.execute("""
                SELECT 
                    report_period,
                    company_name,
                    net_income,
                    total_assets,
                    prev_total_assets,
                    total_equity,
                    prev_total_equity
                FROM calcurator
                WHERE report_period = %s
            """, (report_period,))
            rows = cur.fetchall()
            print(f"Fetched {len(rows)} rows for ROA/ROE calculation.")  # 디버깅 로그 추가

            updates = []  # 업데이트할 데이터 준비

            for row in rows:
                (
                    report_period,
                    company_name,
                    net_income,
                    total_assets,
                    prev_total_assets,
                    total_equity,
                    prev_total_equity
                ) = row

                # 평균 계산
                avg_total_assets = None
                avg_total_equity = None
                roa = None
                roe = None

                if total_assets is not None and prev_total_assets is not None:
                    avg_total_assets = (total_assets + prev_total_assets) / 2

                if total_equity is not None and prev_total_equity is not None:
                    avg_total_equity = (total_equity + prev_total_equity) / 2

                # ROA 계산 (총자산이익률: Net Income / Average Total Assets) * (4 / Quarter)
                if avg_total_assets is not None and avg_total_assets != 0:
                    roa = (net_income / avg_total_assets) * 100 * (4 / quarter)
                else:
                    roa = 0  # 기본값 설정



                # ROE 계산 (자기자본이익률: Net Income / Average Total Equity)
                if avg_total_equity is not None and avg_total_equity != 0:
                    roe = (net_income / avg_total_equity) * 100 * (4 / quarter)
                else:
                    roe = 0  # 기본값 설정

                # 업데이트 데이터 준비
                updates.append((
                    avg_total_assets,
                    avg_total_equity,
                    roa,
                    roe,
                    report_period,
                    company_name
                ))

            # 업데이트 쿼리 실행
            if updates:
                print(f"Updating {len(updates)} rows in calcurator table...")
                update_query = """
                    UPDATE calcurator
                    SET avg_total_assets = %s,
                        avg_total_equity = %s,
                        roa = %s,
                        roe = %s
                    WHERE report_period = %s AND company_name = %s
                """
                cur.executemany(update_query, updates)
                db_conn.commit()
                print(f"Successfully updated {len(updates)} rows.")
            else:
                print("No data to update.")

    except Exception as e:
        db_conn.rollback()
        print(f"Error during ROA/ROE update: {e}")
        raise


def main(year=None, quarter=None):
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

            # 1단계: 데이터 삽입
            batch_insert_calcurator_data(db_conn.conn, year, quarter)

            # 2단계: ROA/ROE 계산 및 업데이트
            update_roa_roe(db_conn.conn, year, quarter)
        finally:
            db_conn.close()
            print("Database connection closed.")

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
    
