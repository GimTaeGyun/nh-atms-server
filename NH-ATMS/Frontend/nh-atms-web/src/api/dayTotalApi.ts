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

export interface DayTotalRow {
  termGroupId: string;
  termId: string;
  jnlDate?: string;       // YYYYMMDD
  jnlTime?: string;
  totalSrcCd?: string;    // 1=호스트, 2=기기
  totalCd?: string;       // 마감항목코드
  totalVal?: string;      // 집계값
  actionType?: string;    // 5=소계, 6=합계
  beginDate?: string;
  beginTime?: string;
  endDate?: string;
  endTime?: string;
}

export interface DayTotalSearchParams {
  termGroupId?: string;
  termId?: string;
  jnlDate?: string;       // YYYYMMDD
  actionType?: string;    // 5=소계, 6=합계
  totalSrcCd?: string;    // 1=호스트, 2=기기
  totalCd?: string;
  page?: number;
  size?: number;
}

export const dayTotalApi = {
  getList: (params: DayTotalSearchParams) =>
    api.get<ApiResponse<PageResult<DayTotalRow>>>('/day-total', { params }),
};
