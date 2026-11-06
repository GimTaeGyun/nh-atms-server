import { create } from 'zustand';

const STORAGE_KEY = 'nh-atms-favorites';

const load = (): number[] => {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    const arr = saved ? JSON.parse(saved) : [];
    return Array.isArray(arr) ? arr.filter((v) => typeof v === 'number') : [];
  } catch {
    return [];
  }
};

const save = (ids: number[]) => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(ids));
  } catch {
    /* localStorage 사용 불가 시 무시 */
  }
};

interface FavoriteState {
  /** 즐겨찾기한 메뉴번호(menuNo) 목록 */
  favorites: number[];
  isFavorite: (menuNo: number) => boolean;
  toggle: (menuNo: number) => void;
}

export const useFavoriteStore = create<FavoriteState>((set, get) => ({
  favorites: load(),

  isFavorite: (menuNo: number) => get().favorites.includes(menuNo),

  toggle: (menuNo: number) => {
    const cur = get().favorites;
    const next = cur.includes(menuNo)
      ? cur.filter((n) => n !== menuNo)
      : [...cur, menuNo];
    save(next);
    set({ favorites: next });
  },
}));
