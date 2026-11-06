import React, { useEffect, useState } from 'react';
import {
  Button,
  Descriptions,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  message,
} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  InfoCircleOutlined,
  CheckOutlined,
} from '@ant-design/icons';
import PageTemplate from '@/components/PageTemplate';
import { useCodeStore } from '@/stores/useCodeStore';
import {
  serviceProviderApi,
  type ServiceProvider,
  type ServiceProviderSearchParams,
} from '@/api/serviceProviderApi';

/** 업체종류 그룹코드 */
const PROVIDER_TYPE_GROUP = '003';

const SEND_TYPE_OPTIONS = [
  { value: '1', label: '즉시' },
  { value: '2', label: '지연' },
];

const FLAG_OPTIONS = [
  { value: '1', label: '예' },
  { value: '0', label: '아니오' },
];

const USE_YN_OPTIONS = [
  { value: '', label: '전체' },
  { value: '1', label: '사용' },
  { value: '0', label: '미사용' },
];

const EMPTY_FORM: Partial<ServiceProvider> = {
  providerCd: '',
  knbankFlag: '0',
  providerName: '',
  providerTypeCd: '',
  person: '',
  phoneNo: '',
  faxNo: '',
  email: '',
  zipCd: '',
  address: '',
  sendType: '1',
  faultSendType: '1',
  useYn: '1',
  wooriFlag: '0',
};

