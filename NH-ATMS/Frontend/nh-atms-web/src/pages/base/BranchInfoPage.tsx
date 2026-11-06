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
  message,
} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { SearchOutlined, ReloadOutlined, InfoCircleOutlined } from '@ant-design/icons';
import PageTemplate from '@/components/PageTemplate';
import { branchApi, type Branch, type BranchSearchParams } from '@/api/branchApi';

const BRANCH_TYPE_OPTIONS = [
  { value: '', label: '전체' },
  { value: '1', label: '지점' },
  { value: '2', label: '출장소' },
  { value: '3', label: '본점' },
];

const branchTypeLabel = (v?: string) => {
  const opt = BRANCH_TYPE_OPTIONS.find((o) => o.value === v);
  return opt?.label ?? (v || '-');
};

const BranchInfoPage: React.FC = () => {
  const [form] = Form.useForm<BranchSearchParams>();
  const [rows, setRows] = useState<Branch[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [detail, setDetail] = useState<Branch | null>(null);
  const [detailOpen, setDetailOpen] = useState(false);

  const fetchList = async (p = page, s = size) => {
    const v = form.getFieldsValue();
    setLoading(true);
    try {
      const res = await branchApi.getList({
        termGroupId: v.termGroupId || undefined,
        branchCd:   v.branchCd   || undefined,
        branchName: v.branchName || undefined,
        branchTypeCd: (v as any).branchTypeCd || undefined,
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

  const openDetail = (row: Branch) => {
    setDetail(row);
    setDetailOpen(true);
  };

  const columns: ColumnsType<Branch> = [
    { title: '영업점코드', dataIndex: 'termGroupId',  width: 100, align: 'center' },
    { title: '영업점명',   dataIndex: 'termGroupName', width: 160, ellipsis: true },
    { title: '본지점코드', dataIndex: 'branchCd',     width: 100, align: 'center' },
    { title: '본지점명',   dataIndex: 'branchName',   width: 200, ellipsis: true },
    {
      title: '구분',
      dataIndex: 'branchTypeCd',
      width: 80,
      align: 'center',
      render: branchTypeLabel,
    },
    { title: '우편번호', dataIndex: 'zipcode',    width: 90,  align: 'center' },
    { title: '주소',     dataIndex: 'address',    ellipsis: true },
    { title: '전화번호', dataIndex: 'phoneNo',    width: 130, align: 'center' },
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
    <PageTemplate title="지점정보조회">
      <Form form={form} layout="inline" style={{ marginBottom: 16 }} onFinish={handleSearch}>
        <Form.Item name="termGroupId" label="영업점코드">
          <Input placeholder="영업점코드" allowClear style={{ width: 120 }} />
        </Form.Item>
        <Form.Item name="branchCd" label="본지점코드">
          <Input placeholder="본지점코드" allowClear style={{ width: 120 }} />
        </Form.Item>
        <Form.Item name="branchName" label="지점명">
          <Input placeholder="지점명 검색" allowClear style={{ width: 160 }} />
        </Form.Item>
        <Form.Item name="branchTypeCd" label="구분">
          <Select
            style={{ width: 100 }}
            options={BRANCH_TYPE_OPTIONS}
            defaultValue=""
          />
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

      <Table<Branch>
        rowKey={(r) => `${r.termGroupId}-${r.branchCd}-${r.branchName}`}
        size="small"
        bordered
        loading={loading}
        columns={columns}
        dataSource={rows}
        scroll={{ x: 1100 }}
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
        title="지점 상세정보"
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        footer={null}
        width={700}
        destroyOnClose
      >
        {detail && (
          <Descriptions bordered column={2} size="small">
            <Descriptions.Item label="영업점코드">{detail.termGroupId}</Descriptions.Item>
            <Descriptions.Item label="영업점명">{detail.termGroupName ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="본지점코드">{detail.branchCd}</Descriptions.Item>
            <Descriptions.Item label="본지점명">{detail.branchName}</Descriptions.Item>
            <Descriptions.Item label="구분">{branchTypeLabel(detail.branchTypeCd)}</Descriptions.Item>
            <Descriptions.Item label="팩스">{detail.faxNo ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="전화번호">{detail.phoneNo ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="우편번호">{detail.zipcode ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="주소" span={2}>{detail.address ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="상세주소" span={2}>{detail.detailAddress ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="지역명">{detail.sotyNm ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="작업자코드">{detail.partnerCd ?? '-'}</Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </PageTemplate>
  );
};

export default BranchInfoPage;
