import React, { useEffect, useState } from 'react';
import {
  Button,
  Descriptions,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  message,
} from 'antd';
import { CheckOutlined } from '@ant-design/icons';
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
import { atmModelApi, type AtmModel, type AtmModelSearchParams } from '@/api/atmModelApi';

const ATM_CO_GROUP   = '050'; // 기기업체
const ATM_TYPE_GROUP = '032'; // 기종구분
const OS_CD_GROUP    = '033'; // OS

const USE_YN_OPT = [
  { value: '', label: '전체' },
  { value: '1', label: '사용' },
  { value: '0', label: '미사용' },
];

const YN_OPT = [
  { value: '1', label: '예' },
  { value: '0', label: '아니오' },
];

/** 1 이면 체크아이콘, 아니면 빈칸 */
const supportCheck = (v?: string) =>
  v === '1' ? <CheckOutlined style={{ color: '#1890ff' }} /> : null;

const supportTag = supportCheck;

const EMPTY: Partial<AtmModel> = {
  modelCd: '', modelName: '', atmCoCd: '', atmTypeCd: '', useYn: '1', remoteYn: '0',
  dataSupported: '0', sprImgSupported: '0', faceImgSupported: '0',
  handImgSupported: '0', mcrImgSupported: '0',
  chkFrontImgSupported: '0', chkBackImgSupported: '0',
  dotSupported: '0', earphoneSupported: '0', zoomSupported: '0', wheelchairSupported: '0',
};

