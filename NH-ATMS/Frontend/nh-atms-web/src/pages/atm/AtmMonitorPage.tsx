import React, { useCallback, useEffect, useState } from 'react';
import {
  Button, Card, Checkbox, Col, DatePicker, Descriptions, Empty, Form, Input,
  Row, Select, Space, Spin, Table, Tabs, Tag, Tooltip, Typography, message,
} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { SearchOutlined, ReloadOutlined, VerticalAlignBottomOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import PageTemplate from '@/components/PageTemplate';
import { atmMonitorApi, type AtmMonitor } from '@/api/atmMonitorApi';
import { transactionJournalApi, type TransactionJournal } from '@/api/transactionJournalApi';
import { cashApi, type CashPosition } from '@/api/cashApi';
import { journalApi, type Journal } from '@/api/journalApi';
import { useBranchStore } from '@/stores/useBranchStore';

const { RangePicker } = DatePicker;
const { Text } = Typography;

interface SearchValues {
  termGroupId?: string;
  termId?: string;
  atmStatus?: string;
  errCd?: string;
  errorOnly?: boolean;
  range?: [Dayjs, Dayjs];
}

const ATM_STATUS_OPTIONS = [
  { value: '00', label: '정상' },
  { value: '01', label: '주의' },
  { value: '02', label: '장애' },
  { value: '09', label: '점검' },
];

const renderDeviceStatus = (v?: string) => {
  if (!v || v === '1') return <Tag color="green">정상</Tag>;
  if (v === '2') return <Tag color="orange">경고</Tag>;
  if (v === '3') return <Tag color="red">장애</Tag>;
  if (v === 'F') return <Tag color="default">미장착</Tag>;
  return <Tag>{v}</Tag>;
};

const renderAtmStatus = (v?: string) => {
  if (v === '00') return <Tag color="green">정상</Tag>;
  if (v === '01') return <Tag color="orange">주의</Tag>;
  if (v === '02') return <Tag color="red">장애</Tag>;
  if (v === '09') return <Tag color="blue">점검</Tag>;
  return <Tag>{v ?? '-'}</Tag>;
};

const fmtDT = (d?: string, t?: string) => {
  if (!d || d.length < 8) return '-';
  const ymd = `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}`;
  if (!t || t.length < 6) return ymd;
  return `${ymd} ${t.substring(0, 2)}:${t.substring(2, 4)}:${t.substring(4, 6)}`;
};

const fmtAmt = (v?: number) =>
  v != null && v !== 0 ? `${v.toLocaleString()}원` : '-';

/* ─────────────────────────── 장치별 상태 탭 ─────────────────────────── */
const DEVICE_FIELDS: { key: keyof AtmMonitor; label: string }[] = [
  { key: 'manStatus', label: '만원권' },
  { key: 'omanStatus', label: '오만원권' },
  { key: 'sprStatus', label: '명세표' },
  { key: 'jprStatus', label: '저널부' },
  { key: 'chkStatus', label: '수표부' },
  { key: 'mcrStatus', label: '카드부' },
  { key: 'pbStatus', label: '통장부' },
  { key: 'rjtStatus', label: '회수함' },
];

const DeviceStatusTab: React.FC<{ row: AtmMonitor }> = ({ row }) => (
  <div style={{ padding: '12px 4px' }}>
    <Descriptions bordered size="small" column={4} style={{ marginBottom: 16 }}>
      <Descriptions.Item label="ATM 상태">{renderAtmStatus(row.atmStatus)}</Descriptions.Item>
      <Descriptions.Item label="장애코드">
        {row.errCd ? <Tag color="red">{row.errCd}</Tag> : '-'}
      </Descriptions.Item>
      <Descriptions.Item label="기기장애코드">{row.atmErrCd ?? '-'}</Descriptions.Item>
      <Descriptions.Item label="처리건수">{row.processCount ?? '-'}</Descriptions.Item>
      <Descriptions.Item label="발생일시">{fmtDT(row.jnlDate, row.jnlTime)}</Descriptions.Item>
      <Descriptions.Item label="수신일시">{fmtDT(row.recvDate, row.recvTime)}</Descriptions.Item>
      <Descriptions.Item label="에이전트">{row.agentType ?? '-'}</Descriptions.Item>
      <Descriptions.Item label="관리자상태">{row.manStatus ?? '-'}</Descriptions.Item>
    </Descriptions>
    <Row gutter={[12, 12]}>
      {DEVICE_FIELDS.map(({ key, label }) => (
        <Col key={key} xs={12} sm={8} md={6} lg={4}>
          <Card
            size="small"
            bordered
            styles={{ body: { textAlign: 'center', padding: '10px 8px' } }}
          >
            <Text type="secondary" style={{ fontSize: 11 }}>{label}</Text>
            <div style={{ marginTop: 4 }}>{renderDeviceStatus(row[key] as string)}</div>
          </Card>
        </Col>
      ))}
    </Row>
  </div>
);

/* ─────────────────────────── 미복구장애내역 탭 ─────────────────────────── */
const FaultTab: React.FC<{ termGroupId: string; termId: string }> = ({ termGroupId, termId }) => {
  const [rows, setRows] = useState<AtmMonitor[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchRows = useCallback(async () => {
    setLoading(true);
    try {
      const r = await atmMonitorApi.getList({ termGroupId, termId, errorOnly: true, size: 50 });
      setRows(r.data.data.content);
    } catch {
      message.error('장애내역 조회 실패');
    } finally {
      setLoading(false);
    }
  }, [termGroupId, termId]);

  useEffect(() => { void fetchRows(); }, [fetchRows]);

  const cols: ColumnsType<AtmMonitor> = [
    { title: '발생일시', width: 160, render: (_, r) => fmtDT(r.jnlDate, r.jnlTime) },
    { title: 'ATM상태', dataIndex: 'atmStatus', width: 90, align: 'center', render: renderAtmStatus },
    { title: '장애코드', dataIndex: 'errCd', width: 120, render: (v) => v ? <Tag color="red">{v}</Tag> : '-' },
    { title: '기기장애코드', dataIndex: 'atmErrCd', width: 120 },
    { title: '수신일시', width: 160, render: (_, r) => fmtDT(r.recvDate, r.recvTime) },
  ];

  return (
    <Spin spinning={loading}>
      <Table<AtmMonitor>
        columns={cols}
        dataSource={rows}
        size="small"
        bordered
        rowKey={(r) => `${r.jnlDate}-${r.jnlTime}`}
        pagination={false}
        locale={{ emptyText: <Empty description="미복구 장애내역 없음" /> }}
        scroll={{ x: 700, y: 250 }}
      />
    </Spin>
  );
};

/* ─────────────────────────── 거래내역 탭 ─────────────────────────── */
const TR_TYPE_LABEL: Record<string, string> = {
  '000100': '입금',
  '000200': '출금',
  '000300': '계좌이체',
  '000400': '잔액조회',
  '000500': '통장정리',
};

const TxTab: React.FC<{ termGroupId: string; termId: string }> = ({ termGroupId, termId }) => {
  const [rows, setRows] = useState<TransactionJournal[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchRows = useCallback(async () => {
    setLoading(true);
    try {
      const r = await transactionJournalApi.getList({ termGroupId, termId, size: 50 });
      setRows(r.data.data.content);
    } catch {
      message.error('거래내역 조회 실패');
    } finally {
      setLoading(false);
    }
  }, [termGroupId, termId]);

  useEffect(() => { void fetchRows(); }, [fetchRows]);

  const cols: ColumnsType<TransactionJournal> = [
    { title: '거래일시', width: 160, render: (_, r) => fmtDT(r.jnlDate, r.jnlTime) },
    {
      title: '거래구분', dataIndex: 'trType', width: 100, align: 'center',
      render: (v) => <Tag color="blue">{TR_TYPE_LABEL[v] ?? v ?? '-'}</Tag>,
    },
    { title: '계좌', dataIndex: 'accountNo', width: 140 },
    { title: '요청금액', dataIndex: 'requestAmount', width: 110, align: 'right', render: fmtAmt },
    { title: '지급금액', dataIndex: 'dispenseAmount', width: 110, align: 'right', render: fmtAmt },
    { title: '잔여금액', dataIndex: 'remainAmount', width: 110, align: 'right', render: fmtAmt },
    {
      title: '결과', dataIndex: 'trResult', width: 80, align: 'center',
      render: (v) => v === '0000'
        ? <Tag color="green">정상</Tag>
        : <Tag color="red">{v ?? '-'}</Tag>,
    },
  ];

  return (
    <Spin spinning={loading}>
      <Table<TransactionJournal>
        columns={cols}
        dataSource={rows}
        size="small"
        bordered
        rowKey={(r) => `${r.jnlDate}-${r.jnlTime}`}
        pagination={false}
        locale={{ emptyText: <Empty description="거래내역 없음" /> }}
        scroll={{ x: 900, y: 250 }}
      />
    </Spin>
  );
};

/* ─────────────────────────── 카세트상태 탭 ─────────────────────────── */
const CassetteTab: React.FC<{ termGroupId: string; termId: string }> = ({ termGroupId, termId }) => {
  const [rows, setRows] = useState<CashPosition[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchRows = useCallback(async () => {
    setLoading(true);
    try {
      const r = await cashApi.getList({ termGroupId, termId, size: 50 });
      setRows(r.data.data.content);
    } catch {
      message.error('카세트 조회 실패');
    } finally {
      setLoading(false);
    }
  }, [termGroupId, termId]);

  useEffect(() => { void fetchRows(); }, [fetchRows]);

  const cols: ColumnsType<CashPosition> = [
    { title: '카세트', dataIndex: 'cassetteNo', width: 90, align: 'center' },
    { title: '종류', dataIndex: 'cassetteType', width: 90, align: 'center' },
    { title: '통화', dataIndex: 'currency', width: 70, align: 'center' },
    { title: '권종', dataIndex: 'denomination', width: 100, align: 'right',
      render: (v: number) => v ? `${v.toLocaleString()}원` : '-' },
    { title: '잔여수량', dataIndex: 'remainQty', width: 100, align: 'right',
      render: (v: number) => v != null ? `${v.toLocaleString()}매` : '-' },
    { title: '금액', dataIndex: 'amount', width: 120, align: 'right', render: fmtAmt },
    { title: '기준일', render: (_, r) => fmtDT(r.jnlDate, r.jnlTime) },
  ];

  return (
    <Spin spinning={loading}>
      <Table<CashPosition>
        columns={cols}
        dataSource={rows}
        size="small"
        bordered
        rowKey={(r, i) => `${r.cassetteNo ?? ''}-${i}`}
        pagination={false}
        locale={{ emptyText: <Empty description="카세트 정보 없음" /> }}
        scroll={{ x: 700, y: 250 }}
      />
    </Spin>
  );
};

/* ─────────────────────────── 저널 내역 공통 탭 ─────────────────────────── */
const JournalTab: React.FC<{ termGroupId: string; termId: string; jnlTypeCd: string; emptyText: string }> = ({
  termGroupId, termId, jnlTypeCd, emptyText,
}) => {
  const [rows, setRows] = useState<Journal[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchRows = useCallback(async () => {
    setLoading(true);
    try {
      const r = await journalApi.getList({ termGroupId, termId, jnlTypeCd, size: 50 });
      setRows(r.data.data.content);
    } catch {
      message.error('저널 조회 실패');
    } finally {
      setLoading(false);
    }
  }, [termGroupId, termId, jnlTypeCd]);

  useEffect(() => { void fetchRows(); }, [fetchRows]);

  const cols: ColumnsType<Journal> = [
    { title: '일시', width: 160, render: (_, r) => fmtDT(r.jnlDate, r.jnlTime) },
    { title: '저널종류', dataIndex: 'jnlTypeCd', width: 90, align: 'center' },
    { title: '저널구분', dataIndex: 'jnlWriteTypeCd', width: 90, align: 'center' },
    { title: 'SEQ', dataIndex: 'seqNo', width: 80, align: 'center' },
    { title: '수신일시', width: 160, render: (_, r) => fmtDT(r.recvDate, r.recvTime) },
  ];

  return (
    <Spin spinning={loading}>
      <Table<Journal>
        columns={cols}
        dataSource={rows}
        size="small"
        bordered
        rowKey={(r) => `${r.jnlDate}-${r.jnlTime}-${r.seqNo}`}
        pagination={false}
        locale={{ emptyText: <Empty description={emptyText} /> }}
        scroll={{ x: 600, y: 250 }}
      />
    </Spin>
  );
};

/* ═══════════════════════════ 메인 페이지 ═══════════════════════════ */
const AtmMonitorPage: React.FC = () => {
  const [form] = Form.useForm<SearchValues>();
  const [rows, setRows] = useState<AtmMonitor[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [selected, setSelected] = useState<AtmMonitor | null>(null);
  const [activeTab, setActiveTab] = useState('device');

  const branchLabel = useBranchStore((s) => s.label);
  const loadBranches = useBranchStore((s) => s.load);

  const fetchList = async (p = page, s = size) => {
    const v = form.getFieldsValue();
    setLoading(true);
    try {
      const res = await atmMonitorApi.getList({
        termGroupId: v.termGroupId,
        termId: v.termId,
        atmStatus: v.atmStatus,
        errCd: v.errCd,
        errorOnly: v.errorOnly,
        fromDate: v.range?.[0]?.format('YYYYMMDD'),
        toDate: v.range?.[1]?.format('YYYYMMDD'),
        page: p,
        size: s,
      });
      const data = res.data.data;
      setRows(data.content);
      setTotal(data.totalCount);
    } catch (err) {
      const e = err as { response?: { data?: { message?: string } } };
      message.error(e?.response?.data?.message ?? '조회 실패');
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
    setSelected(null);
    fetchList(1, size);
  };

  const handleTableChange = (pg: TablePaginationConfig) => {
    const newPage = pg.current ?? 1;
    const newSize = pg.pageSize ?? size;
    setPage(newPage);
    setSize(newSize);
    fetchList(newPage, newSize);
  };

  const columns: ColumnsType<AtmMonitor> = [
    {
      title: '영업점', dataIndex: 'termGroupId', width: 150, align: 'center',
      render: (v) => branchLabel(v),
    },
    { title: '단말', dataIndex: 'termId', width: 70, align: 'center' },
    { title: '발생일시', width: 155, render: (_, r) => fmtDT(r.jnlDate, r.jnlTime) },
    {
      title: 'ATM 상태', dataIndex: 'atmStatus', width: 85, align: 'center',
      render: renderAtmStatus,
    },
    {
      title: '장애코드', dataIndex: 'errCd', width: 110,
      render: (v, r) =>
        v ? (
          <Tooltip title={r.atmErrCd ? `기기코드: ${r.atmErrCd}` : ''}>
            <Tag color="red">{v}</Tag>
          </Tooltip>
        ) : '-',
    },
    { title: '만원권', dataIndex: 'manStatus', width: 70, align: 'center', render: renderDeviceStatus },
    { title: '오만원', dataIndex: 'omanStatus', width: 70, align: 'center', render: renderDeviceStatus },
    { title: '명세표', dataIndex: 'sprStatus', width: 70, align: 'center', render: renderDeviceStatus },
    { title: '저널부', dataIndex: 'jprStatus', width: 70, align: 'center', render: renderDeviceStatus },
    { title: '수표부', dataIndex: 'chkStatus', width: 70, align: 'center', render: renderDeviceStatus },
    { title: '카드부', dataIndex: 'mcrStatus', width: 70, align: 'center', render: renderDeviceStatus },
    { title: '통장부', dataIndex: 'pbStatus', width: 70, align: 'center', render: renderDeviceStatus },
    { title: '회수함', dataIndex: 'rjtStatus', width: 70, align: 'center', render: renderDeviceStatus },
    { title: '수신', width: 155, render: (_, r) => fmtDT(r.recvDate, r.recvTime) },
  ];

  const detailTabs = selected
    ? [
        {
          key: 'device',
          label: '장치별상태',
          children: <DeviceStatusTab row={selected} />,
        },
        {
          key: 'fault',
          label: '미복구장애내역',
          children: (
            <FaultTab
              key={`fault-${selected.termGroupId}-${selected.termId}`}
              termGroupId={selected.termGroupId}
              termId={selected.termId}
            />
          ),
        },
        {
          key: 'tx',
          label: '거래내역',
          children: (
            <TxTab
              key={`tx-${selected.termGroupId}-${selected.termId}`}
              termGroupId={selected.termGroupId}
              termId={selected.termId}
            />
          ),
        },
        {
          key: 'cassette',
          label: '카세트상태',
          children: (
            <CassetteTab
              key={`cash-${selected.termGroupId}-${selected.termId}`}
              termGroupId={selected.termGroupId}
              termId={selected.termId}
            />
          ),
        },
        {
          key: 'refill',
          label: '투입내역',
          children: (
            <JournalTab
              key={`jnl41-${selected.termGroupId}-${selected.termId}`}
              termGroupId={selected.termGroupId}
              termId={selected.termId}
              jnlTypeCd="41"
              emptyText="투입내역 없음"
            />
          ),
        },
        {
          key: 'total',
          label: '합계내역',
          children: (
            <JournalTab
              key={`jnl31-${selected.termGroupId}-${selected.termId}`}
              termGroupId={selected.termGroupId}
              termId={selected.termId}
              jnlTypeCd="31"
              emptyText="합계내역 없음"
            />
          ),
        },
      ]
    : [];

  return (
    <PageTemplate title="기기상태조회">
      {/* 검색 폼 */}
      <div className="search-panel" style={{ marginBottom: 8 }}>
        <Form form={form} layout="inline" onFinish={handleSearch} style={{ gap: 8, flexWrap: 'wrap' }}>
          <Form.Item name="termGroupId" label="영업점">
            <Input size="small" placeholder="영업점코드" style={{ width: 120 }} allowClear />
          </Form.Item>
          <Form.Item name="termId" label="단말">
            <Input size="small" placeholder="단말번호" style={{ width: 80 }} allowClear />
          </Form.Item>
          <Form.Item name="atmStatus" label="ATM 상태">
            <Select size="small" placeholder="전체" style={{ width: 100 }} allowClear options={ATM_STATUS_OPTIONS} />
          </Form.Item>
          <Form.Item name="errCd" label="장애코드">
            <Input size="small" placeholder="장애코드" style={{ width: 120 }} allowClear />
          </Form.Item>
          <Form.Item name="range" label="기간">
            <RangePicker size="small" />
          </Form.Item>
          <Form.Item name="errorOnly" valuePropName="checked">
            <Checkbox>장애만</Checkbox>
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" size="small" icon={<SearchOutlined />} htmlType="submit" loading={loading}>
                조회
              </Button>
              <Button
                size="small"
                icon={<ReloadOutlined />}
                onClick={() => {
                  form.resetFields();
                  form.setFieldsValue({ range: [dayjs().subtract(7, 'day'), dayjs()] });
                  setSelected(null);
                  setPage(1);
                  fetchList(1, size);
                }}
              >
                초기화
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </div>

      {/* 마스터 테이블 */}
      <Table<AtmMonitor>
        columns={columns}
        dataSource={rows}
        loading={loading}
        size="small"
        bordered
        rowKey={(r) => `${r.termGroupId}-${r.termId}-${r.jnlDate}-${r.jnlTime}`}
        scroll={{ x: 1400, y: selected ? 260 : 460 }}
        rowClassName={(r) =>
          selected &&
          r.termGroupId === selected.termGroupId &&
          r.termId === selected.termId &&
          r.jnlDate === selected.jnlDate &&
          r.jnlTime === selected.jnlTime
            ? 'ant-table-row-selected'
            : ''
        }
        onRow={(r) => ({
          onClick: () => {
            setSelected(r);
            setActiveTab('device');
          },
          style: { cursor: 'pointer' },
        })}
        pagination={{
          current: page,
          pageSize: size,
          total,
          showSizeChanger: true,
          pageSizeOptions: ['10', '20', '50', '100'],
          showTotal: (t, range) => `${range[0]}-${range[1]} / 총 ${t}건`,
        }}
        onChange={handleTableChange}
      />

      {/* 디테일 패널 */}
      {selected && (
        <Card
          size="small"
          style={{ marginTop: 8 }}
          styles={{ body: { padding: '0 8px 8px' } }}
          title={
            <Space>
              <VerticalAlignBottomOutlined />
              <Text strong>
                {branchLabel(selected.termGroupId)} &nbsp;|&nbsp; 단말 {selected.termId} &nbsp;|&nbsp;
                {fmtDT(selected.jnlDate, selected.jnlTime)}
              </Text>
              <Tag color={selected.errCd ? 'red' : 'green'}>
                {selected.errCd ? `장애: ${selected.errCd}` : '정상'}
              </Tag>
            </Space>
          }
          extra={
            <Button size="small" onClick={() => setSelected(null)}>
              닫기
            </Button>
          }
        >
          <Tabs
            activeKey={activeTab}
            onChange={setActiveTab}
            size="small"
            items={detailTabs}
          />
        </Card>
      )}
    </PageTemplate>
  );
};

export default AtmMonitorPage;
