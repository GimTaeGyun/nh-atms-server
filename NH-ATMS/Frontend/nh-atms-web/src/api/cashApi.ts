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

export interface CashPosition {
  termGroupId: string;
  termId: string;
  jnlDate?: string;       // YYYYMMDD
  jnlTime?: string;
  cassetteType?: string;
  cassetteNo?: string;
  currency?: string;
  denomination?: number;
  remainQty?: number;
  amount?: number;
}

export interface CashSearchParams {
  termGroupId?: string;
  termId?: string;
  baseDate?: string;      // YYYYMMDD
  page?: number;
  size?: number;
}

export const cashApi = {
  getList: (params: CashSearchParams) =>
    api.get<ApiResponse<PageResult<CashPosition>>>('/cash', { params }),
};
