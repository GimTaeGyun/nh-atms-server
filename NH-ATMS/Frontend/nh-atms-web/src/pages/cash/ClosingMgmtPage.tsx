import React, { useEffect, useState } from 'react';
import { Button, DatePicker, Form, Input, Select, Space, Table, Tag, message } from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import PageTemplate from '@/components/PageTemplate';
import { dayTotalApi, type DayTotalRow } from '@/api/dayTotalApi';
import { useBranchStore } from '@/stores/useBranchStore';
import { useCornerStore } from '@/stores/useCornerStore';

interface SearchValues {
  termGroupId?: string;
  termId?: string;
  jnlDate?: Dayjs;
  actionType?: string;
  totalSrcCd?: string;
}

const errMsgOf = (e: unknown, fallback: string) =>
  (e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? fallback;

const fmtDate = (d?: string) => {
  if (!d || d.length < 8) return '-';
  return `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}`;
};

const fmtTime = (t?: string) => {
  if (!t || t.length < 6) return '-';
  return `${t.substring(0, 2)}:${t.substring(2, 4)}:${t.substring(4, 6)}`;
};

const ACTION_LABEL: Record<string, string> = { '5': '소계', '6': '합계' };
const SRC_LABEL: Record<string, string> = { '1': '호스트', '2': '기기' };

const ClosingMgmtPage: React.FC = () => {
  const [form] = Form.useForm<SearchValues>();
  const [rows, setRows] = useState<DayTotalRow[]>([]);
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
      const res = await dayTotalApi.getList({
        termGroupId: v.termGroupId,
        termId: v.termId,
        jnlDate: v.jnlDate?.format('YYYYMMDD'),
        actionType: v.actionType,
        totalSrcCd: v.totalSrcCd,
        page: p,
        size: s,
      });
      const data = res.data.data;
      setRows(data.content);
      setTotal(data.totalCount);
    } catch (e: unknown) {
      message.error(errMsgOf(e, '조회 실패'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadBranches();
    loadCorners();
    form.setFieldsValue({ jnlDate: dayjs() });
    fetchList(1, size);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = () => {
    setPage(1);
    fetchList(1, size);
  };

  const handleReset = () => {
    form.resetFields();
    form.setFieldsValue({ jnlDate: dayjs() });
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

  const columns: ColumnsType<DayTotalRow> = [
    { title: '영업점', dataIndex: 'termGroupId', width: 150, align: 'center', render: (v) => branchLabel(v) },
    {
      title: '코너',
      width: 140,
      align: 'center',
      render: (_, r) => cornerLabel(r.termGroupId, r.termId),
    },
    { title: '마감일자', width: 110, align: 'center', render: (_, r) => fmtDate(r.jnlDate) },
    { title: '마감시각', width: 100, align: 'center', render: (_, r) => fmtTime(r.jnlTime) },
    {
      title: '마감종류',
      dataIndex: 'actionType',
      width: 90,
      align: 'center',
      render: (v?: string) =>
        v ? <Tag color={v === '6' ? 'blue' : 'default'}>{ACTION_LABEL[v] ?? v}</Tag> : '-',
    },
    {
      title: '집계원천',
      dataIndex: 'totalSrcCd',
      width: 90,
      align: 'center',
      render: (v?: string) => (v ? (SRC_LABEL[v] ?? v) : '-'),
    },
    { title: '항목코드', dataIndex: 'totalCd', width: 90, align: 'center' },
    {
      title: '집계값',
      dataIndex: 'totalVal',
      width: 140,
      align: 'right',
      render: (v?: string) => {
        const n = v != null ? Number(v) : NaN;
        return Number.isFinite(n) ? n.toLocaleString() : (v ?? '-');
      },
    },
    {
      title: '마감기간',
      width: 220,
      align: 'center',
      render: (_, r) =>
        `${fmtDate(r.beginDate)} ${fmtTime(r.beginTime)} ~ ${fmtTime(r.endTime)}`,
    },
  ];

  return (
    <PageTemplate title="마감관리">
      <Form form={form} layout="inline" style={{ marginBottom: 16 }} onFinish={handleSearch}>
        <Form.Item name="termGroupId" label="영업점">
          <Input placeholder="영업점코드" allowClear style={{ width: 120 }} />
        </Form.Item>
        <Form.Item name="termId" label="코너코드">
          <Input placeholder="코너코드" allowClear style={{ width: 120 }} />
        </Form.Item>
        <Form.Item name="jnlDate" label="마감일자">
          <DatePicker />
        </Form.Item>
        <Form.Item name="actionType" label="마감종류">
          <Select
            allowClear
            placeholder="전체"
            style={{ width: 110 }}
            options={[
              { value: '5', label: '소계' },
              { value: '6', label: '합계' },
            ]}
          />
        </Form.Item>
        <Form.Item name="totalSrcCd" label="집계원천">
          <Select
            allowClear
            placeholder="전체"
            style={{ width: 110 }}
            options={[
              { value: '1', label: '호스트' },
              { value: '2', label: '기기' },
            ]}
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

      <Table<DayTotalRow>
        rowKey={(r) => `${r.termGroupId}-${r.termId}-${r.jnlDate}-${r.jnlTime}-${r.totalSrcCd}-${r.totalCd}`}
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
    </PageTemplate>
  );
};

export default ClosingMgmtPage;
