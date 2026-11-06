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

export interface AttachedFile {
  catCd: string;
  seqNo: number;
  fileNo: number;
  attachedFileName: string;
  attachedFileSize: number;
}

export interface Notice {
  catCd: string;
  seqNo: number;
  subject: string;
  content: string;
  hit: number;
  step?: number;
  depth?: number;
  enterDate: string;
  enterUserId: string;
  enterUserName?: string;
  enterTermGroupId?: string;
  attachedFileFlag?: string;
  parentSeqNo?: number;
  bankCd?: string;
  targetUser?: string;
  useYn?: string;
  startDate?: string;
  endDate?: string;
}

export interface NoticeSearchParams {
  catCd?: string;
  subject?: string;
  startDate?: string;
  endDate?: string;
  useYn?: string;
  page?: number;
  size?: number;
}

const BASE = '/notice';

export const noticeApi = {
  /** 목록 */
  getList: (params?: NoticeSearchParams) =>
    api.get<ApiResponse<PageResult<Notice>>>(BASE, { params }),

  /** 첨부파일 목록 */
  getFiles: (catCd: string, seqNo: number) =>
    api.get<ApiResponse<AttachedFile[]>>(`${BASE}/${catCd}/${seqNo}/files`),

  /** 조회수 증가 */
  increaseHit: (catCd: string, seqNo: number) =>
    api.patch<ApiResponse<{ hit: number }>>(`${BASE}/${catCd}/${seqNo}/hit`),

  /** 등록 — Step1: JSON, Step2: 파일(있을 때만) */
  create: async (notice: Partial<Notice>, files?: File[]) => {
    const res = await api.post<ApiResponse<{ seqNo: number }>>(BASE, notice);
    const seqNo = res.data.data?.seqNo;
    if (files && files.length > 0 && seqNo) {
      const fd = new FormData();
      files.forEach((f) => fd.append('files', f));
      await api.post(`${BASE}/${notice.catCd}/${seqNo}/files`, fd, {
        headers: { 'Content-Type': undefined },
      });
    }
    return res;
  },

  /** 수정 — Step1: JSON, Step2: 파일(있을 때만) */
  update: async (catCd: string, seqNo: number, notice: Partial<Notice>, files?: File[]) => {
    const res = await api.put<ApiResponse<void>>(`${BASE}/${catCd}/${seqNo}`, notice);
    if (files && files.length > 0) {
      const fd = new FormData();
      files.forEach((f) => fd.append('files', f));
      await api.post(`${BASE}/${catCd}/${seqNo}/files`, fd, {
        headers: { 'Content-Type': undefined },
      });
    }
    return res;
  },

  /** 삭제 */
  delete: (catCd: string, seqNo: number) =>
    api.delete<ApiResponse<void>>(`${BASE}/${catCd}/${seqNo}`),

  /** 첨부파일 단건 삭제 */
  deleteFile: (catCd: string, seqNo: number, fileNo: number) =>
    api.delete<ApiResponse<void>>(`${BASE}/${catCd}/${seqNo}/files/${fileNo}`),

  /**
   * 파일 다운로드 — axios로 Blob 수신 후 브라우저 임시 링크 클릭 방식
   * (href 방식은 Authorization 헤더를 포함할 수 없어 401 발생)
   */
  download: async (catCd: string, seqNo: number, fileNo: number, fileName: string) => {
    const res = await api.get(
      `${BASE}/${catCd}/${seqNo}/files/${fileNo}/download`,
      { responseType: 'blob' },
    );
    const url = URL.createObjectURL(new Blob([res.data]));
    const a   = document.createElement('a');
    a.href     = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  },
};