const AtmModelTab: React.FC = () => {
  const [searchForm] = Form.useForm<AtmModelSearchParams>();
  const [editForm]   = Form.useForm<AtmModel>();

  const [rows, setRows]         = useState<AtmModel[]>([]);
  const [total, setTotal]       = useState(0);
  const [loading, setLoading]   = useState(false);
  const [page, setPage]         = useState(1);
  const [size, setSize]         = useState(20);
  const [detail, setDetail]     = useState<AtmModel | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [editMode, setEditMode] = useState<'create' | 'edit'>('create');
  const [saving, setSaving]     = useState(false);

  const { ensure, name, maps } = useCodeStore();

  useEffect(() => {
    ensure(ATM_CO_GROUP);
    ensure(ATM_TYPE_GROUP);
    ensure(OS_CD_GROUP);
  }, [ensure]);

  const atmCoOpts = [
    { value: '', label: '전체' },
    ...Object.entries(maps[ATM_CO_GROUP] ?? {}).map(([code, nm]) => ({ value: code, label: `${nm}(${code})` })),
  ];
  const atmTypeOpts = [
    { value: '', label: '전체' },
    ...Object.entries(maps[ATM_TYPE_GROUP] ?? {}).map(([code, nm]) => ({ value: code, label: `${nm}(${code})` })),
  ];

  const fetchList = async (p = page, s = size) => {
    const v = searchForm.getFieldsValue();
    setLoading(true);
    try {
      const res = await atmModelApi.getList({
        modelCd:    v.modelCd    || undefined,
        modelName:  v.modelName  || undefined,
        atmCoCd:    v.atmCoCd    || undefined,
        atmTypeCd:  v.atmTypeCd  || undefined,
        useYn:      v.useYn      || undefined,
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

  const openDetail = (row: AtmModel) => { setDetail(row); setDetailOpen(true); };
  const openCreate = () => { editForm.setFieldsValue(EMPTY); setEditMode('create'); setEditOpen(true); };
  const openEdit   = (row: AtmModel) => { editForm.setFieldsValue(row); setEditMode('edit'); setEditOpen(true); };

  const handleSave = async () => {
    try {
      const v = await editForm.validateFields();
      setSaving(true);
      if (editMode === 'create') {
        await atmModelApi.create(v as AtmModel);
        message.success('기기모델이 등록되었습니다.');
      } else {
        await atmModelApi.update(v.modelCd!, v as AtmModel);
        message.success('기기모델이 수정되었습니다.');
      }
      setEditOpen(false);
      fetchList(page, size);
    } catch (e: any) {
      if (e?.response) message.error(e.response.data?.message ?? '저장 실패');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (modelCd: string) => {
    try {
      await atmModelApi.delete(modelCd);
      message.success('기기모델이 삭제되었습니다.');
      fetchList(page, size);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '삭제 실패');
    }
  };

  const columns: ColumnsType<AtmModel> = [
    {
      title: '기기업체',
      dataIndex: 'atmCoCd',
      width: 80,
      align: 'center',
      render: (v) => name(ATM_CO_GROUP, v),
    },
    { title: '모델코드', dataIndex: 'modelCd',   width: 90,  align: 'center' },
    { title: '모델명',   dataIndex: 'modelName', width: 130, ellipsis: true },
    {
      title: '기종구분',
      dataIndex: 'atmTypeCd',
      width: 90,
      align: 'center',
      render: (v) => name(ATM_TYPE_GROUP, v),
    },
    {
      title: '최대장진금액',
      dataIndex: 'maxAmount',
      width: 110,
      align: 'right',
      render: (v) => v != null ? v.toLocaleString() : '',
    },
    { title: '원격가능',     dataIndex: 'remoteYn',           width: 80, align: 'center', render: supportCheck },
    { title: '효성구기종여부', dataIndex: 'dataSupported',    width: 110, align: 'center', render: supportCheck },
    { title: '점자지원',     dataIndex: 'dotSupported',       width: 75, align: 'center', render: supportCheck },
    { title: '화면확대',     dataIndex: 'zoomSupported',      width: 75, align: 'center', render: supportCheck },
    { title: '이어폰책',     dataIndex: 'earphoneSupported',  width: 75, align: 'center', render: supportCheck },
    { title: '휠체어',       dataIndex: 'wheelchairSupported',width: 65, align: 'center', render: supportCheck },
    {
      title: 'OS',
      dataIndex: 'osCd',
      width: 120,
      render: (v) => name(OS_CD_GROUP, v) || v || '',
    },
    {
      title: '관리', width: 110, align: 'center', fixed: 'right',
      render: (_, row) => (
        <Space size={4}>
          <Button size="small" icon={<InfoCircleOutlined />} onClick={() => openDetail(row)} />
          <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(row)} />
          <Popconfirm title="기기모델을 삭제하시겠습니까?" onConfirm={() => handleDelete(row.modelCd)} okText="삭제" cancelText="취소">
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <>
      <Form form={searchForm} layout="inline" style={{ marginBottom: 16 }} onFinish={handleSearch}>
        <Form.Item name="modelCd" label="모델코드">
          <Input placeholder="모델코드" allowClear style={{ width: 110 }} />
        </Form.Item>
        <Form.Item name="modelName" label="모델명">
          <Input placeholder="모델명 검색" allowClear style={{ width: 150 }} />
        </Form.Item>
        <Form.Item name="atmCoCd" label="제조사">
          <Select style={{ width: 130 }} options={atmCoOpts} defaultValue="" />
        </Form.Item>
        <Form.Item name="atmTypeCd" label="ATM유형">
          <Select style={{ width: 130 }} options={atmTypeOpts} defaultValue="" />
        </Form.Item>
        <Form.Item name="useYn" label="사용여부">
          <Select style={{ width: 90 }} options={USE_YN_OPT} defaultValue="" />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>조회</Button>
            <Button icon={<ReloadOutlined />} onClick={handleReset}>초기화</Button>
            <Button icon={<PlusOutlined />} onClick={openCreate}>등록</Button>
          </Space>
        </Form.Item>
      </Form>

      <Table<AtmModel>
        rowKey="modelCd"
        size="small" bordered loading={loading}
        columns={columns} dataSource={rows} scroll={{ x: 1400 }}
        pagination={{ current: page, pageSize: size, total, showSizeChanger: true, showTotal: (t) => `총 ${t}건` }}
        onChange={handleTableChange}
      />

      {/* 상세 모달 */}
      <Modal title="기기모델 상세정보" open={detailOpen} onCancel={() => setDetailOpen(false)} footer={null} width={800} destroyOnClose>
        {detail && (
          <Descriptions bordered column={2} size="small">
            <Descriptions.Item label="모델코드">{detail.modelCd}</Descriptions.Item>
            <Descriptions.Item label="모델명">{detail.modelName}</Descriptions.Item>
            <Descriptions.Item label="제조사">{name(ATM_CO_GROUP, detail.atmCoCd)}</Descriptions.Item>
            <Descriptions.Item label="ATM유형">{name(ATM_TYPE_GROUP, detail.atmTypeCd)}</Descriptions.Item>
            <Descriptions.Item label="최신앱버전">{detail.lastAppVer ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="MWI버전">{detail.lastMwiVer ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="에이전트버전">{detail.lastAgentVer ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="저널기록모드">{detail.jnlWriteMode ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="최대금액">{detail.maxAmount?.toLocaleString() ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="OS코드">{detail.osCd ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="은행모델코드">{detail.bankModelCd ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="원격여부">{supportTag(detail.remoteYn)}</Descriptions.Item>
            <Descriptions.Item label="AP로그경로" span={2}>{detail.apLogDir ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="에이전트로그경로" span={2}>{detail.agentLogDir ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="저널인덱스경로" span={2}>{detail.jnlIndexDir ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="다운로드경로" span={2}>{detail.downloadPath ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="실행파일경로" span={2}>{detail.exeFilePath ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="사용여부">{supportTag(detail.useYn)}</Descriptions.Item>
            <Descriptions.Item label="데이터지원">{supportTag(detail.dataSupported)}</Descriptions.Item>
            <Descriptions.Item label="영수증이미지">{supportTag(detail.sprImgSupported)}</Descriptions.Item>
            <Descriptions.Item label="얼굴이미지">{supportTag(detail.faceImgSupported)}</Descriptions.Item>
            <Descriptions.Item label="손바닥이미지">{supportTag(detail.handImgSupported)}</Descriptions.Item>
            <Descriptions.Item label="MCR이미지">{supportTag(detail.mcrImgSupported)}</Descriptions.Item>
            <Descriptions.Item label="수표앞면이미지">{supportTag(detail.chkFrontImgSupported)}</Descriptions.Item>
            <Descriptions.Item label="수표뒷면이미지">{supportTag(detail.chkBackImgSupported)}</Descriptions.Item>
            <Descriptions.Item label="점자지원">{supportTag(detail.dotSupported)}</Descriptions.Item>
            <Descriptions.Item label="이어폰지원">{supportTag(detail.earphoneSupported)}</Descriptions.Item>
            <Descriptions.Item label="화면확대지원">{supportTag(detail.zoomSupported)}</Descriptions.Item>
            <Descriptions.Item label="휠체어지원">{supportTag(detail.wheelchairSupported)}</Descriptions.Item>
          </Descriptions>
        )}
      </Modal>

      {/* 등록/수정 모달 */}
      <Modal
        title={editMode === 'create' ? '기기모델 등록' : '기기모델 수정'}
        open={editOpen} onCancel={() => setEditOpen(false)}
        onOk={handleSave} okText={editMode === 'create' ? '등록' : '저장'}
        cancelText="취소" confirmLoading={saving} width={720} destroyOnClose
      >
        <Form form={editForm} layout="horizontal" labelCol={{ span: 8 }} wrapperCol={{ span: 14 }}>
          {/* 기본 정보 */}
          <Form.Item name="modelCd" label="모델코드" rules={[{ required: true, message: '필수 입력' }]}>
            <Input disabled={editMode === 'edit'} maxLength={20} />
          </Form.Item>
          <Form.Item name="modelName" label="모델명" rules={[{ required: true, message: '필수 입력' }]}>
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item name="atmCoCd" label="제조사">
            <Select options={atmCoOpts.filter((o) => o.value !== '')} allowClear placeholder="선택" />
          </Form.Item>
          <Form.Item name="atmTypeCd" label="ATM유형">
            <Select options={atmTypeOpts.filter((o) => o.value !== '')} allowClear placeholder="선택" />
          </Form.Item>
          <Form.Item name="lastAppVer" label="최신앱버전"><Input maxLength={30} /></Form.Item>
          <Form.Item name="lastMwiVer" label="MWI버전"><Input maxLength={30} /></Form.Item>
          <Form.Item name="lastAgentVer" label="에이전트버전"><Input maxLength={30} /></Form.Item>
          <Form.Item name="jnlWriteMode" label="저널기록모드"><Input maxLength={2} /></Form.Item>
          <Form.Item name="maxAmount" label="최대금액">
            <InputNumber style={{ width: '100%' }} min={0} formatter={(v) => `${v}`.replace(/\B(?=(\d{3})+(?!\d))/g, ',')} />
          </Form.Item>
          <Form.Item name="osCd" label="OS코드"><Input maxLength={10} /></Form.Item>
          <Form.Item name="bankModelCd" label="은행모델코드"><Input maxLength={20} /></Form.Item>
          {/* 경로 */}
          <Form.Item name="apLogDir" label="AP로그경로"><Input maxLength={200} /></Form.Item>
          <Form.Item name="agentLogDir" label="에이전트로그경로"><Input maxLength={200} /></Form.Item>
          <Form.Item name="jnlIndexDir" label="저널인덱스경로"><Input maxLength={200} /></Form.Item>
          <Form.Item name="downloadPath" label="다운로드경로"><Input maxLength={200} /></Form.Item>
          <Form.Item name="exeFilePath" label="실행파일경로"><Input maxLength={200} /></Form.Item>
          {/* Y/N 플래그 */}
          <Form.Item name="useYn" label="사용여부"><Select options={YN_OPT} /></Form.Item>
          <Form.Item name="remoteYn" label="원격여부"><Select options={YN_OPT} /></Form.Item>
          <Form.Item name="dataSupported" label="데이터지원"><Select options={YN_OPT} /></Form.Item>
          <Form.Item name="sprImgSupported" label="영수증이미지"><Select options={YN_OPT} /></Form.Item>
          <Form.Item name="faceImgSupported" label="얼굴이미지"><Select options={YN_OPT} /></Form.Item>
          <Form.Item name="handImgSupported" label="손바닥이미지"><Select options={YN_OPT} /></Form.Item>
          <Form.Item name="mcrImgSupported" label="MCR이미지"><Select options={YN_OPT} /></Form.Item>
          <Form.Item name="chkFrontImgSupported" label="수표앞면이미지"><Select options={YN_OPT} /></Form.Item>
          <Form.Item name="chkBackImgSupported" label="수표뒷면이미지"><Select options={YN_OPT} /></Form.Item>
          <Form.Item name="dotSupported" label="점자지원"><Select options={YN_OPT} /></Form.Item>
          <Form.Item name="earphoneSupported" label="이어폰지원"><Select options={YN_OPT} /></Form.Item>
          <Form.Item name="zoomSupported" label="화면확대지원"><Select options={YN_OPT} /></Form.Item>
          <Form.Item name="wheelchairSupported" label="휠체어지원"><Select options={YN_OPT} /></Form.Item>
        </Form>
      </Modal>
    </>
  );
};

export default AtmModelTab;
