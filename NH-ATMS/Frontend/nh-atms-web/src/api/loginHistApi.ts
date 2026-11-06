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

export interface LoginHist {
  loginSeq: number;
  userId: string;
  loginDt: string;
  logoutDt?: string;
  loginIp?: string;
  loginResult: string; // S/F
  failReason?: string;
  userAgent?: string;
}

export interface LoginHistSearchParams {
  userId?: string;
  loginResult?: string;
  fromDt?: string;
  toDt?: string;
  page?: number;
  size?: number;
}

export const loginHistApi = {
  getList: (params: LoginHistSearchParams) =>
    api.get<ApiResponse<PageResult<LoginHist>>>('/login-hist', { params }),
};
