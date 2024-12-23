"use client";

interface SidebarProps {
  isOpen: boolean;
  selectService: (service: string) => void;
  toggleSidebar: () => void;
}

export default function Sidebar({ isOpen, selectService, toggleSidebar }: SidebarProps) {
  const services = [
    { id: "overseas-loan", name: "해외주식 담보대출 모니터링" },
    { id: "financial-statements", name: "증권사 재무제표" },
    { id: "branch-manual", name: "영업점 메뉴얼 정보 조회" },
    { id: "meeting-minutes", name: "회의록 자동 작성" },
    { id: "investment-report", name: "개인화 투자정보 리포트" },
  ];

  return (
    <aside
      className={`fixed top-0 left-0 h-full bg-gray-800 text-white p-4 z-20 transition-transform duration-300 ${
        isOpen ? "translate-x-0" : "-translate-x-full"
      }`}
      style={{ width: "16rem" }}
    >
      {/* 닫기 버튼 */}
      <button
        onClick={toggleSidebar}
        className="absolute top-4 left-4 bg-gray-700 text-white p-2 rounded-full hover:bg-gray-600"
      >
        ✖
      </button>

      {/* 서비스 메뉴 제목 */}
      <h2
        className="text-lg font-bold mb-4 mt-12 cursor-pointer hover:underline"
        onClick={() => {
          selectService("default"); // 메인 화면으로 돌아가기
          toggleSidebar(); // 사이드바 닫기
        }}
      >
        서비스 메뉴
      </h2>

      {/* 서비스 리스트 */}
      <ul className="space-y-2">
        {services.map((service) => (
          <li
            key={service.id}
            onClick={() => {
              selectService(service.id);
              toggleSidebar(); // 메뉴 클릭 시 사이드바 닫기
            }}
            className="p-2 bg-gray-700 rounded cursor-pointer hover:bg-gray-600"
          >
            {service.name}
          </li>
        ))}
      </ul>
    </aside>
  );
}
