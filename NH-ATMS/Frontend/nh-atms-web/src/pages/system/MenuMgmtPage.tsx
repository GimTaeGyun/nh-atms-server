import React, { useEffect, useState } from 'react';
import {
  Button,
  Checkbox,
  Col,
  Empty,
  Form,
  Input,
  InputNumber,
  message,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Spin,
  Table,
  Typography,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined, KeyOutlined } from '@ant-design/icons';
import PageTemplate from '@/components/PageTemplate';
import { menuApi, type MenuEntity } from '@/api/menuApi';
import type { MenuTreeNode } from '@/types/menu';
import { useMenuStore } from '@/stores/useMenuStore';
import { roleApi, type Role, type MenuRolePermDetail, type MenuRolePermSaveRequest } from '@/api/roleApi';

/** 트리 → 부모 옵션 (depth=1만, 즉 PARENT_NO=null인 메뉴들) */
const buildParentOptions = (tree: MenuTreeNode[]) =>
  tree
    .filter((n) => !n.parentNo)
    .map((n) => ({ value: n.menuNo, label: `${n.menuName} (${n.menuNo})` }));

const MenuMgmtPage: React.FC = () => {
  const [tree, setTree] = useState<MenuTreeNode[]>([]);
  const [loading, setLoading] = useState(false);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<MenuEntity | null>(null);
  const [form] = Form.useForm<MenuEntity>();

  const [permModalOpen, setPermModalOpen] = useState(false);

  const refreshSidebar = useMenuStore((s) => s.fetchMenus);

  const fetchTree = async () => {
    setLoading(true);
    try {
      const res = await menuApi.getAllMenus();
      setTree(res.data.data ?? []);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '메뉴 조회 실패');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTree();
  }, []);

  const openCreate = (parentNo?: number) => {
    setEditing(null);
    form.resetFields();
    form.setFieldsValue({
      parentNo: parentNo ?? null,
      menuLevel: parentNo ? 2 : 1,
      menuOrder: 1,
      menuType: 'M',
      visibleYn: 'Y',
      useYn: 'Y',
    });
    setModalOpen(true);
  };

  const openEdit = async (menuNo: number) => {
    try {
      const res = await menuApi.getOne(menuNo);
      setEditing(res.data.data);
      form.setFieldsValue(res.data.data);
      setModalOpen(true);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '메뉴 조회 실패');
    }
  };

  const handleSubmit = async () => {
    const values = await form.validateFields();
    try {
      if (editing?.menuNo) {
        await menuApi.update(editing.menuNo, values);
        message.success('수정되었습니다.');
      } else {
        await menuApi.create(values);
        message.success('등록되었습니다.');
      }
      setModalOpen(false);
      await fetchTree();
      await refreshSidebar(); // 사이드바도 갱신
    } catch (e: any) {
      if (e?.errorFields) return;
      message.error(e?.response?.data?.message ?? '저장 실패');
    }
  };

  const handleDelete = async (menuNo: number) => {
    try {
      await menuApi.remove(menuNo);
      message.success('삭제되었습니다.');
      await fetchTree();
      await refreshSidebar();
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '삭제 실패');
    }
  };

  const columns: ColumnsType<MenuTreeNode> = [
    { title: '메뉴번호', dataIndex: 'menuNo', width: 100, align: 'right' },
    { title: '메뉴명', dataIndex: 'menuName', width: 220 },
    { title: 'URL', dataIndex: 'menuUrl', width: 200, render: (v) => v || '-' },
    { title: '아이콘', dataIndex: 'menuIcon', width: 160, render: (v) => v || '-' },
    {
      title: '레벨',
      dataIndex: 'menuLevel',
      width: 60,
      align: 'center',
    },
    {
      title: '순서',
      dataIndex: 'menuOrder',
      width: 60,
      align: 'center',
    },
    {
      title: '타입',
      dataIndex: 'menuType',
      width: 60,
      align: 'center',
      render: (v) => (v === 'D' ? '디렉토리' : '메뉴'),
    },
    {
      title: '작업',
      width: 220,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          {!record.parentNo && (
            <Button size="small" icon={<PlusOutlined />} onClick={() => openCreate(record.menuNo)}>
              하위추가
            </Button>
          )}
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(record.menuNo)}>
            수정
          </Button>
          <Popconfirm
            title="삭제하시겠습니까?"
            okText="삭제"
            cancelText="취소"
            onConfirm={() => handleDelete(record.menuNo)}
          >
            <Button size="small" danger icon={<DeleteOutlined />}>
              삭제
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <PageTemplate title="메뉴관리">
      <div style={{ marginBottom: 8, display: 'flex', justifyContent: 'space-between' }}>
        <Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openCreate()}>
            상위메뉴 등록
          </Button>
          <Button icon={<ReloadOutlined />} onClick={fetchTree}>
            새로고침
          </Button>
        </Space>
      </div>

      <Table<MenuTreeNode>
        columns={columns}
        dataSource={tree}
        loading={loading}
        size="small"
        bordered
        rowKey="menuNo"
        pagination={false}
        scroll={{ x: 1100 }}
        expandable={{ defaultExpandAllRows: true, childrenColumnName: 'children' }}
      />

      <Modal
        title={editing?.menuNo ? `메뉴 수정 (No.${editing.menuNo})` : '메뉴 등록'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        okText="저장"
        cancelText="취소"
        width={560}
        destroyOnHidden
      >
        <Form form={form} layout="vertical" size="small">
          <Form.Item name="parentNo" label="상위메뉴">
            <Select
              allowClear
              placeholder="없음(최상위)"
              options={buildParentOptions(tree)}
            />
          </Form.Item>
          <Form.Item
            name="menuName"
            label="메뉴명"
            rules={[{ required: true, message: '메뉴명을 입력하세요' }]}
          >
            <Input maxLength={50} />
          </Form.Item>
          <Form.Item name="menuUrl" label="URL (경로)">
            <Input placeholder="/system/menu" maxLength={200} />
          </Form.Item>
          <Form.Item name="menuIcon" label="아이콘 (Ant Design Icon명)">
            <Input placeholder="DashboardOutlined" maxLength={50} />
          </Form.Item>

          {/* 접근권한 버튼: 수정 모드(menuNo 존재)일 때만 활성화 */}
          <Form.Item>
            <Button
              icon={<KeyOutlined />}
              onClick={() => setPermModalOpen(true)}
              disabled={!editing?.menuNo}
              title={!editing?.menuNo ? '메뉴 등록 후 접근권한을 설정할 수 있습니다' : ''}
            >
              접근권한
            </Button>
          </Form.Item>

          <Space style={{ width: '100%' }} size="middle">
            <Form.Item name="menuLevel" label="레벨" style={{ flex: 1 }}>
              <InputNumber min={1} max={5} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="menuOrder" label="순서" style={{ flex: 1 }}>
              <InputNumber min={1} max={999} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="menuType" label="타입" style={{ flex: 1 }}>
              <Select
                options={[
                  { value: 'M', label: '메뉴' },
                  { value: 'D', label: '디렉토리' },
                ]}
              />
            </Form.Item>
          </Space>
          <Space style={{ width: '100%' }} size="middle">
            <Form.Item name="visibleYn" label="노출여부" style={{ flex: 1 }}>
              <Select
                options={[
                  { value: 'Y', label: '노출' },
                  { value: 'N', label: '숨김' },
                ]}
              />
            </Form.Item>
            <Form.Item name="useYn" label="사용여부" style={{ flex: 1 }}>
              <Select
                options={[
                  { value: 'Y', label: '사용' },
                  { value: 'N', label: '미사용' },
                ]}
              />
            </Form.Item>
          </Space>
          <Form.Item name="description" label="설명">
            <Input.TextArea rows={2} maxLength={200} />
          </Form.Item>
        </Form>
      </Modal>
      {/* ───────────────────────────────────────────────
          접근권한 팝업 (skeleton)
          TODO: 역할/그룹별 메뉴 접근 권한 조회·저장 API 연동
      ─────────────────────────────────────────────── */}
      <MenuPermModal
        open={permModalOpen}
        menuNo={editing?.menuNo ?? null}
        menuName={editing?.menuName ?? ''}
        onClose={() => setPermModalOpen(false)}
      />

    </PageTemplate>
  );
};

