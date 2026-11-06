import React, { useEffect, useState } from 'react';
import {
  Button,
  Descriptions,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  message,
} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { SearchOutlined, ReloadOutlined, InfoCircleOutlined } from '@ant-design/icons';
import PageTemplate from '@/components/PageTemplate';
import { cornerApi, type Corner, type CornerSearchParams } from '@/api/cornerApi';
import { useBranchStore } from '@/stores/useBranchStore';

const CORNER_TYPE_OPTIONS = [
  { value: '', label: '전체' },
  { value: '1', label: '점내' },
  { value: '2', label: '점외' },
  { value: '3', label: '독립' },
];

const CORNER_STATUS_OPTIONS = [
  { value: '', label: '전체' },
  { value: '0', label: '정상' },
  { value: '1', label: '임시폐쇄' },
  { value: '2', label: '폐쇄' },
];

const cornerTypeLabel = (v?: string) => {
  const opt = CORNER_TYPE_OPTIONS.find((o) => o.value === v);
  return opt?.label ?? (v || '-');
};

const renderStatus = (v?: string) => {
  if (v === '0') return <Tag color="green">정상</Tag>;
  if (v === '1') return <Tag color="orange">임시폐쇄</Tag>;
  if (v === '2') return <Tag color="red">폐쇄</Tag>;
  return <Tag>{v ?? '-'}</Tag>;
};

const fmtDate = (d?: string) => {
  if (!d || d.length < 8) return '-';
  return `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}`;
};

const fmtTime = (t?: string) => {
  if (!t || t.length < 4) return '-';
  return `${t.substring(0, 2)}:${t.substring(2, 4)}`;
};

