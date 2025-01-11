import requests
import json
import datetime
import base64
import os

# 옵션별 디렉토리 이름 설정
option_dirs = {
    1: "manpower_status",
    2: "financial_condition",
    3: "financial_ratio",
    4: "investment_company_announcement",
    5: "organization_structure"
}

def get_quarter_dates(test_year=None, test_quarter=None):
    if test_year and test_quarter:
        # 테스트용 파라미터가 있을 때는 해당 분기 조회
        year = int(test_year)
        quarter = int(test_quarter)
    else:
        # 크론잡으로 실행될 때는 이전 분기 조회
        today = datetime.datetime.now()
        year = today.year
        month = today.month
        quarter = (month - 1) // 3  # 현재 달의 이전 분기
        
        # 1분기(1,2,3월)면 이전년도 4분기로 변경
        if quarter == 0:
            year -= 1
            quarter = 4
        
    # 분기별 날짜 설정
    if quarter == 1:  # Q1: 1월1일 ~ 3월31일
        start_date = f"{str(year)}0101"
        end_date = f"{str(year)}0331"
    elif quarter == 2:  # Q2: 4월1일 ~ 6월30일
        start_date = f"{str(year)}0401"
        end_date = f"{str(year)}0630"
    elif quarter == 3:  # Q3: 7월1일 ~ 9월30일
        start_date = f"{str(year)}0701"
        end_date = f"{str(year)}0930"
    else:  # Q4: 10월1일 ~ 12월31일
        start_date = f"{str(year)}1001"
        end_date = f"{str(year)}1231"
    
    return start_date, end_date

def call_api(option, test_year=None, test_quarter=None):
    url = "http://211.111.214.248:52167/ScrapService"
    start_date, end_date = get_quarter_dates(test_year, test_quarter)

    headers = {
        "Content-Type": "application/json"
    }

    # 옵션 4번의 경우 분기 전체 기간 조회, 나머지는 분기 마지막 날짜만 조회
    data = {
        "라이선스키": "A07D7CC707D24D6BADE5EE9C87658A9",
        "maxPool": 1,
        "serviceUri": "https://down.finger.co.kr/down/mws/big/service",
        "스크래핑": [
            {
                "서비스명": "금융투자협회_금융투자회사공시",
                "옵션": str(option),
                "조회기간": end_date,
                "조회시작일": start_date,
                "조회종료일": end_date,
                "jobId": "금융투자협회_금융투자회사공시"
            }
        ]
    }

    response = requests.post(url, headers=headers, data= json.dumps(data, ensure_ascii=False, indent=4))
    if response.status_code == 200:
        try:
            json_data = response.json()
            if '조회결과' in json_data and json_data['조회결과']:
                excel_data_base64 = json_data['조회결과'][0]['데이터'][0]['엑셀데이터']
                return excel_data_base64
            else:
                print("No '조회결과' found or empty results.")
                return None
        except json.JSONDecodeError:
            print(f"Invalid JSON received: {response.text}")
            return None
    else:
        print(f"API request failed with status {response.status_code}: {response.text}")
        return None

def save_excel(option, content, test_year=None, test_quarter=None):
    try:
        decoded_bytes = base64.b64decode(content)
    except base64.binascii.Error as e:
        print(f"Failed to decode Base64 data: {e}")
        return

    # 파일명용 연도와 분기 계산
    if test_year and test_quarter:
        year = test_year
        quarter = test_quarter
    else:
        # 크론잡으로 실행될 때는 이전 분기 정보로 저장
        today = datetime.datetime.now()
        year = today.year
        month = today.month
        quarter = (month - 1) // 3  # 현재 달의 이전 분기
        
        # 1분기(1,2,3월)면 이전년도 4분기로 변경
        if quarter == 0:
            year = str(year - 1)
            quarter = 4
        else:
            year = str(year)

    # 디렉토리 생성
    dir_name = option_dirs[option]
    if not os.path.exists(dir_name):
        os.makedirs(dir_name)
    
    # 파일명에 연도와 분기 정보 포함
    file_name = f"{dir_name}/{year}Q{quarter}_{dir_name}.xlsx"
    
    with open(file_name, "wb") as file:
        file.write(decoded_bytes)
        print(f"Saved {file_name}")

def main():
    # 테스트 실행 예시:
    test_year = "2024"  # 테스트시 연도 입력 (예: "2024")
    test_quarter = 1    # 테스트시 분기 입력 (1,2,3,4)
    
    for option in range(1, 6):
        excel_data_base64 = call_api(option, test_year, test_quarter)
        if excel_data_base64:
            save_excel(option, excel_data_base64, test_year, test_quarter)
        else:
            print(f"No data to save for option {option}")

if __name__ == "__main__":
    main()