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

export interface FaultManagement {
  faultSeq: number;
  globalId?: string;
  termGroupId: string;
  termId: string;
  faultType?: string;
  errCd?: string;
  faultStatus?: string;   // O=접수,P=진행,C=완료,X=취소
  callDate?: string;
  callTime?: string;
  doneDate?: string;
  doneTime?: string;
  vendorCd?: string;
  faultDesc?: string;
  regDt?: string;
  updDt?: string;
}

export interface FaultManagementSearchParams {
  termGroupId?: string;
  termId?: string;
  faultStatus?: string;
  faultType?: string;
  vendorCd?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

export interface FaultReceiptRequest {
  termGroupId: string;
  termId: string;
  faultType?: string;
  errCd?: string;
  vendorCd?: string;
  faultDesc?: string;
}

export const faultMgmtApi = {
  getList: (params: FaultManagementSearchParams) =>
    api.get<ApiResponse<PageResult<FaultManagement>>>('/fault-mgmt', { params }),
  getOne: (faultSeq: number) =>
    api.get<ApiResponse<FaultManagement>>(`/fault-mgmt/${faultSeq}`),
  receive: (body: FaultReceiptRequest) =>
    api.post<ApiResponse<FaultManagement>>('/fault-mgmt', body),
  cancel: (faultSeq: number) =>
    api.post<ApiResponse<void>>(`/fault-mgmt/${faultSeq}/cancel`),
};
