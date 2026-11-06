import React, { useEffect, useState } from 'react';
import {
  Button,
  Checkbox,
  Drawer,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  message,
} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  SearchOutlined,
  SafetyOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import PageTemplate from '@/components/PageTemplate';
import { userGroupApi, type UserGroup } from '@/api/userGroupApi';
import { roleApi, type Role } from '@/api/roleApi';

const userTypeOptions = [
  { value: 'A', label: '관리자' },
  { value: 'U', label: '일반' },
  { value: 'M', label: '모니터링' },
];

const GroupMgmtPage: React.FC = () => {
  const [searchForm] = Form.useForm();
  const [editForm] = Form.useForm<UserGroup>();

  const [rows, setRows] = useState<UserGroup[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<UserGroup | null>(null);

  // 권한매핑 Drawer
  const [authDrawerOpen, setAuthDrawerOpen] = useState(false);
  const [authTarget, setAuthTarget] = useState<UserGroup | null>(null);
  const [allRoles, setAllRoles] = useState<Role[]>([]);
  const [checkedAuthCodes, setCheckedAuthCodes] = useState<string[]>([]);

  const fetchList = async (p = page, s = size) => {
    const v = searchForm.getFieldsValue();
    setLoading(true);
    try {
      const res = await userGroupApi.list({ ...v, page: p, size: s });
      const data = res.data.data;
      setRows(data.content);
      setTotal(data.totalCount);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '조회 실패');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchList(1, size);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const openCreate = () => {
    setEditing(null);
    editForm.resetFields();
    editForm.setFieldsValue({ userTypeCd: 'U', useYn: 'Y' });
    setModalOpen(true);
  };

  const openEdit = async (no: number) => {
    try {
      const res = await userGroupApi.get(no);
      setEditing(res.data.data);
      editForm.setFieldsValue(res.data.data);
      setModalOpen(true);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '조회 실패');
    }
  };

  const handleSubmit = async () => {
    const values = await editForm.validateFields();
    try {
      if (editing?.userGroupNo) {
        await userGroupApi.update(editing.userGroupNo, values);
        message.success('수정되었습니다.');
      } else {
        await userGroupApi.create(values);
        message.success('등록되었습니다.');
      }
      setModalOpen(false);
      fetchList(page, size);
    } catch (e: any) {
      if (e?.errorFields) return;
      message.error(e?.response?.data?.message ?? '저장 실패');
    }
  };

  const handleDelete = async (no: number) => {
    try {
      await userGroupApi.remove(no);
      message.success('삭제되었습니다.');
      fetchList(page, size);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '삭제 실패');
    }
  };

  const openAuthDrawer = async (g: UserGroup) => {
    setAuthTarget(g);
    try {
      const [rolesRes, codesRes] = await Promise.all([
        roleApi.all(),
        userGroupApi.getAuthCodes(g.userGroupNo!),
      ]);
      setAllRoles(rolesRes.data.data ?? []);
      setCheckedAuthCodes(codesRes.data.data ?? []);
      setAuthDrawerOpen(true);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '권한 조회 실패');
    }
  };

  const handleAuthSave = async () => {
    if (!authTarget?.userGroupNo) return;
    try {
      await userGroupApi.updateAuthCodes(authTarget.userGroupNo, checkedAuthCodes);
      message.success('권한이 저장되었습니다.');
      setAuthDrawerOpen(false);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '권한 저장 실패');
    }
  };

  const columns: ColumnsType<UserGroup> = [
    { title: '번호', dataIndex: 'userGroupNo', width: 80, align: 'right' },
    { title: '그룹명', dataIndex: 'userGroupName', width: 200 },
    {
      title: '유형',
      dataIndex: 'userTypeCd',
      width: 90,
      align: 'center',
      render: (v) => userTypeOptions.find((o) => o.value === v)?.label ?? '-',
    },
    { title: '설명', dataIndex: 'description', ellipsis: true, render: (v) => v || '-' },
    {
      title: '사용',
      dataIndex: 'useYn',
      width: 70,
      align: 'center',
      render: (v) =>
        v === 'Y' ? <Tag color="green">사용</Tag> : <Tag color="default">미사용</Tag>,
    },
    {
      title: '등록일',
      dataIndex: 'regDt',
      width: 160,
      render: (v) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-'),
    },
    {
      title: '작업',
      width: 280,
      fixed: 'right',
      render: (_, r) => (
        <Space size="small">
          <Button size="small" icon={<SafetyOutlined />} onClick={() => openAuthDrawer(r)}>
            권한설정
          </Button>
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(r.userGroupNo!)}>
            수정
          </Button>
          <Popconfirm
            title="삭제하시겠습니까?"
            okText="삭제"
            cancelText="취소"
            onConfirm={() => handleDelete(r.userGroupNo!)}
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
    <PageTemplate title="그룹·권한관리">
      <div className="search-panel" style={{ marginBottom: 8 }}>
        <Form
          form={searchForm}
          layout="inline"
          onFinish={() => { setPage(1); fetchList(1, size); }}
          style={{ gap: 8, flexWrap: 'wrap' }}
        >
          <Form.Item name="userGroupName" label="그룹명">
            <Input size="small" placeholder="그룹명 검색" style={{ width: 160 }} allowClear />
          </Form.Item>
          <Form.Item name="userTypeCd" label="유형">
            <Select size="small" placeholder="전체" style={{ width: 110 }} allowClear options={userTypeOptions} />
          </Form.Item>
          <Form.Item name="useYn" label="사용여부">
            <Select
              size="small"
              placeholder="전체"
              style={{ width: 100 }}
              allowClear
              options={[
                { value: 'Y', label: '사용' },
                { value: 'N', label: '미사용' },
              ]}
            />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" size="small" htmlType="submit" icon={<SearchOutlined />} loading={loading}>
                조회
              </Button>
              <Button type="primary" size="small" icon={<PlusOutlined />} onClick={openCreate}>
                등록
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </div>

      <Table<UserGroup>
        columns={columns}
        dataSource={rows}
        loading={loading}
        size="small"
        bordered
        rowKey="userGroupNo"
        scroll={{ x: 1100 }}
        pagination={{
          current: page,
          pageSize: size,
          total,
          showSizeChanger: true,
          pageSizeOptions: ['10', '20', '50', '100'],
          showTotal: (t, r) => `${r[0]}-${r[1]} / 총 ${t}건`,
        }}
        onChange={(pg: TablePaginationConfig) => {
          const np = pg.current ?? 1;
          const ns = pg.pageSize ?? size;
          setPage(np);
          setSize(ns);
          fetchList(np, ns);
        }}
      />

      {/* 등록/수정 모달 */}
      <Modal
        title={editing ? `그룹 수정 (No.${editing.userGroupNo})` : '그룹 등록'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        okText="저장"
        cancelText="취소"
        width={520}
        destroyOnHidden
      >
        <Form form={editForm} layout="vertical" size="small">
          <Form.Item
            name="userGroupName"
            label="그룹명"
            rules={[{ required: true, message: '필수' }]}
          >
            <Input maxLength={50} />
          </Form.Item>
          <Space style={{ width: '100%' }} size="middle">
            <Form.Item name="userTypeCd" label="유형" style={{ flex: 1 }}>
              <Select options={userTypeOptions} />
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
            <Input.TextArea rows={3} maxLength={200} />
          </Form.Item>
        </Form>
      </Modal>

      {/* 권한 매핑 Drawer */}
      <Drawer
        title={`권한 설정 - ${authTarget?.userGroupName ?? ''}`}
        open={authDrawerOpen}
        onClose={() => setAuthDrawerOpen(false)}
        width={420}
        extra={
          <Space>
            <Button onClick={() => setAuthDrawerOpen(false)}>취소</Button>
            <Button type="primary" onClick={handleAuthSave}>
              저장
            </Button>
          </Space>
        }
      >
        <Checkbox.Group
          value={checkedAuthCodes}
          onChange={(v) => setCheckedAuthCodes(v as string[])}
          style={{ width: '100%' }}
        >
          <Space direction="vertical" style={{ width: '100%' }}>
            {allRoles.map((r) => (
              <div key={r.authCode} style={{ padding: '8px 0', borderBottom: '1px solid #f0f0f0' }}>
                <Checkbox value={r.authCode}>
                  <span style={{ fontWeight: 500 }}>{r.authName}</span>
                  <span style={{ color: '#999', marginLeft: 8, fontSize: 12 }}>
                    ({r.authCode})
                  </span>
                </Checkbox>
                {r.authDesc && (
                  <div style={{ marginLeft: 24, color: '#888', fontSize: 12 }}>
                    {r.authDesc}
                  </div>
                )}
              </div>
            ))}
          </Space>
        </Checkbox.Group>
      </Drawer>
    </PageTemplate>
  );
};

export default GroupMgmtPage;
