import api from './axiosInstance';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  code: string;
  data: T;
}

export interface Role {
  authCode: string;
  authName: string;
  authDesc?: string;
  authType?: string;
  fstgMenuNo?: number;
  useYn?: string;
  regDt?: string;
  updDt?: string;
}

/** 접근권한 팝업 좌측: 특정 메뉴에 대한 역할별 접근 현황 */
export interface MenuRolePerm {
  authCode: string;
  authName: string;
  accessYn: 'Y' | 'N';
}

/** 접근권한 팝업 우측: 특정 메뉴 + 역할의 조회/수정 권한 상세 */
export interface MenuRolePermDetail {
  authCode: string;
  menuNo: number;
  readYn:  'Y' | 'N';   // 조회 권한
  writeYn: 'Y' | 'N';   // 수정 권한
}

/** 접근권한 저장 요청 body */
export interface MenuRolePermSaveRequest {
  readYn:  'Y' | 'N';
  writeYn: 'Y' | 'N';
}

export const roleApi = {
  /** 전체 역할 목록 */
  all: () =>
    api.get<ApiResponse<Role[]>>('/role/all'),

  /** 특정 메뉴의 역할별 접근권한 목록 (접근권한 팝업 좌측) */
  getMenuRoles: (menuNo: number) =>
    api.get<ApiResponse<MenuRolePerm[]>>(`/role/menu/${menuNo}`),

  /** 특정 메뉴 + 역할의 조회/수정 권한 조회 (접근권한 팝업 우측) */
  getMenuRolePerm: (menuNo: number, authCode: string) =>
    api.get<ApiResponse<MenuRolePermDetail>>(`/role/menu/${menuNo}/perm/${authCode}`),

  /** 특정 메뉴 + 역할의 조회/수정 권한 저장 */
  saveMenuRolePerm: (menuNo: number, authCode: string, body: MenuRolePermSaveRequest) =>
    api.put<ApiResponse<void>>(`/role/menu/${menuNo}/perm/${authCode}`, body),
};
