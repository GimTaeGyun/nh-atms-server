import React from 'react';
import { Form, Input, Button, message } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/stores/useAuthStore';
import api from '@/api/axiosInstance';
import { encryptField, handleFieldCryptoError } from '@/utils/fieldCrypto';

const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const login = useAuthStore((s) => s.login);
  const [loading, setLoading] = React.useState(false);

  const handleLogin = async (values: { userId: string; password: string }) => {
    setLoading(true);
    try {
      // 비밀번호는 암호화해서 보냄 (docs/FIELD_ENCRYPTION_DESIGN.md)
      const res = await api.post('/auth/login', { ...values, password: await encryptField(values.password) });
      const { accessToken, refreshToken, user } = res.data.data;

      login(accessToken, refreshToken, user);
      message.success(`${user.userName}님, 환영합니다.`);
      navigate('/dashboard');
    } catch (error: any) {
      handleFieldCryptoError(error); // 서버 키 교체 등 → 다음 시도에서 공개키를 다시 받음
      const msg = error.response?.data?.message || '로그인에 실패했습니다.';
      message.error(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
        background: 'linear-gradient(160deg, #0a1628 0%, #132744 40%, #1a3a5c 100%)',
      }}
    >
      <div
        style={{
          width: 420,
          background: 'rgba(255,255,255,0.06)',
          backdropFilter: 'blur(20px)',
          borderRadius: 16,
          border: '1px solid rgba(255,255,255,0.1)',
          boxShadow: '0 8px 40px rgba(0,0,0,0.4)',
          overflow: 'hidden',
        }}
      >
        {/* 상단 로고 영역 */}
        <div style={{ padding: '40px 0 24px', textAlign: 'center' }}>
          <img
            src="/nh_logo.png"
            alt="NH농협은행"
            style={{
              height: 44,
              objectFit: 'contain',
              marginBottom: 12,
            }}
            onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
          />
          <div
            style={{
              color: '#fff',
              fontSize: 15,
              fontWeight: 400,
              letterSpacing: 1,
              opacity: 0.85,
            }}
          >
            ATM Total Management System
          </div>
        </div>

        {/* 로그인 폼 */}
        <div style={{ padding: '8px 36px 36px' }}>
          <Form onFinish={handleLogin} size="large">
            <Form.Item name="userId" rules={[{ required: true, message: '아이디를 입력하세요' }]}>
              <Input
                prefix={<UserOutlined style={{ color: 'rgba(255,255,255,0.4)' }} />}
                placeholder="아이디"
                style={{
                  background: 'rgba(255,255,255,0.08)',
                  border: '1px solid rgba(255,255,255,0.15)',
                  borderRadius: 8,
                  color: '#fff',
                  height: 48,
                }}
              />
            </Form.Item>
            <Form.Item name="password" rules={[{ required: true, message: '비밀번호를 입력하세요' }]}>
              <Input.Password
                prefix={<LockOutlined style={{ color: 'rgba(255,255,255,0.4)' }} />}
                placeholder="비밀번호"
                style={{
                  background: 'rgba(255,255,255,0.08)',
                  border: '1px solid rgba(255,255,255,0.15)',
                  borderRadius: 8,
                  color: '#fff',
                  height: 48,
                }}
              />
            </Form.Item>
            <Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                block
                loading={loading}
                style={{
                  height: 48,
                  background: 'linear-gradient(90deg, #006400, #00695c)',
                  border: 'none',
                  borderRadius: 8,
                  fontSize: 16,
                  fontWeight: 600,
                  boxShadow: '0 4px 16px rgba(0,100,0,0.3)',
                }}
              >
                로그인
              </Button>
            </Form.Item>
            <div style={{ textAlign: 'center', color: 'rgba(255,255,255,0.4)', fontSize: 12 }}>
              테스트 계정: admin / admin1234!
            </div>
          </Form>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;