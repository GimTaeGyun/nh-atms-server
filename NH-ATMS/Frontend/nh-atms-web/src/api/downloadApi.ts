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

/** 패키지 유형 : A=AP, G=일반, M=매체 */
export type PackageType = 'A' | 'G' | 'M';
/** 결재상태 : N=미결재, Y=결재완료, R=반려 */
export type ConfirmStatus = 'N' | 'Y' | 'R';

/** 개별다운로드관리 대상 기기(현재버전 vs 최종버전) */
export interface DownloadDevice {
  termGroupId: string;
  termId: string;
  branchCd?: string;
  cornerCd?: string;
  locationName?: string;
  atmCoCd?: string;
  modelCd?: string;
  modelName?: string;
  atmIp?: string;
  appVersion?: string;
  agentVer?: string;
  apDownVer?: string;
  lastAppVer?: string;
  connectFlag?: string;
}

/** 다운로드 패키지 */
export interface DownloadPackage {
  pkgId: number;
  pkgName?: string;
  pkgType: PackageType;
  atmCoCd: string;
  modelCd: string;
  modelName?: string;
  downVer: string;
  fileName: string;
  fileSize?: number;
  filePath?: string;
  downloadPath?: string;
  execCmd?: string;
  applyDate?: string;
  applyTime?: string;
  crcValue?: string;
  confirmStatus: ConfirmStatus;
  confirmUserId?: string;
  confirmDt?: string;
  useYn?: string;
  regUserId?: string;
  regDt?: string;
  updDt?: string;
}

/** 다운로드 작업/배포 이력 */
export interface DownloadJob {
  jobId: number;
  pkgId: number;
  termGroupId: string;
  termId: string;
  execType: string; // I/B
  status: string; // W/S/C/F/X
  cmdId?: string;
  reserveDate?: string;
  reserveTime?: string;
  resultCd?: string;
  resultMsg?: string;
  resultDt?: string;
  regUserId?: string;
  regDt?: string;
  pkgName?: string;
  pkgType?: PackageType;
  downVer?: string;
  fileName?: string;
}

export interface DownloadDeviceSearchParams {
  termGroupId?: string;
  branchCd?: string;
  cornerCd?: string;
  termId?: string;
  atmCoCd?: string;
  modelCd?: string;
  page?: number;
  size?: number;
}

export interface DownloadPackageSearchParams {
  atmCoCd?: string;
  modelCd?: string;
  pkgType?: string;
  confirmStatus?: string;
  page?: number;
  size?: number;
}

export interface DownloadJobSearchParams {
  pkgId?: number;
  termGroupId?: string;
  termId?: string;
  execType?: string;
  status?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

export interface DownloadPackageSaveRequest {
  pkgName?: string;
  pkgType: PackageType;
  atmCoCd: string;
  modelCd: string;
  downVer: string;
  fileName: string;
  fileSize?: number;
  filePath?: string;
  downloadPath?: string;
  execCmd?: string;
  applyDate?: string;
  applyTime?: string;
  crcValue?: string;
}

export interface IndividualUploadMeta {
  appType: PackageType;
  /** 대표 기기업체(선택, 마스터 기록용) */
  atmCoCd?: string;
  /** 대표 모델(선택, 마스터 기록용) */
  modelCd?: string;
  appVer: string;
  execType?: string;
  downloadPath?: string;
  applyMemo?: string;
  targets: { termGroupId: string; termId: string }[];
}

/** 개별다운로드 적용이력 */
export interface IndividualApplyHistory {
  downNo: string;
  termGroupId: string;
  termId: string;
  cmdId?: string;
  startDate?: string;
  startTime?: string;
  endDate?: string;
  endTime?: string;
  applyFlag?: string; // 0=미적용,1=적용완료,2=다운로드중
  applyResult?: string; // 0=실패,1=성공
  asyncUid?: number;
  regUserId?: string;
  regDt?: string;
  appTypeCd?: string;
  appFileName?: string;
  appVer?: string;
  atmCoCd?: string;
  modelCd?: string;
}

export interface IndividualHistorySearchParams {
  downNo?: string;
  termGroupId?: string;
  termId?: string;
  applyFlag?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

export const downloadApi = {
  // ── 개별다운로드관리 ──────────────────────────────────────────
  /** 개별 배포 대상 기기 목록 */
  getDevices: (params?: DownloadDeviceSearchParams) =>
    api.get<ApiResponse<PageResult<DownloadDevice>>>('/download/devices', { params }),

  /** ZIP 파일을 업로드하여 선택 기기들에 개별 배포(결재 무관) */
  uploadIndividual: (file: File, meta: IndividualUploadMeta) => {
    const form = new FormData();
    form.append('file', file);
    form.append(
      'request',
      new Blob([JSON.stringify(meta)], { type: 'application/json' }),
    );
    return api.post<ApiResponse<number>>('/download/individual/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  /** 개별다운로드 적용이력 */
  getIndividualHistory: (params?: IndividualHistorySearchParams) =>
    api.get<ApiResponse<PageResult<IndividualApplyHistory>>>(
      '/download/individual/history',
      { params },
    ),

  // ── 일괄다운로드관리 ──────────────────────────────────────────
  /** 다운로드 패키지 목록 */
  getPackages: (params?: DownloadPackageSearchParams) =>
    api.get<ApiResponse<PageResult<DownloadPackage>>>('/download/packages', { params }),

  /** 패키지 단건 조회 */
  getPackage: (pkgId: number) =>
    api.get<ApiResponse<DownloadPackage>>(`/download/packages/${pkgId}`),

  /** 패키지 등록 */
  registerPackage: (body: DownloadPackageSaveRequest) =>
    api.post<ApiResponse<DownloadPackage>>('/download/packages', body),

  /** 결재(승인) */
  confirm: (pkgId: number) =>
    api.post<ApiResponse<void>>(`/download/packages/${pkgId}/confirm`),

  /** 반려 */
  reject: (pkgId: number) =>
    api.post<ApiResponse<void>>(`/download/packages/${pkgId}/reject`),

  /** 일괄 배포(기종 전체) */
  distributeBatch: (pkgId: number) =>
    api.post<ApiResponse<number>>(`/download/packages/${pkgId}/distribute`),

  // ── 배포 이력 ─────────────────────────────────────────────────
  getJobs: (params?: DownloadJobSearchParams) =>
    api.get<ApiResponse<PageResult<DownloadJob>>>('/download/jobs', { params }),
};
