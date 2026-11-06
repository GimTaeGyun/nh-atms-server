import api from './axiosInstance';
import type { MenuTreeNode } from '@/types/menu';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  code: string;
  data: T;
}

/** 메뉴 엔티티 (관리화면용 - 전체 필드) */
export interface MenuEntity {
  menuNo?: number;
  parentNo?: number | null;
  menuName: string;
  menuUrl?: string | null;
  menuLevel?: number;
  menuOrder?: number;
  menuIcon?: string | null;
  menuType?: string;
  visibleYn?: string;
  useYn?: string;
  description?: string | null;
}

export const menuApi = {
  /** 로그인 사용자 권한 기반 메뉴 트리 */
  getMyMenus: () =>
    api.get<ApiResponse<MenuTreeNode[]>>('/menu/my'),

  /** 전체 메뉴 트리 (관리자) */
  getAllMenus: () =>
    api.get<ApiResponse<MenuTreeNode[]>>('/menu/all'),

  getOne: (menuNo: number) =>
    api.get<ApiResponse<MenuEntity>>(`/menu/${menuNo}`),

  create: (menu: MenuEntity) =>
    api.post<ApiResponse<MenuEntity>>('/menu', menu),

  update: (menuNo: number, menu: MenuEntity) =>
    api.put<ApiResponse<void>>(`/menu/${menuNo}`, menu),

  remove: (menuNo: number) =>
    api.delete<ApiResponse<void>>(`/menu/${menuNo}`),
};

