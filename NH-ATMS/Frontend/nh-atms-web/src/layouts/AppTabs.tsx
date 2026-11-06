import React from 'react';
import { Tabs, Button, Tooltip } from 'antd';
import { CloseOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useTabStore } from '@/stores/useTabStore';

const AppTabs: React.FC = () => {
  const navigate = useNavigate();
  const { tabs, activeKey, setActiveKey, removeTab, closeAll } = useTabStore();

  const handleChange = (key: string) => {
    setActiveKey(key);
    const tab = tabs.find((t) => t.key === key);
    if (tab) navigate(tab.path);
  };

  const handleEdit = (
    targetKey: React.MouseEvent | React.KeyboardEvent | string,
    action: 'add' | 'remove',
  ) => {
    if (action === 'remove' && typeof targetKey === 'string') {
      removeTab(targetKey);
      const remaining = tabs.filter((t) => t.key !== targetKey);
      if (remaining.length > 0) {
        navigate(remaining[remaining.length - 1].path);
      }
    }
  };

  const handleCloseAll = () => {
    closeAll();
    const kept = tabs.filter((t) => t.closable === false);
    if (kept.length > 0) {
      navigate(kept[kept.length - 1].path);
    } else {
      navigate('/dashboard');
    }
  };

  if (tabs.length === 0) return null;

  return (
    <div
      className="menu-tabs"
      style={{
        background: '#132744',
        paddingLeft: 8,
        borderBottom: '1px solid rgba(255,255,255,0.08)',
        height: 40,
        display: 'flex',
        alignItems: 'flex-end',
      }}
    >
      <Tabs
        type="editable-card"
        hideAdd
        size="small"
        activeKey={activeKey}
        onChange={handleChange}
        onEdit={handleEdit}
        items={tabs.map((tab) => ({
          key: tab.key,
          label: tab.label,
          closable: tab.closable,
        }))}
        style={{ marginBottom: 0, flex: 1, minWidth: 0 }}
      />
      <Tooltip title="모든 탭 닫기">
        <Button
          type="text"
          size="small"
          icon={<CloseOutlined />}
          onClick={handleCloseAll}
          style={{ color: 'rgba(255,255,255,0.65)', margin: '0 8px 4px', flexShrink: 0 }}
        >
          전체 닫기
        </Button>
      </Tooltip>
    </div>
  );
};

export default AppTabs;