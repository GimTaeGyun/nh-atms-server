import api from './axiosInstance';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  code: string;
  data: T;
}

export interface PageResult<T> {
  content: T[];
  totalCount: number;
  page: number;
  size: number;
  totalPages: number;
}

export interface AtmMonitor {
  termGroupId: string;
  termId: string;
  jnlDate: string;       // YYYYMMDD
  jnlTime: string;       // HHMMSSFFF + flag
  agentType?: string;
  atmStatus?: string;
  errCd?: string;
  atmErrCd?: string;
  processCount?: string;
  manStatus?: string;
  omanStatus?: string;
  sprStatus?: string;
  jprStatus?: string;
  chkStatus?: string;
  chaStatus?: string;
  mcrStatus?: string;
  pbStatus?: string;
  rjtStatus?: string;
  sendFlag?: string;
  recvDate?: string;
  recvTime?: string;
}

export interface AtmMonitorSearchParams {
  termGroupId?: string;
  termId?: string;
  atmStatus?: string;
  errCd?: string;
  fromDate?: string;     // YYYYMMDD
  toDate?: string;       // YYYYMMDD
  errorOnly?: boolean;
  page?: number;
  size?: number;
}

export const atmMonitorApi = {
  getList: (params: AtmMonitorSearchParams) =>
    api.get<ApiResponse<PageResult<AtmMonitor>>>('/atm-monitor', { params }),
};
