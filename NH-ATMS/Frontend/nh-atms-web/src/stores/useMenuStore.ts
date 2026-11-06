import { create } from 'zustand';
import { menuApi } from '@/api/menuApi';
import type { MenuTreeNode } from '@/types/menu';

const STORAGE_KEY = 'nh-atms-menus';

const loadFromSession = (): MenuTreeNode[] => {
  try {
    const saved = sessionStorage.getItem(STORAGE_KEY);
    return saved ? JSON.parse(saved) : [];
  } catch {
    return [];
  }
};

const saveToSession = (menus: MenuTreeNode[]) => {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(menus));
};

interface MenuState {
  menus: MenuTreeNode[];
  loaded: boolean;
  loading: boolean;
  error: string | null;
  fetchMenus: () => Promise<void>;
  clearMenus: () => void;
}

export const useMenuStore = create<MenuState>((set, get) => ({
  menus: loadFromSession(),
  loaded: loadFromSession().length > 0,
  loading: false,
  error: null,

  fetchMenus: async () => {
    if (get().loading) return;
    set({ loading: true, error: null });
    try {
      const res = await menuApi.getMyMenus();
      const menus = res.data.data ?? [];
      saveToSession(menus);
      set({ menus, loaded: true, loading: false });
    } catch (e: any) {
      set({ loading: false, error: e?.message ?? '메뉴 로드 실패' });
    }
  },

  clearMenus: () => {
    sessionStorage.removeItem(STORAGE_KEY);
    set({ menus: [], loaded: false, error: null });
  },
}));
