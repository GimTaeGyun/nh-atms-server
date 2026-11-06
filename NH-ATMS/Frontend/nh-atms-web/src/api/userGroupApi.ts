import api from './axiosInstance';
import type { PageResult } from './loginHistApi';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  code: string;
  data: T;
}

export interface UserGroup {
  userGroupNo?: number;
  userGroupName: string;
  userTypeCd?: string;
  description?: string;
  useYn?: string;
  regDt?: string;
  updDt?: string;
  authCodes?: string[];
}

export interface UserGroupSearchParams {
  userGroupName?: string;
  userTypeCd?: string;
  useYn?: string;
  page?: number;
  size?: number;
}

export const userGroupApi = {
  list: (params: UserGroupSearchParams) =>
    api.get<ApiResponse<PageResult<UserGroup>>>('/usergroup', { params }),
  all: () =>
    api.get<ApiResponse<UserGroup[]>>('/usergroup/all'),
  get: (no: number) =>
    api.get<ApiResponse<UserGroup>>(`/usergroup/${no}`),
  create: (g: UserGroup) =>
    api.post<ApiResponse<void>>('/usergroup', g),
  update: (no: number, g: UserGroup) =>
    api.put<ApiResponse<void>>(`/usergroup/${no}`, g),
  remove: (no: number) =>
    api.delete<ApiResponse<void>>(`/usergroup/${no}`),
  getAuthCodes: (no: number) =>
    api.get<ApiResponse<string[]>>(`/usergroup/${no}/auth`),
  updateAuthCodes: (no: number, authCodes: string[]) =>
    api.put<ApiResponse<void>>(`/usergroup/${no}/auth`, { authCodes }),
};
