import React, { useEffect, useMemo, useState } from 'react';
import { Input, Layout, Menu, Spin, Tooltip } from 'antd';
import * as AntIcons from '@ant-design/icons';
import { StarFilled, StarOutlined, SearchOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useTabStore } from '@/stores/useTabStore';
import { useMenuStore } from '@/stores/useMenuStore';
import { useFavoriteStore } from '@/stores/useFavoriteStore';
import type { MenuTreeNode } from '@/types/menu';

const { Sider } = Layout;

/** 아이콘 문자열 → React 노드 */
const getIcon = (name?: string): React.ReactNode => {
  if (!name) return null;
  const Icon = (AntIcons as unknown as Record<string, React.ComponentType>)[name];
  return Icon ? <Icon /> : null;
};

/** menuNo 로 노드 탐색 */
const findNode = (nodes: MenuTreeNode[], key: string): MenuTreeNode | null => {
  for (const n of nodes) {
    if (String(n.menuNo) === key) return n;
    if (n.children) {
      const found = findNode(n.children, key);
      if (found) return found;
    }
  }
  return null;
};

/** URL 을 가진 모든 리프 메뉴를 평탄화 */
const flattenLeaves = (nodes: MenuTreeNode[], acc: MenuTreeNode[] = []): MenuTreeNode[] => {
  for (const n of nodes) {
    if (n.children && n.children.length > 0) flattenLeaves(n.children, acc);
    else if (n.menuUrl) acc.push(n);
  }
  return acc;
};

/** 검색어로 트리 필터링 (매칭 리프 + 매칭 리프를 가진 부모 유지) */
const filterTree = (nodes: MenuTreeNode[], kw: string): MenuTreeNode[] => {
  const lower = kw.trim().toLowerCase();
  if (!lower) return nodes;
  const walk = (list: MenuTreeNode[]): MenuTreeNode[] => {
    const result: MenuTreeNode[] = [];
    for (const n of list) {
      if (n.children && n.children.length > 0) {
        const kids = walk(n.children);
        if (kids.length > 0 || n.menuName.toLowerCase().includes(lower)) {
          result.push({ ...n, children: kids.length > 0 ? kids : n.children });
        }
      } else if (n.menuName.toLowerCase().includes(lower)) {
        result.push(n);
      }
    }
    return result;
  };
  return walk(nodes);
};

interface AppSidebarProps {
  collapsed: boolean;
}

const AppSidebar: React.FC<AppSidebarProps> = ({ collapsed }) => {
  const navigate = useNavigate();
  const addTab = useTabStore((s) => s.addTab);
  const { menus, loaded, loading, fetchMenus } = useMenuStore();
  const favorites = useFavoriteStore((s) => s.favorites);
  const toggleFavorite = useFavoriteStore((s) => s.toggle);
  const [keyword, setKeyword] = useState('');

  useEffect(() => {
    if (!loaded) fetchMenus();
  }, [loaded, fetchMenus]);

  const leaves = useMemo(() => flattenLeaves(menus), [menus]);
  const favoriteNodes = useMemo(
    () =>
      favorites
        .map((no) => leaves.find((l) => l.menuNo === no))
        .filter(Boolean) as MenuTreeNode[],
    [favorites, leaves],
  );
  const filteredMenus = useMemo(() => filterTree(menus, keyword), [menus, keyword]);

  const openMenu = (node: MenuTreeNode) => {
    if (!node.menuUrl) return;
    addTab({
      key: String(node.menuNo),
      label: node.menuName,
      path: node.menuUrl,
      closable: node.menuUrl !== '/dashboard',
    });
    navigate(node.menuUrl);
  };

  /** 리프 메뉴 라벨: 메뉴명 + 즐겨찾기 토글 별 */
  const leafLabel = (node: MenuTreeNode): React.ReactNode => {
    const fav = favorites.includes(node.menuNo);
    const onStar = (e: React.MouseEvent) => {
      e.stopPropagation();
      toggleFavorite(node.menuNo);
    };
    return (
      <span style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <span style={{ overflow: 'hidden', textOverflow: 'ellipsis' }}>{node.menuName}</span>
        <Tooltip title={fav ? '즐겨찾기 해제' : '즐겨찾기 추가'}>
          {fav ? (
            <StarFilled style={{ color: '#fadb14', marginLeft: 8 }} onClick={onStar} />
          ) : (
            <StarOutlined
              style={{ color: 'rgba(255,255,255,0.45)', marginLeft: 8 }}
              onClick={onStar}
            />
          )}
        </Tooltip>
      </span>
    );
  };

  /** MenuTreeNode 트리 → Ant Design Menu ItemType 변환 */
  const toAntItems = (nodes: MenuTreeNode[]): any[] =>
    nodes.map((node) => {
      const hasChildren = node.children && node.children.length > 0;
      return {
        key: String(node.menuNo),
        icon: getIcon(node.menuIcon),
        label: hasChildren ? node.menuName : leafLabel(node),
        children: hasChildren ? toAntItems(node.children!) : undefined,
      };
    });

  const favItems: any[] =
    favoriteNodes.length > 0 && !keyword.trim()
      ? [
          {
            key: 'fav-group',
            icon: <StarFilled style={{ color: '#fadb14' }} />,
            label: '즐겨찾기',
            children: favoriteNodes.map((n) => ({
              key: `fav-${n.menuNo}`,
              label: leafLabel(n),
            })),
          },
        ]
      : [];

  const items = [...favItems, ...toAntItems(filteredMenus)];

  const handleClick = ({ key }: { key: string }) => {
    if (key === 'fav-group') return;
    const menuNo = key.startsWith('fav-') ? key.slice(4) : key;
    const node = findNode(menus, menuNo);
    if (node) openMenu(node);
  };

  // 검색 중에는 매칭된 부모를 펼침
  const openKeys = keyword.trim()
    ? filteredMenus
        .filter((n) => n.children && n.children.length > 0)
        .map((n) => String(n.menuNo))
    : undefined;

  return (
    <Sider
      collapsible
      collapsed={collapsed}
      trigger={null}
      width={200}
      collapsedWidth={0}
      style={{
        background: '#0e1d36',
        borderRight: '1px solid rgba(255,255,255,0.06)',
        overflow: 'auto',
        height: 'calc(100vh - 50px - 40px)',
      }}
    >
      {loading ? (
        <div style={{ textAlign: 'center', paddingTop: 40 }}>
          <Spin size="small" />
        </div>
      ) : (
        <>
          <div style={{ padding: '8px 8px 4px' }}>
            <Input
              allowClear
              size="small"
              prefix={<SearchOutlined style={{ color: 'rgba(255,255,255,0.45)' }} />}
              placeholder="메뉴 검색"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
            />
          </div>
          <Menu
            mode="inline"
            className="sidebar-menu"
            theme="dark"
            items={items}
            onClick={handleClick}
            selectable={false}
            {...(openKeys ? { openKeys } : {})}
            style={{
              background: 'transparent',
              borderRight: 'none',
              fontSize: 13,
            }}
          />
        </>
      )}
    </Sider>
  );
};

export default AppSidebar;
