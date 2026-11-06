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

export interface ServiceProvider {
  providerCd: string;
  knbankFlag?: string;
  providerName: string;
  providerTypeCd?: string;
  person?: string;
  phoneNo?: string;
  faxNo?: string;
  email?: string;
  zipCd?: string;
  address?: string;
  sendType?: string;
  faultSendType?: string;
  useYn?: string;
  wooriFlag?: string;
}

export interface ServiceProviderSearchParams {
  providerCd?: string;
  providerName?: string;
  providerTypeCd?: string;
  useYn?: string;
  page?: number;
  size?: number;
}

export const serviceProviderApi = {
  getList: (params?: ServiceProviderSearchParams) =>
    api.get<ApiResponse<PageResult<ServiceProvider>>>('/service-provider', { params }),

  getOne: (providerCd: string) =>
    api.get<ApiResponse<ServiceProvider>>(`/service-provider/${providerCd}`),

  create: (body: ServiceProvider) =>
    api.post<ApiResponse<void>>('/service-provider', body),

  update: (providerCd: string, body: ServiceProvider) =>
    api.put<ApiResponse<void>>(`/service-provider/${providerCd}`, body),

  delete: (providerCd: string) =>
    api.delete<ApiResponse<void>>(`/service-provider/${providerCd}`),
};
