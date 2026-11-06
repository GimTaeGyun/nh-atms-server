import React, { useEffect, useMemo, useState } from 'react';
import { Button, Form, Input, Select, Space, Table, Tag, DatePicker, message } from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import PageTemplate from '@/components/PageTemplate';
import { smsApi, type SmsSendData } from '@/api/smsApi';
import { useBranchStore } from '@/stores/useBranchStore';

/**
 * 장애SMS통보이력 (/atm/sms-history)
 *  - 데이터: T_SMS_SEND_DATA (SMS 발송 이력) 읽기전용 조회
 *  - MG [출동조치관리] > 장애SMS통보이력 대응
 */

const { RangePicker } = DatePicker;

interface SearchValues {
  termGroupId?: string;
  recvPhone?: string;
  sendStatus?: string;
  range?: [Dayjs, Dayjs];
}

const STATUS_OPTIONS = [
  { value: 'W', label: '대기' },
  { value: 'S', label: '성공' },
  { value: 'F', label: '실패' },
];

const renderStatus = (v?: string) => {
  if (v === 'S') return <Tag color="green">성공</Tag>;
  if (v === 'F') return <Tag color="red">실패</Tag>;
  if (v === 'W') return <Tag color="orange">대기</Tag>;
  return <Tag>{v ?? '-'}</Tag>;
};

const fmtDateTime = (d?: string, t?: string) => {
  if (!d || d.length < 8) return '-';
  const ymd = `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}`;
  if (!t || t.length < 6) return ymd;
  return `${ymd} ${t.substring(0, 2)}:${t.substring(2, 4)}:${t.substring(4, 6)}`;
};

const errMsgOf = (e: unknown, fallback: string): string =>
  (e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? fallback;

const SmsHistoryPage: React.FC = () => {
  const [form] = Form.useForm<SearchValues>();
  const [rows, setRows] = useState<SmsSendData[]>([]);
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
      const res = await smsApi.getList({
        termGroupId: v.termGroupId,
        recvPhone: v.recvPhone,
        sendStatus: v.sendStatus,
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

  const columns: ColumnsType<SmsSendData> = useMemo(
    () => [
      { title: '일련번호', dataIndex: 'smsSeq', width: 90, align: 'center' },
      { title: '발송상태', dataIndex: 'sendStatus', width: 100, align: 'center', render: renderStatus },
      { title: '영업점(금고)', dataIndex: 'termGroupId', width: 150, align: 'center', render: (v?: string) => (v ? branchLabel(v) : '-') },
      { title: '기번(단말)', dataIndex: 'termId', width: 100, align: 'center', render: (v?: string) => v ?? '-' },
      { title: '수신번호', dataIndex: 'recvPhone', width: 130, align: 'center' },
      { title: '발신번호', dataIndex: 'sendPhone', width: 130, align: 'center', render: (v?: string) => v ?? '-' },
      { title: '메시지내용', dataIndex: 'msgContent', width: 280, ellipsis: true },
      {
        title: '장애코드',
        dataIndex: 'errCd',
        width: 100,
        align: 'center',
        render: (v?: string) => (v ? <Tag color="red">{v}</Tag> : '-'),
      },
      { title: '발송일시', width: 170, render: (_, r) => fmtDateTime(r.sendDate, r.sendTime) },
      { title: '결과메시지', dataIndex: 'resultMsg', width: 160, ellipsis: true, render: (v?: string) => v ?? '-' },
    ],
    [branchLabel],
  );

  return (
    <PageTemplate title="장애SMS통보이력">
      <div className="search-panel" style={{ marginBottom: 8 }}>
        <Form form={form} layout="inline" onFinish={handleSearch} style={{ gap: 8, flexWrap: 'wrap' }}>
          <Form.Item name="termGroupId" label="영업점">
            <Input size="small" placeholder="영업점코드" style={{ width: 110 }} allowClear />
          </Form.Item>
          <Form.Item name="recvPhone" label="수신번호">
            <Input size="small" placeholder="수신번호" style={{ width: 130 }} allowClear />
          </Form.Item>
          <Form.Item name="sendStatus" label="발송상태">
            <Select size="small" placeholder="전체" style={{ width: 120 }} allowClear options={STATUS_OPTIONS} />
          </Form.Item>
          <Form.Item name="range" label="발송일">
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

      <Table<SmsSendData>
        rowKey="smsSeq"
        size="small"
        loading={loading}
        columns={columns}
        dataSource={rows}
        scroll={{ x: 1500 }}
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

export default SmsHistoryPage;
