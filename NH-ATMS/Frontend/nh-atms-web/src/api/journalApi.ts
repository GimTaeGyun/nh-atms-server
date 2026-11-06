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

export interface Journal {
  termGroupId: string;
  termId: string;
  jnlDate: string;
  jnlTime: string;
  agentType?: string;
  seqNo?: string;
  jnlTypeCd?: string;
  jnlWriteTypeCd?: string;
  recvDate?: string;
  recvTime?: string;
}

export interface JournalDetail {
  journal: Journal;
  journalText?: string;
}

export interface JournalSearchParams {
  termGroupId?: string;
  termId?: string;
  jnlTypeCd?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

export const journalApi = {
  getList: (params: JournalSearchParams) =>
    api.get<ApiResponse<PageResult<Journal>>>('/journals', { params }),
  getDetail: (termGroupId: string, termId: string, jnlDate: string, jnlTime: string) =>
    api.get<ApiResponse<JournalDetail>>(
      `/journals/${termGroupId}/${termId}/${jnlDate}/${jnlTime}/detail`,
    ),
};
