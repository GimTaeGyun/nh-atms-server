import api from './axiosInstance';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  code: string;
  data: T;
}

// ── T_CODEGROUP ────────────────────────────────────────────────
export interface CodeGroup {
  langCd: string;
  groupCd: string;
  groupName: string;
}

export interface CodeGroupSaveRequest {
  langCd: string;
  groupCd: string;
  groupName: string;
}

// ── T_CODE ────────────────────────────────────────────────────
export interface CodeItem {
  langCd: string;
  groupCd: string;
  code: string;
  name: string;
  orderNo: number;
}

export interface CodeItemSaveRequest {
  langCd: string;
  groupCd: string;
  code: string;
  name: string;
  orderNo: number;
}

export const codeApi = {
  // ── T_CODEGROUP ──────────────────────────────────────────────

  /** 그룹코드 전체 목록 */
  getGroups: () =>
    api.get<ApiResponse<CodeGroup[]>>('/code/groups'),

  /** 그룹코드 단건 조회 */
  getOneGroup: (langCd: string, groupCd: string) =>
    api.get<ApiResponse<CodeGroup>>(`/code/groups/${langCd}/${groupCd}`),

  /** 그룹코드 등록 */
  createGroup: (data: CodeGroupSaveRequest) =>
    api.post<ApiResponse<void>>('/code/groups', data),

  /** 그룹코드 수정 */
  updateGroup: (langCd: string, groupCd: string, data: Partial<CodeGroupSaveRequest>) =>
    api.put<ApiResponse<void>>(`/code/groups/${langCd}/${groupCd}`, data),

  /** 그룹코드 삭제 */
  removeGroup: (langCd: string, groupCd: string) =>
    api.delete<ApiResponse<void>>(`/code/groups/${langCd}/${groupCd}`),

  // ── T_CODE ────────────────────────────────────────────────────

  /** 특정 그룹의 코드 목록 */
  getCodes: (langCd: string, groupCd: string) =>
    api.get<ApiResponse<CodeItem[]>>(`/code/${langCd}/${groupCd}`),

  /** 코드 단건 조회 */
  getOneCode: (langCd: string, groupCd: string, code: string) =>
    api.get<ApiResponse<CodeItem>>(`/code/${langCd}/${groupCd}/${code}`),

  /** 코드 등록 */
  createCode: (data: CodeItemSaveRequest) =>
    api.post<ApiResponse<void>>('/code', data),

  /** 코드 수정 */
  updateCode: (langCd: string, groupCd: string, code: string, data: Partial<CodeItemSaveRequest>) =>
    api.put<ApiResponse<void>>(`/code/${langCd}/${groupCd}/${code}`, data),

  /** 코드 삭제 */
  removeCode: (langCd: string, groupCd: string, code: string) =>
    api.delete<ApiResponse<void>>(`/code/${langCd}/${groupCd}/${code}`),
};
