import axios from 'axios';
import { useAuthStore } from '@/stores/useAuthStore';

const api = axios.create({
    baseURL: '/api',
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json'
    }
});

//요청 인터셉터: Access Token 자동 첨부 
api.interceptors.request.use((config) => {
    const token = sessionStorage.getItem('accessToken');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
})

//응답 인터셉터: 401시 토큰 갱신 시도 
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        // 로그아웃/로그인 요청은 인터셉터 처리 안함
        if (originalRequest.url?.includes('/auth/logout') || originalRequest.url?.includes('/auth/login')) {
            return Promise.reject(error);
        }

        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;

            const refreshToken = sessionStorage.getItem("refreshToken");

            if (refreshToken) {
                try {
                    const res = await axios.post('/api/auth/refresh', { refreshToken });
                    const { accessToken, refreshToken: newRefreshToken, user } = res.data.data;

                    useAuthStore.getState().login(accessToken, newRefreshToken, user);
                    originalRequest.headers.Authorization = `Bearer ${accessToken}`;

                    return api(originalRequest);
                } catch {
                    useAuthStore.getState().logout();
                    window.location.href = '/login';
                }
            } else {
                useAuthStore.getState().logout();
                window.location.href = '/login';
            }
        }
        return Promise.reject(error);
    }
)

export default api;

