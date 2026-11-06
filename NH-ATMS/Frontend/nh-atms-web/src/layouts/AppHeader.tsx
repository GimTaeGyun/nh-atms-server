import React from 'react';
import { Layout, Button, Space, Typography } from 'antd';
import { MenuFoldOutlined, MenuUnfoldOutlined, LogoutOutlined } from '@ant-design/icons';
import { useAuthStore } from '@/stores/useAuthStore';

const { Header } = Layout;

interface AppHeaderProps {
  collapsed: boolean;
  onToggle: () => void;
}

const AppHeader: React.FC<AppHeaderProps> = ({ collapsed, onToggle }) => {
  const { user, logout } = useAuthStore();

  return (
    <Header
      style={{
        background: 'linear-gradient(90deg, #0a1628 0%, #132744 60%, #1a3a5c 100%)',
        padding: '0 20px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        height: 50,
        lineHeight: '50px',
        borderBottom: '1px solid rgba(255,255,255,0.08)',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <Button
          type="text"
          icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          onClick={onToggle}
          style={{ color: '#fff', fontSize: 16 }}
        />
        <img
          src="/nh_logo.png"
          alt="NH농협은행"
          style={{
            height: 28,
            padding: '1px 6px',
            borderRadius: 4,
            objectFit: 'contain',
            marginTop: '3px'
          }}
          onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
        />
        <span
          style={{
            color: '#fff',
            fontSize: 20,
            fontWeight: 500,
            letterSpacing: 0,
          }}
        >
          ATMS
        </span>
      </div>

      <Space size={12}>
        <Typography.Text style={{ color: 'rgba(255,255,255,0.7)', fontSize: 12 }}>
          로그인: {user?.userName} ({user?.deptName})
        </Typography.Text>
        <Button
          type="text"
          icon={<LogoutOutlined />}
          onClick={logout}
          style={{ color: 'rgba(255,255,255,0.7)', fontSize: 12 }}
        >
          로그아웃
        </Button>
      </Space>
    </Header>
  );
};

export default AppHeader;