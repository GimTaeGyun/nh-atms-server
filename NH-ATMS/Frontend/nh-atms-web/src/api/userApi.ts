import api from './axiosInstance';
import { encryptField } from '@/utils/fieldCrypto';
import type { PageResult } from './loginHistApi';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  code: string;
  data: T;
}

export interface User {
  userId: string;
  userName: string;
  password?: string;
  empNo?: string;
  userGroupNo?: number;
  userGroupName?: string;
  userTypeCd?: string; // A/U/M
  email?: string;
  phoneNo?: string;
  cellPhoneNo?: string;
  deptCd?: string;
  deptName?: string;
  branchCd?: string;
  userPosition?: string;
  lastLoginDt?: string;
  loginFlag?: string;
  loginFailCnt?: number;
  lockYn?: string;
  useYn?: string;
  regDt?: string;
  updDt?: string;
}

export interface UserSearchParams {
  userId?: string;
  userName?: string;
  deptCd?: string;
  userTypeCd?: string;
  useYn?: string;
  userGroupNo?: number;
  page?: number;
  size?: number;
}

export const userApi = {
  list: (params: UserSearchParams) =>
    api.get<ApiResponse<PageResult<User>>>('/user', { params }),
  get: (userId: string) =>
    api.get<ApiResponse<User>>(`/user/${userId}`),
  // 비밀번호는 암호화해서 보냄 (docs/FIELD_ENCRYPTION_DESIGN.md)
  create: async (u: User) =>
    api.post<ApiResponse<void>>('/user', { ...u, password: u.password ? await encryptField(u.password) : u.password }),
  update: (userId: string, u: User) =>
    api.put<ApiResponse<void>>(`/user/${userId}`, u),
  remove: (userId: string) =>
    api.delete<ApiResponse<void>>(`/user/${userId}`),
  changePassword: async (userId: string, password: string) =>
    api.put<ApiResponse<void>>(`/user/${userId}/password`, { password: await encryptField(password) }),
  unlock: (userId: string) =>
    api.put<ApiResponse<void>>(`/user/${userId}/unlock`),
  toggleUseYn: (userId: string, useYn: string) =>
    api.put<ApiResponse<void>>(`/user/${userId}/use-yn`, { useYn }),
};