// ─── 접근권한 팝업 컴포넌트 ──────────────────────────────────────────────────

interface MenuPermModalProps {
  open: boolean;
  menuNo: number | null;
  menuName: string;
  onClose: () => void;
}

const ROLE_COLUMNS: ColumnsType<Role> = [
  { title: '역할코드', dataIndex: 'authCode', width: 130, align: 'center' },
  { title: '역할명',   dataIndex: 'authName' },
];

const PERM_OPTIONS = [
  { label: '조회', value: 'read'  },
  { label: '수정', value: 'write' },
];

/** API 응답(Y/N) → 체크박스 value 배열 변환 */
const toCheckedPerms = (detail: MenuRolePermDetail): string[] => {
  const result: string[] = [];
  if (detail.readYn  === 'Y') result.push('read');
  if (detail.writeYn === 'Y') result.push('write');
  return result;
};

/** 체크박스 value 배열 → 저장 요청 body(Y/N) 변환 */
const toPermRequest = (perms: string[]): MenuRolePermSaveRequest => ({
  readYn:  perms.includes('read')  ? 'Y' : 'N',
  writeYn: perms.includes('write') ? 'Y' : 'N',
});

const MenuPermModal: React.FC<MenuPermModalProps> = ({ open, menuNo, menuName, onClose }) => {
  const [roleList, setRoleList]         = useState<Role[]>([]);
  const [listLoading, setListLoading]   = useState(false);
  const [selectedRole, setSelectedRole] = useState<Role | null>(null);
  const [checkedPerms, setCheckedPerms] = useState<string[]>([]);
  const [permLoading, setPermLoading]   = useState(false);
  const [saveLoading, setSaveLoading]   = useState(false);

  // 팝업이 열릴 때 전체 역할 목록 조회
  useEffect(() => {
    if (!open) return;

    setListLoading(true);
    roleApi
      .all()
      .then((res) => setRoleList(res.data.data ?? []))
      .catch(() => message.error('역할 목록 조회 실패'))
      .finally(() => setListLoading(false));
  }, [open]);

  // 팝업이 닫힐 때 선택 상태 초기화
  useEffect(() => {
    if (!open) {
      setRoleList([]);
      setSelectedRole(null);
      setCheckedPerms([]);
    }
  }, [open]);

  // 저장 버튼 → 현재 선택된 역할의 체크박스 상태를 서버에 반영
  const handleSave = async () => {
    if (!menuNo || !selectedRole) {
      onClose();
      return;
    }

    setSaveLoading(true);
    try {
      await roleApi.saveMenuRolePerm(menuNo, selectedRole.authCode, toPermRequest(checkedPerms));
      message.success(`${selectedRole.authName} 권한이 저장되었습니다.`);
      onClose();
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '권한 저장 실패');
    } finally {
      setSaveLoading(false);
    }
  };

  // 좌측 역할 클릭 → 해당 메뉴+역할의 권한 조회
  const handleRoleClick = (record: Role) => {
    if (!menuNo) return;

    setSelectedRole(record);
    setCheckedPerms([]);
    setPermLoading(true);

    roleApi
      .getMenuRolePerm(menuNo, record.authCode)
      .then((res) => setCheckedPerms(toCheckedPerms(res.data.data)))
      .catch(() => message.error('권한 정보 조회 실패'))
      .finally(() => setPermLoading(false));
  };

  return (
    <Modal
      title={`접근권한 설정${menuName ? ` - ${menuName} (No.${menuNo})` : ''}`}
      open={open}
      onCancel={onClose}
      onOk={handleSave}
      okText="저장"
      cancelText="닫기"
      confirmLoading={saveLoading}
      width={760}
      destroyOnHidden
    >
      <Row style={{ minHeight: 320 }}>

        {/* ── 좌측: 역할 목록 ─────────────────────────────── */}
        <Col span={12} style={{ borderRight: '1px solid #f0f0f0', paddingRight: 12 }}>
          <Typography.Text strong style={{ display: 'block', marginBottom: 8 }}>
            역할 목록
          </Typography.Text>
          <Table<Role>
            size="small"
            bordered
            rowKey="authCode"
            columns={ROLE_COLUMNS}
            dataSource={roleList}
            loading={listLoading}
            pagination={false}
            scroll={{ y: 260 }}
            locale={{ emptyText: '등록된 역할이 없습니다.' }}
            onRow={(record) => ({
              onClick: () => handleRoleClick(record),
              style: {
                cursor: 'pointer',
                background: selectedRole?.authCode === record.authCode ? '#e6f4ff' : undefined,
              },
            })}
          />
        </Col>

        {/* ── 우측: 권한 체크박스 ─────────────────────────── */}
        <Col span={12} style={{ paddingLeft: 16 }}>
          <Typography.Text strong style={{ display: 'block', marginBottom: 8 }}>
            권한 설정
          </Typography.Text>

          {selectedRole ? (
            <Spin spinning={permLoading}>
              <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 20 }}>
                {selectedRole.authName}&nbsp;({selectedRole.authCode})
              </Typography.Text>
              <Checkbox.Group
                value={checkedPerms}
                onChange={(vals) => setCheckedPerms(vals as string[])}
              >
                <Row gutter={[0, 20]}>
                  {PERM_OPTIONS.map((opt) => (
                    <Col span={12} key={opt.value}>
                      <Checkbox value={opt.value}>{opt.label}</Checkbox>
                    </Col>
                  ))}
                </Row>
              </Checkbox.Group>
            </Spin>
          ) : (
            <Empty
              style={{ marginTop: 60 }}
              description="좌측 목록에서 역할을 선택하세요"
            />
          )}
        </Col>

      </Row>
    </Modal>
  );
};

export default MenuMgmtPage;
