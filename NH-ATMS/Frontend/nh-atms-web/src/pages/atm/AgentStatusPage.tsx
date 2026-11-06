import React, { useEffect, useState } from 'react';
import { Button, DatePicker, Form, Input, Select, Space, Table, Tag, message } from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import { type Dayjs } from 'dayjs';
import PageTemplate from '@/components/PageTemplate';
import { agentStatusApi, type AgentStatus } from '@/api/agentStatusApi';
import { useBranchStore } from '@/stores/useBranchStore';

const { RangePicker } = DatePicker;

interface SearchValues {
  termGroupId?: string;
  termId?: string;
  connectFlag?: string;
  range?: [Dayjs, Dayjs];
}

const fmtDateTime = (d?: string, t?: string) => {
  if (!d || d.length < 8) return '-';
  const ymd = `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}`;
  if (!t || t.length < 6) return ymd;
  return `${ymd} ${t.substring(0, 2)}:${t.substring(2, 4)}:${t.substring(4, 6)}`;
};

const renderConnect = (v?: string) =>
  v === '1' ? <Tag color="green">접속</Tag> : <Tag color="default">해제</Tag>;

const AgentStatusPage: React.FC = () => {
  const [form] = Form.useForm<SearchValues>();
  const [rows, setRows] = useState<AgentStatus[]>([]);
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
      const res = await agentStatusApi.getList({
        termGroupId: v.termGroupId,
        termId: v.termId,
        connectFlag: v.connectFlag,
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

  const columns: ColumnsType<AgentStatus> = [
    { title: '영업점', dataIndex: 'termGroupId', width: 140, align: 'center', render: (v) => branchLabel(v) },
    { title: '단말', dataIndex: 'termId', width: 100, align: 'center' },
    { title: '기기 IP', dataIndex: 'atmIp', width: 130, align: 'center' },
    { title: '접속상태', dataIndex: 'connectFlag', width: 90, align: 'center', render: renderConnect },
    { title: '기동일시', width: 170, render: (_, r) => fmtDateTime(r.startDate, r.startTime) },
    { title: '최종폴링', width: 170, render: (_, r) => fmtDateTime(r.lastPollDate, r.lastPollTime) },
    { title: '종료일시', width: 170, render: (_, r) => fmtDateTime(r.exitDate, r.exitTime) },
    { title: 'AG버전', dataIndex: 'agentVer', width: 90, align: 'center' },
    { title: 'AP버전', dataIndex: 'apDownVer', width: 90, align: 'center' },
    { title: 'OS', dataIndex: 'osName', ellipsis: true },
  ];

  return (
    <PageTemplate title="기기개국현황">
      <Form form={form} layout="inline" style={{ marginBottom: 16 }} onFinish={handleSearch}>
        <Form.Item name="termGroupId" label="영업점">
          <Input placeholder="영업점코드" allowClear style={{ width: 130 }} />
        </Form.Item>
        <Form.Item name="termId" label="단말">
          <Input placeholder="단말번호" allowClear style={{ width: 130 }} />
        </Form.Item>
        <Form.Item name="connectFlag" label="접속상태">
          <Select
            allowClear
            placeholder="전체"
            style={{ width: 110 }}
            options={[
              { value: '1', label: '접속' },
              { value: '0', label: '해제' },
            ]}
          />
        </Form.Item>
        <Form.Item name="range" label="기동일자">
          <RangePicker />
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

      <Table<AgentStatus>
        rowKey="atmIp"
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
    </PageTemplate>
  );
};

export default AgentStatusPage;