const ServiceProviderPage: React.FC = () => {
  const [searchForm] = Form.useForm<ServiceProviderSearchParams>();
  const [editForm] = Form.useForm<ServiceProvider>();

  const [rows, setRows] = useState<ServiceProvider[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);

  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<ServiceProvider | null>(null);

  const [editOpen, setEditOpen] = useState(false);
  const [editMode, setEditMode] = useState<'create' | 'edit'>('create');
  const [saving, setSaving] = useState(false);

  /* 코드 스토어 — 003(업체종류) 로드 */
  const { ensure, name, maps } = useCodeStore();

  useEffect(() => {
    ensure(PROVIDER_TYPE_GROUP);
  }, [ensure]);

  /** 003 그룹 코드 목록 → Select options */
  const providerTypeOptions = [
    { value: '', label: '전체' },
    ...Object.entries(maps[PROVIDER_TYPE_GROUP] ?? {}).map(([code, nm]) => ({
      value: code,
      label: `${nm}(${code})`,
    })),
  ];

  /** 등록/수정 폼용 options (전체 제외) */
  const providerTypeEditOptions = providerTypeOptions.filter((o) => o.value !== '');

  const fetchList = async (p = page, s = size) => {
    const v = searchForm.getFieldsValue();
    setLoading(true);
    try {
      const res = await serviceProviderApi.getList({
        providerCd:     v.providerCd     || undefined,
        providerName:   v.providerName   || undefined,
        providerTypeCd: v.providerTypeCd || undefined,
        useYn:          v.useYn          || undefined,
        page: p,
        size: s,
      });
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

  const handleSearch = () => { setPage(1); fetchList(1, size); };

  const handleReset = () => {
    searchForm.resetFields();
    setPage(1);
    fetchList(1, size);
  };

  const handleTableChange = (pg: TablePaginationConfig) => {
    const newPage = pg.current ?? 1;
    const newSize = pg.pageSize ?? size;
    setPage(newPage);
    setSize(newSize);
    fetchList(newPage, newSize);
  };

  const openDetail = (row: ServiceProvider) => { setDetail(row); setDetailOpen(true); };

  const openCreate = () => {
    editForm.setFieldsValue(EMPTY_FORM);
    setEditMode('create');
    setEditOpen(true);
  };

  const openEdit = (row: ServiceProvider) => {
    editForm.setFieldsValue(row);
    setEditMode('edit');
    setEditOpen(true);
  };

  const handleSave = async () => {
    try {
      const values = await editForm.validateFields();
      setSaving(true);
      if (editMode === 'create') {
        await serviceProviderApi.create(values as ServiceProvider);
        message.success('업체가 등록되었습니다.');
      } else {
        await serviceProviderApi.update(values.providerCd!, values as ServiceProvider);
        message.success('업체 정보가 수정되었습니다.');
      }
      setEditOpen(false);
      fetchList(page, size);
    } catch (e: any) {
      if (e?.response) message.error(e.response.data?.message ?? '저장 실패');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (providerCd: string) => {
    try {
      await serviceProviderApi.delete(providerCd);
      message.success('업체가 삭제되었습니다.');
      fetchList(page, size);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '삭제 실패');
    }
  };

  const flagCheck = (v?: string) =>
    v === '1' ? <CheckOutlined style={{ color: '#1890ff' }} /> : null;
  const flagLabel = (v?: string) => (v === '1' ? '예' : v === '0' ? '아니오' : '-');
  const useYnLabel = (v?: string) => (v === '1' ? '사용' : v === '0' ? '미사용' : '-');

  const columns: ColumnsType<ServiceProvider> = [
    { title: '업체코드', dataIndex: 'providerCd',    width: 100, align: 'center' },
    { title: '업체명',   dataIndex: 'providerName',  width: 180, ellipsis: true },
    {
      title: '업체유형',
      dataIndex: 'providerTypeCd',
      width: 120,
      align: 'center',
      render: (v) => name(PROVIDER_TYPE_GROUP, v),
    },
    { title: '담당자',   dataIndex: 'person',         width: 100, align: 'center' },
    { title: '전화번호', dataIndex: 'phoneNo',         width: 130, align: 'center' },
    { title: '이메일',   dataIndex: 'email',           width: 180, ellipsis: true },
{ title: 'KN뱅크',  dataIndex: 'knbankFlag', width: 80, align: 'center', render: flagCheck },
    { title: '우리은행', dataIndex: 'wooriFlag',  width: 80, align: 'center', render: flagCheck },
    { title: '사용여부', dataIndex: 'useYn',      width: 80, align: 'center', render: flagCheck },
    {
      title: '관리',
      width: 110,
      align: 'center',
      fixed: 'right',
      render: (_, row) => (
        <Space size={4}>
          <Button size="small" icon={<InfoCircleOutlined />} onClick={() => openDetail(row)} />
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(row)} />
          <Popconfirm
            title="업체를 삭제하시겠습니까?"
            onConfirm={() => handleDelete(row.providerCd)}
            okText="삭제"
            cancelText="취소"
          >
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <PageTemplate title="업체관리">
      {/* 검색 폼 */}
      <Form form={searchForm} layout="inline" style={{ marginBottom: 16 }} onFinish={handleSearch}>
        <Form.Item name="providerCd" label="업체코드">
          <Input placeholder="업체코드" allowClear style={{ width: 120 }} />
        </Form.Item>
        <Form.Item name="providerName" label="업체명">
          <Input placeholder="업체명 검색" allowClear style={{ width: 160 }} />
        </Form.Item>
        <Form.Item name="providerTypeCd" label="업체유형">
          <Select style={{ width: 130 }} options={providerTypeOptions} defaultValue="" />
        </Form.Item>
        <Form.Item name="useYn" label="사용여부">
          <Select style={{ width: 90 }} options={USE_YN_OPTIONS} defaultValue="" />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>조회</Button>
            <Button icon={<ReloadOutlined />} onClick={handleReset}>초기화</Button>
            <Button icon={<PlusOutlined />} onClick={openCreate}>등록</Button>
          </Space>
        </Form.Item>
      </Form>

      {/* 목록 테이블 */}
      <Table<ServiceProvider>
        rowKey="providerCd"
        size="small"
        bordered
        loading={loading}
        columns={columns}
        dataSource={rows}
        scroll={{ x: 1400 }}
        pagination={{
          current: page,
          pageSize: size,
          total,
          showSizeChanger: true,
          showTotal: (t) => `총 ${t}건`,
        }}
        onChange={handleTableChange}
      />

      {/* 상세 모달 */}
      <Modal
        title="업체 상세정보"
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        footer={null}
        width={720}
        destroyOnClose
      >
        {detail && (
          <Descriptions bordered column={2} size="small">
            <Descriptions.Item label="업체코드">{detail.providerCd}</Descriptions.Item>
            <Descriptions.Item label="업체명">{detail.providerName}</Descriptions.Item>
            <Descriptions.Item label="업체유형">
              {name(PROVIDER_TYPE_GROUP, detail.providerTypeCd)}
            </Descriptions.Item>
            <Descriptions.Item label="담당자">{detail.person ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="전화번호">{detail.phoneNo ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="팩스번호">{detail.faxNo ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="이메일" span={2}>{detail.email ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="우편번호">{detail.zipCd ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="주소">{detail.address ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="송신유형">{detail.sendType ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="장애송신유형">{detail.faultSendType ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="KN뱅크 여부">{flagLabel(detail.knbankFlag)}</Descriptions.Item>
            <Descriptions.Item label="우리은행 여부">{flagLabel(detail.wooriFlag)}</Descriptions.Item>
            <Descriptions.Item label="사용여부">{useYnLabel(detail.useYn)}</Descriptions.Item>
          </Descriptions>
        )}
      </Modal>

      {/* 등록/수정 모달 */}
      <Modal
        title={editMode === 'create' ? '업체 등록' : '업체 수정'}
        open={editOpen}
        onCancel={() => setEditOpen(false)}
        onOk={handleSave}
        okText={editMode === 'create' ? '등록' : '저장'}
        cancelText="취소"
        confirmLoading={saving}
        width={640}
        destroyOnClose
      >
        <Form form={editForm} layout="horizontal" labelCol={{ span: 7 }} wrapperCol={{ span: 15 }}>
          <Form.Item name="providerCd" label="업체코드" rules={[{ required: true, message: '업체코드를 입력하세요.' }]}>
            <Input disabled={editMode === 'edit'} maxLength={20} />
          </Form.Item>
          <Form.Item name="providerName" label="업체명" rules={[{ required: true, message: '업체명을 입력하세요.' }]}>
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="providerTypeCd" label="업체유형">
            <Select options={providerTypeEditOptions} placeholder="선택" allowClear />
          </Form.Item>
          <Form.Item name="person" label="담당자">
            <Input maxLength={50} />
          </Form.Item>
          <Form.Item name="phoneNo" label="전화번호">
            <Input maxLength={20} placeholder="02-0000-0000" />
          </Form.Item>
          <Form.Item name="faxNo" label="팩스번호">
            <Input maxLength={20} />
          </Form.Item>
          <Form.Item name="email" label="이메일">
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="zipCd" label="우편번호">
            <Input maxLength={10} />
          </Form.Item>
          <Form.Item name="address" label="주소">
            <Input maxLength={200} />
          </Form.Item>
          <Form.Item name="sendType" label="송신유형">
            <Select options={SEND_TYPE_OPTIONS} />
          </Form.Item>
          <Form.Item name="faultSendType" label="장애송신유형">
            <Select options={SEND_TYPE_OPTIONS} />
          </Form.Item>
          <Form.Item name="knbankFlag" label="KN뱅크 여부">
            <Select options={FLAG_OPTIONS} />
          </Form.Item>
          <Form.Item name="wooriFlag" label="우리은행 여부">
            <Select options={FLAG_OPTIONS} />
          </Form.Item>
          <Form.Item name="useYn" label="사용여부">
            <Select options={[{ value: '1', label: '사용' }, { value: '0', label: '미사용' }]} />
          </Form.Item>
        </Form>
      </Modal>
    </PageTemplate>
  );
};

export default ServiceProviderPage;
