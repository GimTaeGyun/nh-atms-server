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

export interface FaultFrequent {
  termGroupId: string;
  termId: string;
  totalCnt: number;
  faultCnt: number;
  faultRate: number;
  lastFaultDate?: string;   // YYYYMMDD
  lastFaultTime?: string;
  lastErrCd?: string;
}

export interface FaultFrequentSearchParams {
  termGroupId?: string;
  termId?: string;
  fromDate?: string;        // YYYYMMDD
  toDate?: string;          // YYYYMMDD
  minFaultCnt?: number;
  page?: number;
  size?: number;
}

export const faultFrequentApi = {
  getList: (params: FaultFrequentSearchParams) =>
    api.get<ApiResponse<PageResult<FaultFrequent>>>('/fault-frequent', { params }),
};
