import { create } from 'zustand';
import { branchApi } from '@/api/branchApi';

interface BranchState {
  /** termGroupId(영업점코드) → 영업점명 */
  nameMap: Record<string, string>;
  loaded: boolean;
  loading: boolean;
  /** 최초 1회만 로드(이미 로드/로딩 중이면 무시) */
  load: () => Promise<void>;
  /** 코드 → "영업점명(코드)" 표기. 매핑 없으면 코드 그대로 */
  label: (termGroupId?: string) => string;
}

export const useBranchStore = create<BranchState>((set, get) => ({
  nameMap: {},
  loaded: false,
  loading: false,

  load: async () => {
    const { loaded, loading } = get();
    if (loaded || loading) return;
    set({ loading: true });
    try {
      const res = await branchApi.getList();
      const map: Record<string, string> = {};
      for (const b of res.data.data.content) {
        if (b.termGroupId && b.branchName) {
          map[b.termGroupId.trim()] = b.branchName.trim();
        }
      }
      set({ nameMap: map, loaded: true });
    } catch {
      // 실패해도 코드 그대로 표시 (조회 방해 X)
      set({ loaded: true });
    } finally {
      set({ loading: false });
    }
  },

  label: (termGroupId?: string) => {
    if (!termGroupId) return '-';
    const code = termGroupId.trim();
    const name = get().nameMap[code];
    return name ? `${name}(${code})` : code;
  },
}));
