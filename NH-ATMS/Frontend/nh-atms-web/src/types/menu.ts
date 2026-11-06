/** 백엔드 /api/menu/my 응답 노드 */
export interface MenuTreeNode {
  menuNo: number;
  parentNo?: number;
  menuName: string;
  menuUrl?: string;
  menuLevel?: number;
  menuOrder?: number;
  menuIcon?: string;
  menuType?: string;   // M:메뉴 D:디렉토리
  children?: MenuTreeNode[];
}

/** 탭 아이템 */
export interface TabItem {
  key: string;
  label: string;
  path: string;
  closable: boolean;
}
