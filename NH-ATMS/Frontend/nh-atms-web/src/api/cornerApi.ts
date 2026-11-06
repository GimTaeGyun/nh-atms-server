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

export interface Corner {
  termGroupId: string;
  cornerCd: string;
  cornerName?: string;
  cornerTypeCd?: string;
  operationTypeCd?: string;
  locationTypeCd?: string;
  serviceStartTime?: string;
  serviceEndTime?: string;
  serviceWeekday?: string;
  boothTypeCd?: string;
  boothCount?: number;
  manageTypeCd?: string;
  outsrcCoCd?: string;
  guardCoCd?: string;
  guardStatus?: string;
  cornerSafeUseFlag?: string;
  cornerStatus?: string;
  openDate?: string;
  closeDate?: string;
  tempClsStartDate?: string;
  tempClsEndDate?: string;
  tempClsReason?: string;
  phoneNo?: string;
  zipcode?: string;
  area1Cd?: string;
  area2Cd?: string;
  area3Cd?: string;
  detailAddress?: string;
  address?: string;
  location?: string;
  lat?: string;
  lng?: string;
  enterDate?: string;
  enterUserId?: string;
  cctvSetFlag?: string;
}

export interface CornerSearchParams {
  termGroupId?: string;
  cornerCd?: string;
  cornerName?: string;
  cornerTypeCd?: string;
  cornerStatus?: string;
  page?: number;
  size?: number;
}

export const cornerApi = {
  getList: (params?: CornerSearchParams) =>
    api.get<ApiResponse<PageResult<Corner>>>('/corner', {
      params: { page: 1, size: 20, ...params },
    }),

  getOne: (termGroupId: string, cornerCd: string) =>
    api.get<ApiResponse<Corner>>(`/corner/${termGroupId}/${cornerCd}`),
};
