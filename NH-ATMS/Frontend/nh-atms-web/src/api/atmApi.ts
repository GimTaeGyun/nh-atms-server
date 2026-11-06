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

export interface Atm {
  termGroupId: string;
  termId: string;
  atmStatus?: string;
  assetFlag?: string;
  manageTypeCd?: string;
  locationTypeCd?: string;
  locationName?: string;
  serviceDayTypeCd?: string;
  serviceStartTime?: string;
  serviceEndTime?: string;
  serviceStartDate?: string;
  serviceEndDate?: string;
  enterDate?: string;
  enterTime?: string;
  phoneNo?: string;
  atmCoCd?: string;
  modelCd?: string;
  atmIp?: string;
  bpServerIp?: string;
  securityProviderCd?: string;
  serviceProviderCd?: string;
  officeCd?: string;
  teamCd?: string;
  assetSno?: string;
  machineSno?: string;
  ownerCd?: string;
  appVersion?: string;
  statusFlag?: string;
  tranFlag?: string;
  introduceDate?: string;
  processorType?: string;
  settleDate?: string;
  disuseDate?: string;
  remoteFlag?: string;
  atmCost?: number;
  durableYears?: string;
  firstSetupCosts?: number;
  branchCd?: string;
  protocolType?: string;
  boothTypeCd?: string;
  cornerCd?: string;
}

export interface AtmSearchParams {
  termGroupId?: string;
  termId?: string;
  atmStatus?: string;
  atmCoCd?: string;
  modelCd?: string;
  branchCd?: string;
  enterDateFrom?: string;
  enterDateTo?: string;
  page?: number;
  size?: number;
}

export const atmApi = {
  getList: (params?: AtmSearchParams) =>
    api.get<ApiResponse<PageResult<Atm>>>('/atm', { params }),

  getOne: (termGroupId: string, termId: string) =>
    api.get<ApiResponse<Atm>>(`/atm/${termGroupId}/${termId}`),

  create: (body: Atm) =>
    api.post<ApiResponse<void>>('/atm', body),

  update: (termGroupId: string, termId: string, body: Atm) =>
    api.put<ApiResponse<void>>(`/atm/${termGroupId}/${termId}`, body),

  delete: (termGroupId: string, termId: string) =>
    api.delete<ApiResponse<void>>(`/atm/${termGroupId}/${termId}`),
};
