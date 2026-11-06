import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Checkbox,
  Col,
  DatePicker,
  Descriptions,
  Empty,
  Form,
  Input,
  Modal,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tabs,
  Tag,
  message,
} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import PageTemplate from '@/components/PageTemplate';
import JournalImageGallery from '@/components/JournalImageGallery';
import {
  transactionJournalApi,
  type TransactionJournal,
  type TransactionJournalDetail,
} from '@/api/transactionJournalApi';
import { useBranchStore } from '@/stores/useBranchStore';

/**
 * 거래내역조회 (/atm/journal)
 *  - 데이터 소스는 거래저널과 동일하나, 고객/계좌 관점의 UX
 *  - 상단 KPI: 총건수 / 성공률 / 총 요청금액 / 총 지급금액
 *  - 기본 필터: 최근 1일, 전체 결과
 */

const { RangePicker } = DatePicker;

interface SearchValues {
  termGroupId?: string;
  termId?: string;
  trType?: string;
  accountNo?: string;
  cardData?: string;
  trResult?: string;
  errorOnly?: boolean;
  range?: [Dayjs, Dayjs];
}

const TR_TYPE_OPTIONS = [
  { value: 'WD0001', label: '출금' },
  { value: 'DP0001', label: '입금' },
  { value: 'TR0001', label: '이체' },
  { value: 'BL0001', label: '잔액조회' },
  { value: 'WD0002', label: '출금(취소)' },
];

const TR_TYPE_LABEL: Record<string, { label: string; color: string }> = {
  WD0001: { label: '출금', color: 'red' },
  WD0002: { label: '출금취소', color: 'volcano' },
  DP0001: { label: '입금', color: 'green' },
  TR0001: { label: '이체', color: 'blue' },
  BL0001: { label: '잔액조회', color: 'default' },
};

const renderTrType = (v?: string) => {
  if (!v) return '-';
  const def = TR_TYPE_LABEL[v];
  return def ? <Tag color={def.color}>{def.label}</Tag> : <Tag>{v}</Tag>;
};

const renderTrResult = (v?: string) => {
  if (!v) return '-';
  if (v === '0000') return <Tag color="green">정상</Tag>;
  return <Tag color="red">실패 {v}</Tag>;
};

const fmtAmount = (v?: number) => (v == null ? '-' : Number(v).toLocaleString());

const fmtDateTime = (d?: string, t?: string) => {
  if (!d || d.length < 8) return '-';
  const ymd = `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}`;
  if (!t || t.length < 6) return ymd;
  return `${ymd} ${t.substring(0, 2)}:${t.substring(2, 4)}:${t.substring(4, 6)}`;
};

