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

export interface AgentStatus {
  atmIp: string;
  agentType?: string;
  termGroupId?: string;
  termId?: string;
  connectFlag?: string;   // 1=접속, 0=해제
  agentVer?: string;
  apDownVer?: string;
  osName?: string;
  atmMacAddr?: string;
  connectIp?: string;
  connectPort?: string;
  jnlMode?: string;
  pollCycle?: string;
  startDate?: string;     // YYYYMMDD
  startTime?: string;     // HHMMSS
  lastPollDate?: string;
  lastPollTime?: string;
  exitDate?: string;
  exitTime?: string;
}

export interface AgentStatusSearchParams {
  termGroupId?: string;
  termId?: string;
  connectFlag?: string;
  fromDate?: string;      // YYYYMMDD
  toDate?: string;        // YYYYMMDD
  page?: number;
  size?: number;
}

export const agentStatusApi = {
  getList: (params: AgentStatusSearchParams) =>
    api.get<ApiResponse<PageResult<AgentStatus>>>('/agent-status', { params }),
};
