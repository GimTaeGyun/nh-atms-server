import React, { useEffect, useState } from 'react';
import { Button, DatePicker, Form, Input, InputNumber, Space, Table, Tag, message } from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import PageTemplate from '@/components/PageTemplate';
import { faultFrequentApi, type FaultFrequent } from '@/api/faultFrequentApi';
import { useBranchStore } from '@/stores/useBranchStore';

const { RangePicker } = DatePicker;

interface SearchValues {
  termGroupId?: string;
  termId?: string;
  minFaultCnt?: number;
  range?: [Dayjs, Dayjs];
}

const fmtDateTime = (d?: string, t?: string) => {
  if (!d || d.length < 8) return '-';
  const ymd = `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}`;
  if (!t || t.length < 6) return ymd;
  return `${ymd} ${t.substring(0, 2)}:${t.substring(2, 4)}:${t.substring(4, 6)}`;
};

const FaultFrequentPage: React.FC = () => {
  const [form] = Form.useForm<SearchValues>();
  const [rows, setRows] = useState<FaultFrequent[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const branchLabel = useBranchStore((s) => s.label);
  const loadBranches = useBranchStore((s) => s.load);

  const fetchList = async (p = page, s = size) => {
    const v = form.getFieldsValue();
    setLoading(true);
    try {
      const res = await faultFrequentApi.getList({
        termGroupId: v.termGroupId,
        termId: v.termId,
        minFaultCnt: v.minFaultCnt,
        fromDate: v.range?.[0]?.format('YYYYMMDD'),
        toDate: v.range?.[1]?.format('YYYYMMDD'),
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
    form.setFieldsValue({ range: [dayjs().subtract(7, 'day'), dayjs()], minFaultCnt: 1 });
    fetchList(1, size);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = () => {
    setPage(1);
    fetchList(1, size);
  };

  const handleReset = () => {
    form.resetFields();
    form.setFieldsValue({ range: [dayjs().subtract(7, 'day'), dayjs()], minFaultCnt: 1 });
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

  const columns: ColumnsType<FaultFrequent> = [
    {
      title: '순위',
      width: 60,
      align: 'center',
      render: (_, __, i) => (page - 1) * size + i + 1,
    },
    { title: '영업점', dataIndex: 'termGroupId', width: 140, align: 'center', render: (v) => branchLabel(v) },
    { title: '단말', dataIndex: 'termId', width: 110, align: 'center' },
    { title: '전체건수', dataIndex: 'totalCnt', width: 90, align: 'right' },
    {
      title: '장애건수',
      dataIndex: 'faultCnt',
      width: 90,
      align: 'right',
      render: (v: number) => <Tag color="red">{v}</Tag>,
    },
    {
      title: '장애율(%)',
      dataIndex: 'faultRate',
      width: 100,
      align: 'right',
      render: (v?: number) => (v != null ? v.toFixed(2) : '-'),
    },
    {
      title: '최근장애일시',
      width: 170,
      render: (_, r) => fmtDateTime(r.lastFaultDate, r.lastFaultTime),
    },
    {
      title: '최근장애코드',
      dataIndex: 'lastErrCd',
      width: 120,
      align: 'center',
      render: (v?: string) => (v ? <Tag color="volcano">{v}</Tag> : '-'),
    },
  ];

  return (
    <PageTemplate title="장애다발기기조회">
      <Form form={form} layout="inline" style={{ marginBottom: 16 }} onFinish={handleSearch}>
        <Form.Item name="termGroupId" label="영업점">
          <Input placeholder="영업점코드" allowClear style={{ width: 130 }} />
        </Form.Item>
        <Form.Item name="termId" label="단말">
          <Input placeholder="단말번호" allowClear style={{ width: 130 }} />
        </Form.Item>
        <Form.Item name="range" label="발생일자">
          <RangePicker />
        </Form.Item>
        <Form.Item name="minFaultCnt" label="최소장애건수">
          <InputNumber min={1} style={{ width: 100 }} />
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

      <Table<FaultFrequent>
        rowKey={(r) => `${r.termGroupId}-${r.termId}`}
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
          showTotal: (t) => `총 ${t}건`,
        }}
        onChange={handleTableChange}
      />
    </PageTemplate>
  );
};

export default FaultFrequentPage;
