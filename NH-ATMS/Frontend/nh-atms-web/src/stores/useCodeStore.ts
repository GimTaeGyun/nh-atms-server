import { create } from 'zustand';
import { codeApi } from '@/api/codeApi';

/** 기본 언어코드 (T_CODE.LANG_CD) */
const DEFAULT_LANG = 'KOR';

interface CodeState {
  /** groupCd → (code → name) */
  maps: Record<string, Record<string, string>>;
  /** 로드 완료한 groupCd */
  loaded: Record<string, boolean>;
  /** 로드 진행중인 groupCd (중복요청 방지) */
  loading: Record<string, boolean>;
  /** 해당 그룹을 1회만 로드(이미 로드/로딩 중이면 무시) */
  ensure: (groupCd: string, langCd?: string) => Promise<void>;
  /** 코드 → 명칭. 매핑 없으면 코드 그대로 */
  name: (groupCd?: string, code?: string) => string;
  /** 코드 → "명칭(코드)" 표기. 매핑 없으면 코드 그대로 */
  label: (groupCd?: string, code?: string) => string;
}

export const useCodeStore = create<CodeState>((set, get) => ({
  maps: {},
  loaded: {},
  loading: {},

  ensure: async (groupCd: string, langCd: string = DEFAULT_LANG) => {
    if (!groupCd) return;
    const key = groupCd.trim();
    const { loaded, loading } = get();
    if (loaded[key] || loading[key]) return;
    set((s) => ({ loading: { ...s.loading, [key]: true } }));
    try {
      const res = await codeApi.getCodes(langCd, key);
      const map: Record<string, string> = {};
      for (const c of res.data.data) {
        if (c.code != null && c.name) {
          map[String(c.code).trim()] = c.name.trim();
        }
      }
      set((s) => ({
        maps: { ...s.maps, [key]: map },
        loaded: { ...s.loaded, [key]: true },
      }));
    } catch {
      // 실패해도 코드 그대로 표시 (조회 방해 X)
      set((s) => ({ loaded: { ...s.loaded, [key]: true } }));
    } finally {
      set((s) => ({ loading: { ...s.loading, [key]: false } }));
    }
  },

  name: (groupCd?: string, code?: string) => {
    if (!code) return '-';
    const c = code.trim();
    if (!groupCd) return c;
    return get().maps[groupCd.trim()]?.[c] ?? c;
  },

  label: (groupCd?: string, code?: string) => {
    if (!code) return '-';
    const c = code.trim();
    if (!groupCd) return c;
    const nm = get().maps[groupCd.trim()]?.[c];
    return nm ? `${nm}(${c})` : c;
  },
}));
