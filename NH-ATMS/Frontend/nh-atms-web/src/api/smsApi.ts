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

export interface SmsSendData {
  smsSeq: number;
  globalId?: string;
  termGroupId?: string;
  termId?: string;
  recvPhone: string;
  sendPhone?: string;
  msgContent: string;
  errCd?: string;
  sendStatus?: string;   // W=대기,S=성공,F=실패
  sendDate?: string;
  sendTime?: string;
  resultCd?: string;
  resultMsg?: string;
  regDt?: string;
}

export interface SmsSearchParams {
  termGroupId?: string;
  recvPhone?: string;
  sendStatus?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

export interface SmsSendRequest {
  termGroupId?: string;
  termId?: string;
  recvPhone: string;
  sendPhone: string;
  msgContent: string;
}

export const smsApi = {
  getList: (params: SmsSearchParams) =>
    api.get<ApiResponse<PageResult<SmsSendData>>>('/sms', { params }),
  send: (body: SmsSendRequest) =>
    api.post<ApiResponse<SmsSendData>>('/sms', body),
};
