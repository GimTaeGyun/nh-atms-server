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

export interface TransactionJournal {
  termGroupId: string;
  termId: string;
  jnlDate: string;
  jnlTime: string;
  trNo?: string;
  trType?: string;
  trFromAccount?: string;
  trToAccount?: string;
  bankCd?: string;
  accountNo?: string;
  toBankCd?: string;
  toAccountNo?: string;
  trHostDate?: string;
  trHostTime?: string;
  trHostNo?: string;
  trHostAuditNo?: string;
  hostNetId?: string;
  hostSettleDate?: string;
  depositNo?: string;
  surcharge?: number;
  requestAmount?: number;
  dispenseAmount?: number;
  remainAmount?: number;
  availAmount?: number;
  hostCashAmount?: number;
  hostCheckAmount?: number;
  procCount?: string;
  trResult?: string;
  errCd1?: string;
  errCd2?: string;
  errCd3?: string;
  cardData?: string;
  nonCashValue?: string;
  nonCashType?: string;
  trMediaCd?: string;
  message?: string;
  totalTypeCd?: string;
  transferName?: string;
}

export interface TransactionJournalSearchParams {
  termGroupId?: string;
  termId?: string;
  trType?: string;
  trTypes?: string[];
  accountNo?: string;
  cardData?: string;
  trResult?: string;
  fromDate?: string;
  toDate?: string;
  errorOnly?: boolean;
  page?: number;
  size?: number;
}

export interface JournalImage {
  termGroupId: string;
  termId: string;
  jnlDate: string;
  jnlTime: string;
  imageType: string;
  imagePath: string;
}

export interface TransactionJournalDetail {
  journal: TransactionJournal;
  journalText?: string;
  images: JournalImage[];
}

export const transactionJournalApi = {
  getList: (params: TransactionJournalSearchParams) =>
    api.get<ApiResponse<PageResult<TransactionJournal>>>('/transaction-journal', {
      params,
      // trTypes 배열을 trTypes=a&trTypes=b 형식으로 직렬화 (Spring @ModelAttribute List 바인딩 호환)
      paramsSerializer: { indexes: null },
    }),
  getOne: (termGroupId: string, termId: string, jnlDate: string, jnlTime: string) =>
    api.get<ApiResponse<TransactionJournal>>(
      `/transaction-journal/${termGroupId}/${termId}/${jnlDate}/${jnlTime}`,
    ),
  getDetail: (termGroupId: string, termId: string, jnlDate: string, jnlTime: string) =>
    api.get<ApiResponse<TransactionJournalDetail>>(
      `/transaction-journal/${termGroupId}/${termId}/${jnlDate}/${jnlTime}/detail`,
    ),
  /** 저널 첨부 이미지(JPEG) 바이너리 다운로드 → blob */
  getImageBlob: (
    termGroupId: string,
    termId: string,
    jnlDate: string,
    jnlTime: string,
    imageType: string,
  ) =>
    api.get<Blob>('/file/journal-image/download', {
      params: { termGroupId, termId, jnlDate, jnlTime, imageType },
      responseType: 'blob',
    }),
};
