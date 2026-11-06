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

// ── T_BATCHJOB ─────────────────────────────────────────────────
export interface BatchJob {
  procName: string;
  jobDesc?: string;
  cronExpr: string;
  hostName?: string;
  jobStatus?: string;        // IDLE / RUNNING
  lastStartDate?: string;
  lastStartTime?: string;
  lastEndDate?: string;
  lastEndTime?: string;
  useFlag?: string;          // Y / N
  sortOrder?: number;
  regDt?: string;
  updDt?: string;
}

export interface BatchJobSaveRequest {
  procName: string;
  jobDesc?: string;
  cronExpr: string;
  hostName?: string;
  useFlag?: string;
  sortOrder?: number;
}

export interface BatchJobSearchParams {
  procName?: string;
  jobDesc?: string;
  useFlag?: string;
  hostName?: string;
  page?: number;
  size?: number;
}

// ── T_BATCHHISTORY ─────────────────────────────────────────────
export interface BatchHistory {
  histId: number;
  procName: string;
  hostName?: string;
  runType?: string;          // SCHEDULE / MANUAL
  statusCd?: string;         // RUNNING / SUCCESS / FAIL
  startDate?: string;
  startTime?: string;
  endDate?: string;
  endTime?: string;
  delayTime?: number;
  targetCnt?: number;
  procCnt?: number;
  errCnt?: number;
  errMsg?: string;
  regDt?: string;
}

export interface CronValidateResult {
  valid: boolean;
  next: string;
}

export const batchJobApi = {
  /** 배치잡 목록(페이징) */
  getList: (params: BatchJobSearchParams) =>
    api.get<ApiResponse<PageResult<BatchJob>>>('/batch/jobs', { params }),

  /** 배치잡 단건 */
  getOne: (procName: string) =>
    api.get<ApiResponse<BatchJob>>(`/batch/jobs/${procName}`),

  /** 신규 등록 */
  create: (data: BatchJobSaveRequest) =>
    api.post<ApiResponse<BatchJob>>('/batch/jobs', data),

  /** 수정 */
  update: (procName: string, data: BatchJobSaveRequest) =>
    api.put<ApiResponse<BatchJob>>(`/batch/jobs/${procName}`, data),

  /** 삭제 */
  remove: (procName: string) =>
    api.delete<ApiResponse<void>>(`/batch/jobs/${procName}`),

  /** 수동 실행(재실행) */
  run: (procName: string) =>
    api.post<ApiResponse<BatchHistory>>(`/batch/jobs/${procName}/run`),

  /** 실행 이력(페이징) */
  getHistory: (procName: string, page = 1, size = 20) =>
    api.get<ApiResponse<PageResult<BatchHistory>>>(`/batch/jobs/${procName}/history`, {
      params: { page, size },
    }),

  /** Cron 표현식 검증 */
  validateCron: (expr: string) =>
    api.get<ApiResponse<CronValidateResult>>('/batch/cron/validate', { params: { expr } }),
};
