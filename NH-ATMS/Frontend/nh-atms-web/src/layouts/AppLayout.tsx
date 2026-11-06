import React, { useState } from 'react';
import { Layout } from 'antd';
import AppHeader from './AppHeader';
import AppSidebar from './AppSidebar';
import AppTabs from './AppTabs';

const { Content } = Layout;

const AppLayout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [collapsed, setCollapsed] = useState(false);

  return (
    <Layout style={{ height: '100vh', overflow: 'hidden' }}>
      {/* 상단 헤더 */}
      <AppHeader collapsed={collapsed} onToggle={() => setCollapsed(!collapsed)} />

      {/* 메뉴 히스토리 탭 */}
      <AppTabs />

      <Layout style={{ flex: 1, overflow: 'hidden' }}>
        {/* 좌측 사이드바 */}
        <AppSidebar collapsed={collapsed} />

        {/* 컨텐츠 영역 */}
        <Content style={{ padding: '12px 16px', background: '#f4f6f9', overflow: 'auto' }}>
          {children}
        </Content>
      </Layout>
    </Layout>
  );
};

export default AppLayout;