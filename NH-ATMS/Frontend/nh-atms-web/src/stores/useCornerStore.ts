import { create } from 'zustand';
import { cornerApi } from '@/api/cornerApi';

interface CornerState {
  /** "termGroupId:cornerCd" → 코너명 */
  nameMap: Record<string, string>;
  loaded: boolean;
  loading: boolean;
  /** 최초 1회만 전체 로드 */
  load: () => Promise<void>;
  /** 특정 영업점의 코너만 지연 로드 */
  loadByGroup: (termGroupId: string) => Promise<void>;
  /** (termGroupId, cornerCd) → "코너명(코드)" or 코드 그대로 */
  label: (termGroupId?: string, cornerCd?: string) => string;
}

export const useCornerStore = create<CornerState>((set, get) => ({
  nameMap: {},
  loaded: false,
  loading: false,

  load: async () => {
    const { loaded, loading } = get();
    if (loaded || loading) return;
    set({ loading: true });
    try {
      const res = await cornerApi.getList({ size: 5000 });
      const map: Record<string, string> = {};
      for (const c of res.data.data.content) {
        if (c.termGroupId && c.cornerCd && c.cornerName) {
          map[`${c.termGroupId.trim()}:${c.cornerCd.trim()}`] = c.cornerName.trim();
        }
      }
      set({ nameMap: map, loaded: true });
    } catch {
      set({ loaded: true });
    } finally {
      set({ loading: false });
    }
  },

  loadByGroup: async (termGroupId: string) => {
    // 이미 이 그룹의 데이터가 있으면 skip
    const existing = Object.keys(get().nameMap).some((k) => k.startsWith(`${termGroupId}:`));
    if (existing) return;
    try {
      const res = await cornerApi.getList({ termGroupId, size: 500 });
      const map: Record<string, string> = { ...get().nameMap };
      for (const c of res.data.data.content) {
        if (c.termGroupId && c.cornerCd && c.cornerName) {
          map[`${c.termGroupId.trim()}:${c.cornerCd.trim()}`] = c.cornerName.trim();
        }
      }
      set({ nameMap: map });
    } catch {
      // 실패해도 코드 그대로 표시
    }
  },

  label: (termGroupId?: string, cornerCd?: string) => {
    if (!termGroupId || !cornerCd) return cornerCd ?? '-';
    const key = `${termGroupId.trim()}:${cornerCd.trim()}`;
    const name = get().nameMap[key];
    return name ? `${name}(${cornerCd.trim()})` : cornerCd.trim();
  },
}));
