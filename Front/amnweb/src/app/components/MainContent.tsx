"use client";

import OverseasLoan from "./services/OverseasLoan";
import FinancialStatements from "./services/FinancialStatements"
import BranchManual from "./services/BranchManual";
import MeetingMinutes from "./services/MeetingMinutes";
import InvestmentReport from "./services/InvestmentReport";

export default function MainContent({ currentService }: { currentService: string }) {
  return (
    <div className="p-6">
      {currentService === "overseas-loan" && <OverseasLoan />}
      {currentService === "financial-statements" && <FinancialStatements />}
      {currentService === "branch-manual" && <BranchManual />}
      {currentService === "meeting-minutes" && <MeetingMinutes />}
      {currentService === "investment-report" && <InvestmentReport />}
      {currentService === "default" && <p>서비스를 선택하세요.</p>}
    </div>
  );
}
