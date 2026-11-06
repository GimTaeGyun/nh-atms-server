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

/** 원격제어 명령유형 (계약 enum) */
export type RemoteCommandType =
  | 'STAT' | 'IMGC' | 'DLD' | 'FCOL' | 'RST' | 'RBT' | 'POFF' | 'STOP'
  | 'OPEN_OP' | 'JMOD' | 'MSG' | 'TMOD' | 'CLS' | 'RCMD' | 'GDL';

export interface RemoteCommand {
  cmdId: string;
  termGroupId: string;
  termId: string;
  cmdType: RemoteCommandType;
  payload?: string;
  status: string; // W/A/S/F/T/C
  timeoutSec?: number;
  resultCd?: string;
  resultData?: string;
  errMsg?: string;
  regUserId?: string;
  issuedAt?: string;
  ackAt?: string;
  completedAt?: string;
}

export interface RemoteCommandSearchParams {
  termGroupId?: string;
  termId?: string;
  cmdType?: string;
  status?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

export interface RemoteCommandIssueRequest {
  termGroupId: string;
  termId: string;
  cmdType: RemoteCommandType;
  payload?: Record<string, unknown>;
  timeoutSec?: number;
}

export const remoteControlApi = {
  /** 명령 발행이력 조회 */
  getList: (params: RemoteCommandSearchParams) =>
    api.get<ApiResponse<PageResult<RemoteCommand>>>('/remote-control/commands', { params }),

  /** 명령 단건 조회(처리결과 확인) */
  getOne: (cmdId: string) =>
    api.get<ApiResponse<RemoteCommand>>(`/remote-control/commands/${cmdId}`),

  /** 원격제어 명령 발행 */
  issue: (body: RemoteCommandIssueRequest) =>
    api.post<ApiResponse<RemoteCommand>>('/remote-control/commands', body),
};
