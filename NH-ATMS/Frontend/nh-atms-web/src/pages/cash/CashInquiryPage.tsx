import React, { useEffect, useState } from 'react';
import { Button, DatePicker, Form, Input, Space, Table, message } from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import PageTemplate from '@/components/PageTemplate';
import { cashApi, type CashPosition } from '@/api/cashApi';
import { useBranchStore } from '@/stores/useBranchStore';
import { useCornerStore } from '@/stores/useCornerStore';

interface SearchValues {
  termGroupId?: string;
  termId?: string;
  baseDate?: Dayjs;
}

const fmtDate = (d?: string) => {
  if (!d || d.length < 8) return '-';
  return `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}`;
};

const CashInquiryPage: React.FC = () => {
  const [form] = Form.useForm<SearchValues>();
  const [rows, setRows] = useState<CashPosition[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const branchLabel = useBranchStore((s) => s.label);
  const loadBranches = useBranchStore((s) => s.load);
  const cornerLabel = useCornerStore((s) => s.label);
  const loadCorners = useCornerStore((s) => s.load);

  const fetchList = async (p = page, s = size) => {
    const v = form.getFieldsValue();
    setLoading(true);
    try {
      const res = await cashApi.getList({
        termGroupId: v.termGroupId,
        termId: v.termId,
        baseDate: v.baseDate?.format('YYYYMMDD'),
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
    loadCorners();
    form.setFieldsValue({ baseDate: dayjs() });
    fetchList(1, size);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = () => {
    setPage(1);
    fetchList(1, size);
  };

  const handleReset = () => {
    form.resetFields();
    form.setFieldsValue({ baseDate: dayjs() });
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

  const columns: ColumnsType<CashPosition> = [
    { title: '영업점', dataIndex: 'termGroupId', width: 160, align: 'center', render: (v) => branchLabel(v) },
    {
      title: '코너',
      width: 150,
      align: 'center',
      render: (_, r) => cornerLabel(r.termGroupId, r.termId),
    },
    { title: '기준일자', width: 120, align: 'center', render: (_, r) => fmtDate(r.jnlDate) },
    { title: '카세트', dataIndex: 'cassetteNo', width: 80, align: 'center' },
    { title: '통화', dataIndex: 'currency', width: 70, align: 'center' },
    {
      title: '권종',
      dataIndex: 'denomination',
      width: 110,
      align: 'right',
      render: (v?: number) => (v != null ? v.toLocaleString() : '-'),
    },
    {
      title: '잔량(매)',
      dataIndex: 'remainQty',
      width: 100,
      align: 'right',
      render: (v?: number) => (v != null ? v.toLocaleString() : '-'),
    },
    {
      title: '금액',
      dataIndex: 'amount',
      width: 140,
      align: 'right',
      render: (v?: number) => (v != null ? v.toLocaleString() : '-'),
    },
  ];

  const totalAmount = rows.reduce((sum, r) => sum + (r.amount ?? 0), 0);

  return (
    <PageTemplate title="기기시재조회">
      <Form form={form} layout="inline" style={{ marginBottom: 16 }} onFinish={handleSearch}>
        <Form.Item name="termGroupId" label="영업점">
          <Input placeholder="영업점코드" allowClear style={{ width: 130 }} />
        </Form.Item>
        <Form.Item name="termId" label="코너코드">
          <Input placeholder="코너코드" allowClear style={{ width: 130 }} />
        </Form.Item>
        <Form.Item name="baseDate" label="기준일자">
          <DatePicker />
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

      <Table<CashPosition>
        rowKey={(r) => `${r.termGroupId}-${r.termId}-${r.cassetteType}-${r.cassetteNo}`}
        size="small"
        bordered
        loading={loading}
        columns={columns}
        dataSource={rows}
        scroll={{ x: 900 }}
        pagination={{
          current: page,
          pageSize: size,
          total,
          showSizeChanger: true,
          showTotal: (t) => `총 ${t}건 / 현재 페이지 합계 ${totalAmount.toLocaleString()}원`,
        }}
        onChange={handleTableChange}
      />
    </PageTemplate>
  );
};

export default CashInquiryPage;