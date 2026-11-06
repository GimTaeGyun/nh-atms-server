import { create } from 'zustand';
import api from '@/api/axiosInstance';
//인증 상태관리
interface UserInfo {
    userId: string;
    userName: string;
    deptName: string;
    roleId: string;
}

interface AuthState {
    isAuthenticated: boolean;
    accessToken: string | null;
    refreshToken: string | null;
    user: UserInfo | null;
    login: (token: string, refreshToken: string, user: UserInfo) => void;
    logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
    isAuthenticated: !!sessionStorage.getItem('accessToken'),
    accessToken: sessionStorage.getItem('accessToken'),
    refreshToken: sessionStorage.getItem('refreshToken'),
    user: JSON.parse(sessionStorage.getItem('userInfo') || 'null'),

    login: (accessToken: string, refreshToken: string, user: UserInfo) => {
        sessionStorage.setItem('accessToken', accessToken);
        sessionStorage.setItem('refreshToken', refreshToken);
        sessionStorage.setItem('userInfo', JSON.stringify(user));
        set({ isAuthenticated: true, accessToken, refreshToken, user });
    },

    logout: () => {
        // 토큰이 있을 때만 Backend 호출
        const token = sessionStorage.getItem('accessToken');
        if (token) {
            api.post('/auth/logout').catch(() => {});
        }

        // 메뉴 캐시 초기화 (순환참조 방지를 위해 동적 import)
        import('@/stores/useMenuStore').then(({ useMenuStore }) => {
            useMenuStore.getState().clearMenus();
        });

        // 상태 초기화 (API 호출 후)
        sessionStorage.clear();
        set({ isAuthenticated: false, accessToken: null, refreshToken: null, user: null });
        window.location.href = '/login';
    }
}));