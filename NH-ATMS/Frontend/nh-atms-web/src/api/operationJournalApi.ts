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

export interface OperationJournal {
  termGroupId: string;
  termId: string;
  jnlDate: string;
  jnlTime: string;
  operationCd: string;
  operationAction?: string;
}

export interface OperationJournalDetail {
  journal: OperationJournal;
  journalText?: string;
}

export interface OperationJournalSearchParams {
  termGroupId?: string;
  termId?: string;
  operationCd?: string;
  keyword?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

export const operationJournalApi = {
  getList: (params: OperationJournalSearchParams) =>
    api.get<ApiResponse<PageResult<OperationJournal>>>('/operation-journal', { params }),
  getDetail: (termGroupId: string, termId: string, jnlDate: string, jnlTime: string) =>
    api.get<ApiResponse<OperationJournalDetail>>(
      `/operation-journal/${termGroupId}/${termId}/${jnlDate}/${jnlTime}/detail`,
    ),
};
