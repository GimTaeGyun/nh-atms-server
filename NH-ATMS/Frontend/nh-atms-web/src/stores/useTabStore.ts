import {create} from 'zustand';
import type {TabItem} from '@/types/menu';
//메뉴 탭 상태관리
interface TabState {
    tabs: TabItem[];
    activeKey: string;
    //각 탭의 페이지 상태를 보관(조회조건, 조회결과 등)
    tabPageStates: Record<string, unknown>;
    
    addTab: (tab: TabItem) => void;
    removeTab: (key: string) => void;
    closeAll: () => void;
    setActiveKey: (key: string) => void;
    setPageState: (key: string, state: unknown) => void;
    getPageState: (key: string) => unknown;
}

const STORAGE_KEY = 'nh-atms-tabs';
const STATE_STORAGE_KEY = 'nh-atms-tab-states';

//sessionStorage에서 초기값 복원 
const loadTabs = (): TabItem[] => {
    try{
        const saved = sessionStorage.getItem(STORAGE_KEY);
        return saved ? JSON.parse(saved) : [];
    }catch{
        return [];
    }
}

const loadPageStates = (): Record<string, unknown> => {
    try{
        const saved = sessionStorage.getItem(STATE_STORAGE_KEY);
        return saved ? JSON.parse(saved) : {};
    }catch{
        return {};
    }
}

const saveTabs = (tabs: TabItem[]) => {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(tabs));
}

const savePageStates = (states: Record<string, unknown>) => {
    sessionStorage.setItem(STATE_STORAGE_KEY, JSON.stringify(states));
};

export const useTabStore = create<TabState>((set, get) => ({
    tabs: loadTabs(),
    activeKey: '',
    tabPageStates: loadPageStates(),

    addTab: (tab: TabItem) => {
        const {tabs} = get();
        const exists = tabs.find((t) => t.key === tab.key);

        if(!exists){
            const newTabs = [...tabs, tab];
            saveTabs(newTabs);
            set({tabs: newTabs, activeKey: tab.key});
        }else{
            set({activeKey: tab.key});
        }
    },

    removeTab: (key: string) => {
        const {tabs, activeKey, tabPageStates} = get();
        const newTabs = tabs.filter((t) => t.key !== key);
        const newStates = {...tabPageStates};
        delete newStates[key];

        saveTabs(newTabs);
        savePageStates(newStates);

        //닫은 탭이 활성탭이면 마지막 탭으로 이동
        if(activeKey === key && newTabs.length > 0){
            set({tabs: newTabs, activeKey: newTabs[newTabs.length - 1].key, tabPageStates: newStates});
        }else{
            set({tabs: newTabs, tabPageStates: newStates});
        }
    },

    //전체 닫기 — 고정탭(closable===false)은 유지
    closeAll: () => {
        const {tabs} = get();
        const kept = tabs.filter((t) => t.closable === false);
        saveTabs(kept);
        savePageStates({});
        set({
            tabs: kept,
            activeKey: kept.length > 0 ? kept[kept.length - 1].key : '',
            tabPageStates: {},
        });
    },

    setActiveKey: (key: string) => {
        set({activeKey: key});
    },

    //페이지 상태 저장(조회조건, 조회결과 등)
    setPageState: (key: string, state: unknown) => {
        const {tabPageStates} = get();
        const newStates = {...tabPageStates, [key]: state};
        savePageStates(newStates);
        set({tabPageStates: newStates});
    },

    getPageState: (key: string) => {
        return get().tabPageStates[key];
    }

}));