import React, { useEffect, useState } from 'react';
import {
  Button,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Switch,
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
  UnlockOutlined,
  KeyOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import PageTemplate from '@/components/PageTemplate';
import { userApi, type User } from '@/api/userApi';
import { handleFieldCryptoError } from '@/utils/fieldCrypto';
import { userGroupApi, type UserGroup } from '@/api/userGroupApi';

const userTypeOptions = [
  { value: 'A', label: '관리자' },
  { value: 'U', label: '일반' },
  { value: 'M', label: '모니터링' },
];

const UserMgmtPage: React.FC = () => {
  const [searchForm] = Form.useForm();
  const [editForm] = Form.useForm<User>();
  const [pwForm] = Form.useForm<{ password: string; confirm: string }>();

  const [rows, setRows] = useState<User[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);

  const [groups, setGroups] = useState<UserGroup[]>([]);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<User | null>(null);

  const [pwModalOpen, setPwModalOpen] = useState(false);
  const [pwTarget, setPwTarget] = useState<string>('');

  const fetchGroups = async () => {
    try {
      const res = await userGroupApi.all();
      setGroups(res.data.data ?? []);
    } catch {
      // ignore
    }
  };

  const fetchList = async (p = page, s = size) => {
    const v = searchForm.getFieldsValue();
    setLoading(true);
    try {
      const res = await userApi.list({ ...v, page: p, size: s });
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
    fetchGroups();
    fetchList(1, size);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const openCreate = () => {
    setEditing(null);
    editForm.resetFields();
    editForm.setFieldsValue({
      userTypeCd: 'U',
      useYn: 'Y',
    });
    setModalOpen(true);
  };

  const openEdit = async (userId: string) => {
    try {
      const res = await userApi.get(userId);
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
      if (editing?.userId) {
        await userApi.update(editing.userId, values);
        message.success('수정되었습니다.');
      } else {
        await userApi.create(values);
        message.success('등록되었습니다.');
      }
      setModalOpen(false);
      fetchList(page, size);
    } catch (e: any) {
      if (e?.errorFields) return;
      handleFieldCryptoError(e); // 서버 키 교체 등 → 다음 시도에서 공개키를 다시 받음
      message.error(e?.response?.data?.message ?? '저장 실패');
    }
  };

  const handleDelete = async (userId: string) => {
    try {
      await userApi.remove(userId);
      message.success('삭제되었습니다.');
      fetchList(page, size);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '삭제 실패');
    }
  };

  const handleUnlock = async (userId: string) => {
    try {
      await userApi.unlock(userId);
      message.success('잠금이 해제되었습니다.');
      fetchList(page, size);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '잠금해제 실패');
    }
  };

  const handleToggleUseYn = async (userId: string, useYn: string) => {
    try {
      await userApi.toggleUseYn(userId, useYn);
      message.success('사용여부가 변경되었습니다.');
      fetchList(page, size);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '변경 실패');
    }
  };

  const openPw = (userId: string) => {
    setPwTarget(userId);
    pwForm.resetFields();
    setPwModalOpen(true);
  };

  const handlePwSubmit = async () => {
    const v = await pwForm.validateFields();
    if (v.password !== v.confirm) {
      message.error('비밀번호 확인이 일치하지 않습니다.');
      return;
    }
    try {
      await userApi.changePassword(pwTarget, v.password);
      message.success('비밀번호가 변경되었습니다.');
      setPwModalOpen(false);
    } catch (e: any) {
      handleFieldCryptoError(e); // 서버 키 교체 등 → 다음 시도에서 공개키를 다시 받음
      message.error(e?.response?.data?.message ?? '비밀번호 변경 실패');
    }
  };

  const columns: ColumnsType<User> = [
    { title: '사용자 ID', dataIndex: 'userId', width: 130, fixed: 'left' },
    { title: '사용자명', dataIndex: 'userName', width: 120 },
    {
      title: '그룹',
      dataIndex: 'userGroupNo',
      width: 140,
      render: (v) => groups.find((g) => g.userGroupNo === v)?.userGroupName ?? '-',
    },
    {
      title: '유형',
      dataIndex: 'userTypeCd',
      width: 80,
      align: 'center',
      render: (v) => userTypeOptions.find((o) => o.value === v)?.label ?? '-',
    },
    { title: '부서', dataIndex: 'deptName', width: 120, render: (v) => v || '-' },
    { title: '이메일', dataIndex: 'email', width: 200, render: (v) => v || '-' },
    {
      title: '잠금',
      dataIndex: 'lockYn',
      width: 70,
      align: 'center',
      render: (v) => (v === 'Y' ? <Tag color="red">잠김</Tag> : <Tag color="green">정상</Tag>),
    },
    {
      title: '사용',
      dataIndex: 'useYn',
      width: 80,
      align: 'center',
      render: (v, r) => (
        <Switch
          size="small"
          checked={v === 'Y'}
          onChange={(c) => handleToggleUseYn(r.userId, c ? 'Y' : 'N')}
        />
      ),
    },
    {
      title: '최근 로그인',
      dataIndex: 'lastLoginDt',
      width: 160,
      render: (v) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-'),
    },
    {
      title: '작업',
      width: 280,
      fixed: 'right',
      render: (_, r) => (
        <Space size="small">
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(r.userId)}>
            수정
          </Button>
          <Button size="small" icon={<KeyOutlined />} onClick={() => openPw(r.userId)}>
            비밀번호
          </Button>
          {r.lockYn === 'Y' && (
            <Button size="small" icon={<UnlockOutlined />} onClick={() => handleUnlock(r.userId)}>
              잠금해제
            </Button>
          )}
          <Popconfirm
            title="삭제하시겠습니까?"
            okText="삭제"
            cancelText="취소"
            onConfirm={() => handleDelete(r.userId)}
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
    <PageTemplate title="사용자관리">
      <div className="search-panel" style={{ marginBottom: 8 }}>
        <Form form={searchForm} layout="inline" onFinish={() => { setPage(1); fetchList(1, size); }} style={{ gap: 8, flexWrap: 'wrap' }}>
          <Form.Item name="userId" label="사용자 ID">
            <Input size="small" placeholder="ID 검색" style={{ width: 140 }} allowClear />
          </Form.Item>
          <Form.Item name="userName" label="사용자명">
            <Input size="small" placeholder="이름 검색" style={{ width: 140 }} allowClear />
          </Form.Item>
          <Form.Item name="userGroupNo" label="그룹">
            <Select
              size="small"
              placeholder="전체"
              style={{ width: 140 }}
              allowClear
              options={groups.map((g) => ({ value: g.userGroupNo, label: g.userGroupName }))}
            />
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

      <Table<User>
        columns={columns}
        dataSource={rows}
        loading={loading}
        size="small"
        bordered
        rowKey="userId"
        scroll={{ x: 1500 }}
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
        title={editing ? `사용자 수정 (${editing.userId})` : '사용자 등록'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        okText="저장"
        cancelText="취소"
        width={620}
        destroyOnHidden
      >
        <Form form={editForm} layout="vertical" size="small">
          <Space style={{ width: '100%' }} size="middle">
            <Form.Item
              name="userId"
              label="사용자 ID"
              style={{ flex: 1 }}
              rules={[{ required: true, message: '필수' }]}
            >
              <Input maxLength={20} disabled={!!editing} />
            </Form.Item>
            <Form.Item
              name="userName"
              label="사용자명"
              style={{ flex: 1 }}
              rules={[{ required: true, message: '필수' }]}
            >
              <Input maxLength={50} />
            </Form.Item>
          </Space>
          {!editing && (
            <Form.Item
              name="password"
              label="초기 비밀번호"
              rules={[{ required: true, message: '필수' }, { min: 8, message: '8자 이상' }]}
            >
              <Input.Password maxLength={50} />
            </Form.Item>
          )}
          <Space style={{ width: '100%' }} size="middle">
            <Form.Item name="userGroupNo" label="그룹" style={{ flex: 1 }}>
              <Select
                allowClear
                options={groups.map((g) => ({ value: g.userGroupNo, label: g.userGroupName }))}
              />
            </Form.Item>
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
          <Space style={{ width: '100%' }} size="middle">
            <Form.Item name="email" label="이메일" style={{ flex: 1 }}>
              <Input maxLength={100} />
            </Form.Item>
            <Form.Item name="cellPhoneNo" label="휴대폰" style={{ flex: 1 }}>
              <Input maxLength={20} />
            </Form.Item>
          </Space>
          <Space style={{ width: '100%' }} size="middle">
            <Form.Item name="deptName" label="부서명" style={{ flex: 1 }}>
              <Input maxLength={100} />
            </Form.Item>
            <Form.Item name="userPosition" label="직책" style={{ flex: 1 }}>
              <Input maxLength={50} />
            </Form.Item>
          </Space>
        </Form>
      </Modal>

      {/* 비밀번호 변경 모달 */}
      <Modal
        title={`비밀번호 변경 (${pwTarget})`}
        open={pwModalOpen}
        onCancel={() => setPwModalOpen(false)}
        onOk={handlePwSubmit}
        okText="변경"
        cancelText="취소"
        width={400}
        destroyOnHidden
      >
        <Form form={pwForm} layout="vertical" size="small">
          <Form.Item
            name="password"
            label="새 비밀번호"
            rules={[{ required: true, message: '필수' }, { min: 8, message: '8자 이상' }]}
          >
            <Input.Password maxLength={50} />
          </Form.Item>
          <Form.Item
            name="confirm"
            label="비밀번호 확인"
            rules={[{ required: true, message: '필수' }]}
          >
            <Input.Password maxLength={50} />
          </Form.Item>
        </Form>
      </Modal>
    </PageTemplate>
  );
};

export default UserMgmtPage;