const CornerInfoPage: React.FC = () => {
  const [form] = Form.useForm<CornerSearchParams>();
  const [rows, setRows] = useState<Corner[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [detail, setDetail] = useState<Corner | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);
  const branchLabel = useBranchStore((s) => s.label);
  const loadBranches = useBranchStore((s) => s.load);

  const fetchList = async (p = page, s = size) => {
    const v = form.getFieldsValue();
    setLoading(true);
    try {
      const res = await cornerApi.getList({
        termGroupId:  v.termGroupId  || undefined,
        cornerCd:     v.cornerCd    || undefined,
        cornerName:   v.cornerName  || undefined,
        cornerTypeCd: (v as any).cornerTypeCd  || undefined,
        cornerStatus: (v as any).cornerStatus  || undefined,
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
    loadBranches();
    fetchList(1, size);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = () => {
    setPage(1);
    fetchList(1, size);
  };

  const handleReset = () => {
    form.resetFields();
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

  const openDetail = (row: Corner) => {
    setDetail(row);
    setDetailOpen(true);
  };

  const columns: ColumnsType<Corner> = [
    {
      title: '영업점',
      dataIndex: 'termGroupId',
      width: 150,
      align: 'center',
      render: (v) => branchLabel(v),
    },
    { title: '코너코드', dataIndex: 'cornerCd',    width: 80,  align: 'center' },
    { title: '코너명',   dataIndex: 'cornerName',  width: 180, ellipsis: true },
    {
      title: '구분',
      dataIndex: 'cornerTypeCd',
      width: 80,
      align: 'center',
      render: cornerTypeLabel,
    },
    {
      title: '상태',
      dataIndex: 'cornerStatus',
      width: 90,
      align: 'center',
      render: renderStatus,
    },
    {
      title: '운영시간',
      width: 130,
      align: 'center',
      render: (_, r) =>
        r.serviceStartTime && r.serviceEndTime
          ? `${fmtTime(r.serviceStartTime)} ~ ${fmtTime(r.serviceEndTime)}`
          : '-',
    },
    { title: '전화번호', dataIndex: 'phoneNo',    width: 130, align: 'center' },
    { title: '주소',     dataIndex: 'address',   ellipsis: true },
    { title: '개설일자', dataIndex: 'openDate',  width: 110, align: 'center', render: fmtDate },
    {
      title: '상세',
      width: 70,
      align: 'center',
      fixed: 'right',
      render: (_, row) => (
        <Button size="small" icon={<InfoCircleOutlined />} onClick={() => openDetail(row)} />
      ),
    },
  ];

  return (
    <PageTemplate title="코너정보조회">
      <Form form={form} layout="inline" style={{ marginBottom: 16 }} onFinish={handleSearch}>
        <Form.Item name="termGroupId" label="영업점코드">
          <Input placeholder="영업점코드" allowClear style={{ width: 120 }} />
        </Form.Item>
        <Form.Item name="cornerCd" label="코너코드">
          <Input placeholder="코너코드" allowClear style={{ width: 90 }} />
        </Form.Item>
        <Form.Item name="cornerName" label="코너명">
          <Input placeholder="코너명 검색" allowClear style={{ width: 160 }} />
        </Form.Item>
        <Form.Item name="cornerTypeCd" label="구분">
          <Select style={{ width: 90 }} options={CORNER_TYPE_OPTIONS} defaultValue="" />
        </Form.Item>
        <Form.Item name="cornerStatus" label="상태">
          <Select style={{ width: 90 }} options={CORNER_STATUS_OPTIONS} defaultValue="" />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
              조회
            </Button>
            <Button icon={<ReloadOutlined />} onClick={handleReset}>
              초기화
            </Button>
          </Space>
        </Form.Item>
      </Form>

      <Table<Corner>
        rowKey={(r) => `${r.termGroupId}-${r.cornerCd}`}
        size="small"
        bordered
        loading={loading}
        columns={columns}
        dataSource={rows}
        scroll={{ x: 1200 }}
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
        title="코너 상세정보"
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        footer={null}
        width={780}
        destroyOnClose
      >
        {detail && (
          <Descriptions bordered column={2} size="small">
            <Descriptions.Item label="영업점">{branchLabel(detail.termGroupId)}</Descriptions.Item>
            <Descriptions.Item label="코너코드">{detail.cornerCd}</Descriptions.Item>
            <Descriptions.Item label="코너명" span={2}>{detail.cornerName ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="코너구분">{cornerTypeLabel(detail.cornerTypeCd)}</Descriptions.Item>
            <Descriptions.Item label="상태">{renderStatus(detail.cornerStatus)}</Descriptions.Item>
            <Descriptions.Item label="운영시작">{fmtTime(detail.serviceStartTime)}</Descriptions.Item>
            <Descriptions.Item label="운영종료">{fmtTime(detail.serviceEndTime)}</Descriptions.Item>
            <Descriptions.Item label="개설일자">{fmtDate(detail.openDate)}</Descriptions.Item>
            <Descriptions.Item label="폐쇄일자">{fmtDate(detail.closeDate)}</Descriptions.Item>
            {detail.tempClsStartDate && (
              <Descriptions.Item label="임시폐쇄기간" span={2}>
                {fmtDate(detail.tempClsStartDate)} ~ {fmtDate(detail.tempClsEndDate)}
                {detail.tempClsReason ? ` (${detail.tempClsReason})` : ''}
              </Descriptions.Item>
            )}
            <Descriptions.Item label="전화번호">{detail.phoneNo ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="우편번호">{detail.zipcode ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="주소" span={2}>{detail.address ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="위도">{detail.lat ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="경도">{detail.lng ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="부스수">{detail.boothCount ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="CCTV">{detail.cctvSetFlag === '1' ? 'Y' : (detail.cctvSetFlag === '0' ? 'N' : '-')}</Descriptions.Item>
            <Descriptions.Item label="용역업체">{detail.outsrcCoCd ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="경비업체">{detail.guardCoCd ?? '-'}</Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </PageTemplate>
  );
};

export default CornerInfoPage;
