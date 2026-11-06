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
} from '@ant-design/icons';
import { useCodeStore } from '@/stores/useCodeStore';
import { useBranchStore } from '@/stores/useBranchStore';
import { atmApi, type Atm, type AtmSearchParams } from '@/api/atmApi';

const ATM_STATUS_GROUP  = '004'; // ATM 상태
const ATM_CO_GROUP      = '050'; // 기기업체
const MANAGE_TYPE_GROUP = '006'; // 관리유형

const USE_YN_OPT = [
  { value: '', label: '전체' },
  { value: 'Y', label: '사용' },
  { value: 'N', label: '미사용' },
];

const fmtDate = (d?: string) => {
  if (!d || d.length < 8) return d ?? '-';
  return `${d.slice(0, 4)}-${d.slice(4, 6)}-${d.slice(6, 8)}`;
};

const EMPTY: Partial<Atm> = {
  termGroupId: '', termId: '', atmStatus: '', atmCoCd: '', modelCd: '',
  atmIp: '', branchCd: '', cornerCd: '', locationName: '',
};

const AtmTab: React.FC = () => {
  const [searchForm] = Form.useForm<AtmSearchParams>();
  const [editForm]   = Form.useForm<Atm>();

  const [rows, setRows]         = useState<Atm[]>([]);
  const [total, setTotal]       = useState(0);
  const [loading, setLoading]   = useState(false);
  const [page, setPage]         = useState(1);
  const [size, setSize]         = useState(20);
  const [detail, setDetail]     = useState<Atm | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [editMode, setEditMode] = useState<'create' | 'edit'>('create');
  const [saving, setSaving]     = useState(false);

  const { ensure, name, maps } = useCodeStore();
  const branchLabel = useBranchStore((s) => s.label);
  const loadBranches = useBranchStore((s) => s.load);

  useEffect(() => {
    ensure(ATM_STATUS_GROUP);
    ensure(ATM_CO_GROUP);
    ensure(MANAGE_TYPE_GROUP);
    loadBranches();
  }, [ensure, loadBranches]);

  const atmStatusOpts = [
    { value: '', label: '전체' },
    ...Object.entries(maps[ATM_STATUS_GROUP] ?? {}).map(([code, nm]) => ({ value: code, label: `${nm}(${code})` })),
  ];
  const atmCoOpts = [
    { value: '', label: '전체' },
    ...Object.entries(maps[ATM_CO_GROUP] ?? {}).map(([code, nm]) => ({ value: code, label: `${nm}(${code})` })),
  ];

  const fetchList = async (p = page, s = size) => {
    const v = searchForm.getFieldsValue();
    setLoading(true);
    try {
      const res = await atmApi.getList({
        termGroupId: v.termGroupId || undefined,
        termId:      v.termId      || undefined,
        atmStatus:   v.atmStatus   || undefined,
        atmCoCd:     v.atmCoCd     || undefined,
        modelCd:     v.modelCd     || undefined,
        page: p, size: s,
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

  useEffect(() => { fetchList(1, size); }, []); // eslint-disable-line

  const handleSearch = () => { setPage(1); fetchList(1, size); };
  const handleReset  = () => { searchForm.resetFields(); setPage(1); fetchList(1, size); };
  const handleTableChange = (pg: TablePaginationConfig) => {
    const np = pg.current ?? 1, ns = pg.pageSize ?? size;
    setPage(np); setSize(ns); fetchList(np, ns);
  };

  const openDetail = (row: Atm) => { setDetail(row); setDetailOpen(true); };
  const openCreate = () => { editForm.setFieldsValue(EMPTY); setEditMode('create'); setEditOpen(true); };
  const openEdit   = (row: Atm) => { editForm.setFieldsValue(row); setEditMode('edit'); setEditOpen(true); };

  const handleSave = async () => {
    try {
      const v = await editForm.validateFields();
      setSaving(true);
      if (editMode === 'create') {
        await atmApi.create(v as Atm);
        message.success('ATM이 등록되었습니다.');
      } else {
        await atmApi.update(v.termGroupId!, v.termId!, v as Atm);
        message.success('ATM 정보가 수정되었습니다.');
      }
      setEditOpen(false);
      fetchList(page, size);
    } catch (e: any) {
      if (e?.response) message.error(e.response.data?.message ?? '저장 실패');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (termGroupId: string, termId: string) => {
    try {
      await atmApi.delete(termGroupId, termId);
      message.success('ATM이 삭제되었습니다.');
      fetchList(page, size);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '삭제 실패');
    }
  };

  const columns: ColumnsType<Atm> = [
    { title: '영업점',   dataIndex: 'termGroupId', width: 150, render: (v) => branchLabel(v) },
    { title: '기기번호', dataIndex: 'termId',      width: 120, align: 'center' },
    { title: '상태',     dataIndex: 'atmStatus',   width: 90,  align: 'center', render: (v) => name(ATM_STATUS_GROUP, v) },
    { title: '제조사',   dataIndex: 'atmCoCd',     width: 100, align: 'center', render: (v) => name(ATM_CO_GROUP, v) },
    { title: '모델코드', dataIndex: 'modelCd',     width: 100, align: 'center' },
    { title: 'ATM IP',  dataIndex: 'atmIp',        width: 130, align: 'center' },
    { title: '설치위치', dataIndex: 'locationName', width: 160, ellipsis: true },
    { title: '설치일',   dataIndex: 'enterDate',   width: 100, align: 'center', render: fmtDate },
    { title: '앱버전',   dataIndex: 'appVersion',  width: 100, align: 'center' },
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
            title="ATM을 삭제하시겠습니까?"
            onConfirm={() => handleDelete(row.termGroupId, row.termId!)}
            okText="삭제" cancelText="취소"
          >
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <>
      <Form form={searchForm} layout="inline" style={{ marginBottom: 16 }} onFinish={handleSearch}>
        <Form.Item name="termGroupId" label="영업점코드">
          <Input placeholder="영업점코드" allowClear style={{ width: 120 }} />
        </Form.Item>
        <Form.Item name="termId" label="기기번호">
          <Input placeholder="기기번호" allowClear style={{ width: 120 }} />
        </Form.Item>
        <Form.Item name="atmStatus" label="상태">
          <Select style={{ width: 120 }} options={atmStatusOpts} defaultValue="" />
        </Form.Item>
        <Form.Item name="atmCoCd" label="제조사">
          <Select style={{ width: 120 }} options={atmCoOpts} defaultValue="" />
        </Form.Item>
        <Form.Item name="modelCd" label="모델코드">
          <Input placeholder="모델코드" allowClear style={{ width: 100 }} />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>조회</Button>
            <Button icon={<ReloadOutlined />} onClick={handleReset}>초기화</Button>
            <Button icon={<PlusOutlined />} onClick={openCreate}>등록</Button>
          </Space>
        </Form.Item>
      </Form>

      <Table<Atm>
        rowKey={(r) => `${r.termGroupId}-${r.termId}`}
        size="small" bordered loading={loading}
        columns={columns} dataSource={rows} scroll={{ x: 1200 }}
        pagination={{ current: page, pageSize: size, total, showSizeChanger: true, showTotal: (t) => `총 ${t}건` }}
        onChange={handleTableChange}
      />

      {/* 상세 모달 */}
      <Modal title="ATM 상세정보" open={detailOpen} onCancel={() => setDetailOpen(false)} footer={null} width={780} destroyOnClose>
        {detail && (
          <Descriptions bordered column={2} size="small">
            <Descriptions.Item label="영업점">{branchLabel(detail.termGroupId)}</Descriptions.Item>
            <Descriptions.Item label="기기번호">{detail.termId}</Descriptions.Item>
            <Descriptions.Item label="상태">{name(ATM_STATUS_GROUP, detail.atmStatus)}</Descriptions.Item>
            <Descriptions.Item label="제조사">{name(ATM_CO_GROUP, detail.atmCoCd)}</Descriptions.Item>
            <Descriptions.Item label="모델코드">{detail.modelCd ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="ATM IP">{detail.atmIp ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="설치위치" span={2}>{detail.locationName ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="설치일">{fmtDate(detail.enterDate)}</Descriptions.Item>
            <Descriptions.Item label="앱버전">{detail.appVersion ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="서비스시작일">{fmtDate(detail.serviceStartDate)}</Descriptions.Item>
            <Descriptions.Item label="서비스종료일">{fmtDate(detail.serviceEndDate)}</Descriptions.Item>
            <Descriptions.Item label="폐기일">{fmtDate(detail.disuseDate)}</Descriptions.Item>
            <Descriptions.Item label="원격여부">{detail.remoteFlag ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="자산번호">{detail.assetSno ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="기계번호">{detail.machineSno ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="전화번호">{detail.phoneNo ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="코너코드">{detail.cornerCd ?? '-'}</Descriptions.Item>
          </Descriptions>
        )}
      </Modal>

      {/* 등록/수정 모달 */}
      <Modal
        title={editMode === 'create' ? 'ATM 등록' : 'ATM 수정'}
        open={editOpen} onCancel={() => setEditOpen(false)}
        onOk={handleSave} okText={editMode === 'create' ? '등록' : '저장'}
        cancelText="취소" confirmLoading={saving} width={640} destroyOnClose
      >
        <Form form={editForm} layout="horizontal" labelCol={{ span: 7 }} wrapperCol={{ span: 15 }}>
          <Form.Item name="termGroupId" label="영업점코드" rules={[{ required: true, message: '필수 입력' }]}>
            <Input disabled={editMode === 'edit'} maxLength={5} />
          </Form.Item>
          <Form.Item name="termId" label="기기번호" rules={[{ required: true, message: '필수 입력' }]}>
            <Input disabled={editMode === 'edit'} maxLength={20} />
          </Form.Item>
          <Form.Item name="atmStatus" label="상태">
            <Select options={atmStatusOpts.filter((o) => o.value !== '')} allowClear placeholder="선택" />
          </Form.Item>
          <Form.Item name="atmCoCd" label="제조사">
            <Select options={atmCoOpts.filter((o) => o.value !== '')} allowClear placeholder="선택" />
          </Form.Item>
          <Form.Item name="modelCd" label="모델코드">
            <Input maxLength={20} />
          </Form.Item>
          <Form.Item name="atmIp" label="ATM IP">
            <Input maxLength={15} placeholder="xxx.xxx.xxx.xxx" />
          </Form.Item>
          <Form.Item name="locationName" label="설치위치">
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="branchCd" label="본지점코드">
            <Input maxLength={7} />
          </Form.Item>
          <Form.Item name="cornerCd" label="코너코드">
            <Input maxLength={10} />
          </Form.Item>
          <Form.Item name="phoneNo" label="전화번호">
            <Input maxLength={20} />
          </Form.Item>
          <Form.Item name="remoteFlag" label="원격여부">
            <Select options={USE_YN_OPT.filter((o) => o.value !== '')} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default AtmTab;
