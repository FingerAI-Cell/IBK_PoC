"use client";

export default function Header({
  toggleSidebar,
  resetToMain,
}: {
  toggleSidebar: () => void;
  resetToMain: () => void;
}) {
  return (
    <header className="bg-white shadow p-4 flex justify-between items-center fixed top-0 left-0 right-0 z-10">
      {/* 사이드바 열기 버튼 */}
      <button onClick={toggleSidebar} className="text-lg font-bold">
        ☰
      </button>

      {/* My Services 클릭 */}
      <h1
        className="text-xl font-bold cursor-pointer hover:underline"
        onClick={resetToMain}
      >
        My Services
      </h1>

      {/* 우측 마이페이지 */}
      <div>
        <button className="px-4 py-2 bg-gray-200 rounded">My Page</button>
      </div>
    </header>
  );
}
