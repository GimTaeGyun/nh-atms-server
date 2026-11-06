import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/useAuthStore';
import AppLayout from '@/layouts/AppLayout';
import LoginPage from '@/pages/login/LoginPage';
import DashboardPage from '@/pages/dashboard/DashboardPage';
import MenuMgmtPage from '@/pages/system/MenuMgmtPage';
import UserMgmtPage from '@/pages/system/UserMgmtPage';
import GroupMgmtPage from '@/pages/system/GroupMgmtPage';
import LoginHistPage from '@/pages/system/LoginHistPage';
import AtmMonitorPage from '@/pages/atm/AtmMonitorPage';
import TransactionHistoryPage from '@/pages/atm/TransactionHistoryPage';
import FaultHistoryPage from '@/pages/atm/FaultHistoryPage';
import AgentStatusPage from '@/pages/atm/AgentStatusPage';
import FaultFrequentPage from '@/pages/atm/FaultFrequentPage';
import RemoteControlPage from '@/pages/atm/RemoteControlPage';
import RealtimeMonitorPage from '@/pages/atm/RealtimeMonitorPage';
import MaintenanceReceiptPage from '@/pages/atm/MaintenanceReceiptPage';
import IndividualDownloadPage from '@/pages/atm/IndividualDownloadPage';
import BatchDownloadPage from '@/pages/atm/BatchDownloadPage';
import CashInquiryPage from '@/pages/cash/CashInquiryPage';
import ClosingMgmtPage from '@/pages/cash/ClosingMgmtPage';
import CodeMgmtPage from '@/pages/base/CodeMgmtPage';
import BranchInfoPage from '@/pages/base/BranchInfoPage';
import CornerInfoPage from '@/pages/base/CornerInfoPage';
import TransactionJournalPage from '@/pages/journal/TransactionJournalPage';
import OperationJournalPage from '@/pages/journal/OperationJournalPage';
import JournalListPage from '@/pages/journal/JournalListPage';
import ForexTransactionPage from '@/pages/journal/ForexTransactionPage';
import PassbookTransactionPage from '@/pages/journal/PassbookTransactionPage';
import GiroTransactionPage from '@/pages/journal/GiroTransactionPage';
import BatchJobPage from '@/pages/batch/BatchJobPage';
import AtmMgmtPage from '@/pages/base/atm/AtmMgmtPage';
import NoticePage from '@/pages/notice/NoticePage';
import ServiceProviderPage from '@/pages/external/ServiceProviderPage';
import TransactionSpecificPage from '@/pages/transaction/TransactionSpecificPage';
import DepositWithdrawPage from '@/pages/transaction/DepositWithdrawPage';
import GiroPaymentPage from '@/pages/transaction/GiroPaymentPage';
import CreditCardPage from '@/pages/transaction/CreditCardPage';
import CheckTransactionPage from '@/pages/transaction/CheckTransactionPage';
import CardlessPage from '@/pages/transaction/CardlessPage';
import PassbookEntryPage from '@/pages/transaction/PassbookEntryPage';
import ForexEntryPage from '@/pages/transaction/ForexEntryPage';
import FaultDispatchHistoryPage from '@/pages/atm/FaultDispatchHistoryPage';
import SmsHistoryPage from '@/pages/atm/SmsHistoryPage';
import UnderConstruction from '@/pages/common/UnderConstruction';

const PrivateRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />;
};

const AppRouter: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/*"
          element={
            <PrivateRoute>
              <AppLayout>
                <Routes>
                  <Route path="/dashboard" element={<DashboardPage />} />
                  <Route path="/system/menu" element={<MenuMgmtPage />} />
                  <Route path="/system/user" element={<UserMgmtPage />} />
                  <Route path="/system/group" element={<GroupMgmtPage />} />
                  <Route path="/system/login-hist" element={<LoginHistPage />} />
                  <Route path="/atm/monitor" element={<AtmMonitorPage />} />
                  <Route path="/atm/transaction" element={<TransactionHistoryPage />} />
                  <Route path="/atm/journal" element={<TransactionHistoryPage />} />
                  <Route path="/atm/fault" element={<FaultHistoryPage />} />
                  <Route path="/atm/agent-status" element={<AgentStatusPage />} />
                  <Route path="/atm/fault-frequent" element={<FaultFrequentPage />} />
                  <Route path="/atm/remote-control" element={<RemoteControlPage />} />
                  <Route path="/atm/realtime-monitor" element={<RealtimeMonitorPage />} />
                  <Route path="/atm/maintenance" element={<MaintenanceReceiptPage />} />
                  <Route path="/atm/fault-dispatch" element={<FaultDispatchHistoryPage />} />
                  <Route path="/atm/sms-history" element={<SmsHistoryPage />} />
                  <Route path="/atm/download-individual" element={<IndividualDownloadPage />} />
                  <Route path="/atm/download-batch" element={<BatchDownloadPage />} />
                  <Route path="/transaction/specific" element={<TransactionSpecificPage />} />
                  <Route path="/transaction/deposit-withdraw" element={<DepositWithdrawPage />} />
                  <Route path="/transaction/giro" element={<GiroPaymentPage />} />
                  <Route path="/transaction/card" element={<CreditCardPage />} />
                  <Route path="/transaction/check" element={<CheckTransactionPage />} />
                  <Route path="/transaction/cardless" element={<CardlessPage />} />
                  <Route path="/transaction/passbook" element={<PassbookEntryPage />} />
                  <Route path="/transaction/forex" element={<ForexEntryPage />} />
                  <Route path="/cash" element={<CashInquiryPage />} />
                  <Route path="/cash/day-total" element={<ClosingMgmtPage />} />
                  <Route path="/base/code" element={<CodeMgmtPage />} />
                  <Route path="/base/atm" element={<AtmMgmtPage />} />
                  <Route path="/base/branch" element={<BranchInfoPage />} />
                  <Route path="/base/corner" element={<CornerInfoPage />} />
                  <Route path="/journal/transaction" element={<TransactionJournalPage />} />
                  <Route path="/journal/operation" element={<OperationJournalPage />} />
                  <Route path="/journal/list" element={<JournalListPage />} />
                  <Route path="/journal/forex" element={<ForexTransactionPage />} />
                  <Route path="/journal/passbook" element={<PassbookTransactionPage />} />
                  <Route path="/journal/giro" element={<GiroTransactionPage />} />
                  <Route path="/batch/job" element={<BatchJobPage />} />
                  <Route path="/base/vendor" element={<ServiceProviderPage />} />
                  <Route path="/base/notice" element={<NoticePage />} />
                  <Route path="*" element={<UnderConstruction />} />
                </Routes>
              </AppLayout>
            </PrivateRoute>
          }
        />
      </Routes>
    </BrowserRouter>
  );
};

export default AppRouter;