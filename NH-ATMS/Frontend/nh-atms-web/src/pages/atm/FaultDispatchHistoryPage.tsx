import React, { useEffect, useMemo, useState } from 'react';
import { Button, Form, Input, Select, Space, Table, Tag, DatePicker, message } from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import PageTemplate from '@/components/PageTemplate';
import { faultMgmtApi, type FaultManagement } from '@/api/faultMgmtApi';
import { useBranchStore } from '@/stores/useBranchStore';

/**
 * 출동조치이력조회 (/atm/fault-dispatch)
 *  - 데이터: T_FAULT_MANAGEMENT (출동/장애접수 이력) 읽기전용 조회
 *  - MG [출동조치관리] > 출동조치이력조회 대응
 *  - 접수(기기유지보수접수)와 분리된 이력 열람 화면
 */

const { RangePicker } = DatePicker;

interface SearchValues {
  termGroupId?: string;
  termId?: string;
  faultStatus?: string;
  range?: [Dayjs, Dayjs];
}

const STATUS_OPTIONS = [
  { value: 'O', label: '출동요청' },
  { value: 'P', label: '진행' },
  { value: 'C', label: '완료' },
  { value: 'X', label: '취소' },
];

const renderStatus = (v?: string) => {
  if (v === 'O') return <Tag color="orange">출동요청</Tag>;
  if (v === 'P') return <Tag color="blue">진행</Tag>;
  if (v === 'C') return <Tag color="green">완료</Tag>;
  if (v === 'X') return <Tag>취소</Tag>;
  return <Tag>{v ?? '-'}</Tag>;
};

const FAULT_TYPE_LABEL: Record<string, string> = {
  HW: '하드웨어',
  SW: '소프트웨어',
  NW: '네트워크',
  CASH: '현금/시재',
  ETC: '기타',
};

const fmtDateTime = (d?: string, t?: string) => {
  if (!d || d.length < 8) return '-';
  const ymd = `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}`;
  if (!t || t.length < 6) return ymd;
  return `${ymd} ${t.substring(0, 2)}:${t.substring(2, 4)}:${t.substring(4, 6)}`;
};

const errMsgOf = (e: unknown, fallback: string): string =>
  (e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? fallback;

const FaultDispatchHistoryPage: React.FC = () => {
  const [form] = Form.useForm<SearchValues>();
  const [rows, setRows] = useState<FaultManagement[]>([]);
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
      const res = await faultMgmtApi.getList({
        termGroupId: v.termGroupId,
        termId: v.termId,
        faultStatus: v.faultStatus,
        fromDate: v.range?.[0]?.format('YYYYMMDD'),
        toDate: v.range?.[1]?.format('YYYYMMDD'),
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
    form.setFieldsValue({ range: [dayjs().subtract(7, 'day'), dayjs()] });
    fetchList(1, size);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = () => {
    setPage(1);
    fetchList(1, size);
  };

  const handleReset = () => {
    form.resetFields();
    form.setFieldsValue({ range: [dayjs().subtract(7, 'day'), dayjs()] });
  };

  const handleTableChange = (pg: TablePaginationConfig) => {
    const newPage = pg.current ?? 1;
    const newSize = pg.pageSize ?? size;
    setPage(newPage);
    setSize(newSize);
    fetchList(newPage, newSize);
  };

  const columns: ColumnsType<FaultManagement> = useMemo(
    () => [
      { title: '접수번호', dataIndex: 'faultSeq', width: 90, align: 'center' },
      { title: '상태', dataIndex: 'faultStatus', width: 100, align: 'center', render: renderStatus },
      { title: '영업점(금고)', dataIndex: 'termGroupId', width: 150, align: 'center', render: (v) => branchLabel(v) },
      { title: '기번(단말)', dataIndex: 'termId', width: 100, align: 'center' },
      {
        title: '출동요청사유',
        dataIndex: 'faultType',
        width: 110,
        align: 'center',
        render: (v?: string) => (v ? FAULT_TYPE_LABEL[v] ?? v : '-'),
      },
      {
        title: '장애코드',
        dataIndex: 'errCd',
        width: 110,
        align: 'center',
        render: (v?: string) => (v ? <Tag color="red">{v}</Tag> : '-'),
      },
      { title: '출동업체', dataIndex: 'vendorCd', width: 110, align: 'center', render: (v?: string) => v ?? '-' },
      {
        title: '출동요청내용',
        dataIndex: 'faultDesc',
        width: 220,
        ellipsis: true,
        render: (v?: string) => v ?? '-',
      },
      { title: '출동요청일시', width: 170, render: (_, r) => fmtDateTime(r.callDate, r.callTime) },
      { title: '완료일시', width: 170, render: (_, r) => fmtDateTime(r.doneDate, r.doneTime) },
    ],
    [branchLabel],
  );

  return (
    <PageTemplate title="출동조치이력조회">
      <div className="search-panel" style={{ marginBottom: 8 }}>
        <Form form={form} layout="inline" onFinish={handleSearch} style={{ gap: 8, flexWrap: 'wrap' }}>
          <Form.Item name="termGroupId" label="영업점">
            <Input size="small" placeholder="영업점코드" style={{ width: 110 }} allowClear />
          </Form.Item>
          <Form.Item name="termId" label="단말">
            <Input size="small" placeholder="단말번호" style={{ width: 100 }} allowClear />
          </Form.Item>
          <Form.Item name="faultStatus" label="상태">
            <Select size="small" placeholder="전체" style={{ width: 120 }} allowClear options={STATUS_OPTIONS} />
          </Form.Item>
          <Form.Item name="range" label="출동요청일">
            <RangePicker size="small" />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button size="small" type="primary" htmlType="submit" icon={<SearchOutlined />}>
                조회
              </Button>
              <Button size="small" icon={<ReloadOutlined />} onClick={handleReset}>
                초기화
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </div>

      <Table<FaultManagement>
        rowKey="faultSeq"
        size="small"
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
    </PageTemplate>
  );
};

export default FaultDispatchHistoryPage;