const TransactionHistoryPage: React.FC = () => {
  const [form] = Form.useForm<SearchValues>();
  const [rows, setRows] = useState<TransactionJournal[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [detail, setDetail] = useState<TransactionJournalDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const branchLabel = useBranchStore((s) => s.label);
  const loadBranches = useBranchStore((s) => s.load);

  const kpi = useMemo(() => {
    const success = rows.filter((r) => r.trResult === '0000').length;
    const reqSum = rows.reduce((a, r) => a + (Number(r.requestAmount) || 0), 0);
    const disSum = rows.reduce((a, r) => a + (Number(r.dispenseAmount) || 0), 0);
    const rate = rows.length === 0 ? 0 : Math.round((success / rows.length) * 1000) / 10;
    return { success, reqSum, disSum, rate };
  }, [rows]);

  const openDetail = async (r: TransactionJournal) => {
    setDetailOpen(true);
    setDetail(null);
    setDetailLoading(true);
    try {
      const res = await transactionJournalApi.getDetail(r.termGroupId, r.termId, r.jnlDate, r.jnlTime);
      setDetail(res.data.data);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '상세 조회 실패');
      setDetailOpen(false);
    } finally {
      setDetailLoading(false);
    }
  };

  const fetchList = async (p = page, s = size) => {
    const v = form.getFieldsValue();
    setLoading(true);
    try {
      const res = await transactionJournalApi.getList({
        termGroupId: v.termGroupId,
        termId: v.termId,
        trType: v.trType,
        accountNo: v.accountNo,
        cardData: v.cardData,
        trResult: v.trResult,
        errorOnly: v.errorOnly,
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
    form.setFieldsValue({ range: [dayjs().subtract(1, 'day'), dayjs()] });
    fetchList(1, size);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = () => {
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

  const columns: ColumnsType<TransactionJournal> = [
    {
      title: '발생일시',
      width: 170,
      render: (_, r) => fmtDateTime(r.jnlDate, r.jnlTime),
    },
    { title: '영업점', dataIndex: 'termGroupId', width: 140, align: 'center', render: (v) => branchLabel(v) },
    { title: '단말', dataIndex: 'termId', width: 70, align: 'center' },
    { title: '거래구분', dataIndex: 'trType', width: 100, align: 'center', render: renderTrType },
    { title: '거래번호', dataIndex: 'trNo', width: 80, align: 'center' },
    { title: '계좌번호', dataIndex: 'accountNo', width: 170 },
    { title: '카드', dataIndex: 'cardData', width: 170 },
    {
      title: '요청금액',
      dataIndex: 'requestAmount',
      width: 110,
      align: 'right',
      render: fmtAmount,
    },
    {
      title: '지급금액',
      dataIndex: 'dispenseAmount',
      width: 110,
      align: 'right',
      render: fmtAmount,
    },
    {
      title: '수수료',
      dataIndex: 'surcharge',
      width: 90,
      align: 'right',
      render: fmtAmount,
    },
    { title: '결과', dataIndex: 'trResult', width: 100, align: 'center', render: renderTrResult },
    { title: '메시지', dataIndex: 'message', width: 160 },
  ];

  return (
    <PageTemplate title="거래내역조회">
      <Row gutter={12} style={{ marginBottom: 12 }}>
        <Col span={6}>
          <Card size="small">
            <Statistic title="조회 건수" value={total} suffix="건" />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="성공률 (현재 페이지)"
              value={kpi.rate}
              suffix="%"
              valueStyle={{ color: kpi.rate >= 95 ? '#3f8600' : kpi.rate >= 80 ? '#d48806' : '#cf1322' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="요청금액 합계" value={kpi.reqSum} suffix="원" />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="지급금액 합계" value={kpi.disSum} suffix="원" />
          </Card>
        </Col>
      </Row>

      <div className="search-panel" style={{ marginBottom: 8 }}>
        <Form form={form} layout="inline" onFinish={handleSearch} style={{ gap: 8, flexWrap: 'wrap' }}>
          <Form.Item name="termGroupId" label="영업점">
            <Input size="small" placeholder="영업점코드" style={{ width: 110 }} allowClear />
          </Form.Item>
          <Form.Item name="termId" label="단말">
            <Input size="small" placeholder="단말번호" style={{ width: 100 }} allowClear />
          </Form.Item>
          <Form.Item name="trType" label="거래구분">
            <Select size="small" placeholder="전체" style={{ width: 110 }} allowClear options={TR_TYPE_OPTIONS} />
          </Form.Item>
          <Form.Item name="accountNo" label="계좌">
            <Input size="small" placeholder="계좌번호" style={{ width: 170 }} allowClear />
          </Form.Item>
          <Form.Item name="cardData" label="카드">
            <Input size="small" placeholder="카드번호" style={{ width: 170 }} allowClear />
          </Form.Item>
          <Form.Item name="range" label="기간">
            <RangePicker size="small" />
          </Form.Item>
          <Form.Item name="errorOnly" valuePropName="checked">
            <Checkbox>실패만</Checkbox>
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
                  form.setFieldsValue({ range: [dayjs().subtract(1, 'day'), dayjs()] });
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

      <Table<TransactionJournal>
        columns={columns}
        dataSource={rows}
        loading={loading}
        size="small"
        bordered
        rowKey={(r) => `${r.termGroupId}-${r.termId}-${r.jnlDate}-${r.jnlTime}`}
        scroll={{ x: 1600 }}
        onRow={(r) => ({ onClick: () => openDetail(r), style: { cursor: 'pointer' } })}
        pagination={{
          current: page,
          pageSize: size,
          total,
          showSizeChanger: true,
          pageSizeOptions: ['10', '20', '50', '100'],
          showTotal: (t, r) => `${r[0]}-${r[1]} / 총 ${t}건`,
        }}
        onChange={handleTableChange}
      />

      <Modal
        title="거래 상세"
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        footer={null}
        width={900}
        destroyOnClose
      >
        <Tabs
          items={[
            {
              key: 'info',
              label: '거래 정보',
              children: detail ? (
                <Descriptions size="small" bordered column={2}>
                  <Descriptions.Item label="영업점/단말">
                    {detail.journal.termGroupId} / {detail.journal.termId}
                  </Descriptions.Item>
                  <Descriptions.Item label="발생일시">
                    {fmtDateTime(detail.journal.jnlDate, detail.journal.jnlTime)}
                  </Descriptions.Item>
                  <Descriptions.Item label="거래구분">{renderTrType(detail.journal.trType)}</Descriptions.Item>
                  <Descriptions.Item label="결과">{renderTrResult(detail.journal.trResult)}</Descriptions.Item>
                  <Descriptions.Item label="거래번호">{detail.journal.trNo ?? '-'}</Descriptions.Item>
                  <Descriptions.Item label="은행">{detail.journal.bankCd ?? '-'}</Descriptions.Item>
                  <Descriptions.Item label="계좌번호" span={2}>
                    {detail.journal.accountNo ?? '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="요청금액">{fmtAmount(detail.journal.requestAmount)}</Descriptions.Item>
                  <Descriptions.Item label="지급금액">{fmtAmount(detail.journal.dispenseAmount)}</Descriptions.Item>
                  <Descriptions.Item label="잔액">{fmtAmount(detail.journal.remainAmount)}</Descriptions.Item>
                  <Descriptions.Item label="수수료">{fmtAmount(detail.journal.surcharge)}</Descriptions.Item>
                  <Descriptions.Item label="카드" span={2}>{detail.journal.cardData ?? '-'}</Descriptions.Item>
                  <Descriptions.Item label="메시지" span={2}>{detail.journal.message ?? '-'}</Descriptions.Item>
                </Descriptions>
              ) : (
                <Empty description={detailLoading ? '불러오는 중...' : '데이터 없음'} />
              ),
            },
            {
              key: 'text',
              label: '원본 출력문',
              children: detail?.journalText ? (
                <pre
                  style={{
                    background: '#0f172a',
                    color: '#e2e8f0',
                    padding: 12,
                    borderRadius: 4,
                    maxHeight: 480,
                    overflow: 'auto',
                    fontFamily: 'D2Coding, Consolas, monospace',
                    fontSize: 12,
                    margin: 0,
                  }}
                >
                  {detail.journalText}
                </pre>
              ) : (
                <Empty description="원본 저널이 없습니다 (T_JOURNALDATA)" />
              ),
            },
            {
              key: 'image',
              label: `이미지 (${detail?.images?.length ?? 0})`,
              children: <JournalImageGallery images={detail?.images ?? []} />,
            },
          ]}
        />
      </Modal>
    </PageTemplate>
  );
};

export default TransactionHistoryPage;
