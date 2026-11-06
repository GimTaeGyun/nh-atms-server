import api from './axiosInstance';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  code: string;
  data: T;
}

interface PageResult<T> {
  content: T[];
  totalCount: number;
  page: number;
  size: number;
  totalPages: number;
}

export interface Branch {
  termGroupId: string;
  branchCd: string;
  branchName: string;
  branchTypeCd?: string;
  termGroupName?: string;
  faxNo?: string;
  phoneNo?: string;
  zipcode?: string;
  address?: string;
  detailAddress?: string;
  sotyNm?: string;
  partnerCd?: string;
}

export interface BranchSearchParams {
  termGroupId?: string;
  branchCd?: string;
  branchName?: string;
  branchTypeCd?: string;
  page?: number;
  size?: number;
}

export const branchApi = {
  /** 영업점(조합) 목록 — 코드↔명칭 매핑용으로 전체 조회 */
  getList: (params?: BranchSearchParams) =>
    api.get<ApiResponse<PageResult<Branch>>>('/branch', {
      params: { page: 1, size: 5000, ...params },
    }),
};
