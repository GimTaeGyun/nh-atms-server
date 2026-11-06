import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
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
  Tag,
  message,
} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import PageTemplate from '@/components/PageTemplate';
import { atmMonitorApi, type AtmMonitor } from '@/api/atmMonitorApi';
import { useBranchStore } from '@/stores/useBranchStore';

/**
 * 장애내역조회 (/atm/fault)
 *  - 데이터: T_ATMMONITOR (jnl_kind=22 장애 저널 누적)
 *  - 기본 필터: 최근 7일 + errorOnly=true (에러코드가 있는 행만)
 *  - 상단 KPI: 전체 장애 / 오늘 발생 / 영향 단말수 / 상위 장애코드
 */

const { RangePicker } = DatePicker;

interface SearchValues {
  termGroupId?: string;
  termId?: string;
  atmStatus?: string;
  errCd?: string;
  range?: [Dayjs, Dayjs];
}

const ATM_STATUS_OPTIONS = [
  { value: '00', label: '정상' },
  { value: '01', label: '주의' },
  { value: '02', label: '장애' },
  { value: '09', label: '점검' },
];

const renderAtmStatus = (v?: string) => {
  if (v === '00') return <Tag color="green">정상</Tag>;
  if (v === '01') return <Tag color="orange">주의</Tag>;
  if (v === '02') return <Tag color="red">장애</Tag>;
  if (v === '09') return <Tag color="blue">점검</Tag>;
  return <Tag>{v ?? '-'}</Tag>;
};

const renderDevStatus = (v?: string) => {
  if (!v || v === '1') return <Tag color="green">정상</Tag>;
  if (v === '2') return <Tag color="orange">경고</Tag>;
  if (v === '3') return <Tag color="red">장애</Tag>;
  if (v === 'F') return <Tag>미장착</Tag>;
  return <Tag>{v}</Tag>;
};

const fmtDateTime = (d?: string, t?: string) => {
  if (!d || d.length < 8) return '-';
  const ymd = `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}`;
  if (!t || t.length < 6) return ymd;
  return `${ymd} ${t.substring(0, 2)}:${t.substring(2, 4)}:${t.substring(4, 6)}`;
};

const FaultHistoryPage: React.FC = () => {
  const [form] = Form.useForm<SearchValues>();
  const [rows, setRows] = useState<AtmMonitor[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [detailOpen, setDetailOpen] = useState(false);
  const [detail, setDetail] = useState<AtmMonitor | null>(null);
  const branchLabel = useBranchStore((s) => s.label);
  const loadBranches = useBranchStore((s) => s.load);

  const kpi = useMemo(() => {
    const today = dayjs().format('YYYYMMDD');
    const todayCount = rows.filter((r) => r.jnlDate === today).length;
    const terminals = new Set(rows.map((r) => `${r.termGroupId}-${r.termId}`));
    const codeMap = new Map<string, number>();
    rows.forEach((r) => {
      const k = r.errCd ?? '(없음)';
      codeMap.set(k, (codeMap.get(k) ?? 0) + 1);
    });
    const topCode =
      [...codeMap.entries()].sort((a, b) => b[1] - a[1])[0] ?? (['-', 0] as [string, number]);
    return { todayCount, terminals: terminals.size, topCode };
  }, [rows]);

  const fetchList = async (p = page, s = size) => {
    const v = form.getFieldsValue();
    setLoading(true);
    try {
      const res = await atmMonitorApi.getList({
        termGroupId: v.termGroupId,
        termId: v.termId,
        atmStatus: v.atmStatus,
        errCd: v.errCd,
        errorOnly: true, // 장애내역조회는 항상 에러코드 있는 행만
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
    form.setFieldsValue({ range: [dayjs().subtract(7, 'day'), dayjs()] });
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

  const columns: ColumnsType<AtmMonitor> = [
    {
      title: '발생일시',
      width: 170,
      render: (_, r) => fmtDateTime(r.jnlDate, r.jnlTime),
    },
    { title: '영업점', dataIndex: 'termGroupId', width: 140, align: 'center', render: (v) => branchLabel(v) },
    { title: '단말', dataIndex: 'termId', width: 90, align: 'center' },
    { title: 'ATM 상태', dataIndex: 'atmStatus', width: 100, align: 'center', render: renderAtmStatus },
    {
      title: '은행 장애코드',
      dataIndex: 'errCd',
      width: 130,
      align: 'center',
      render: (v?: string) => (v ? <Tag color="red">{v}</Tag> : '-'),
    },
    {
      title: '기기 장애코드',
      dataIndex: 'atmErrCd',
      width: 130,
      align: 'center',
      render: (v?: string) => (v ? <Tag color="volcano">{v}</Tag> : '-'),
    },
    { title: '카드(MCR)', dataIndex: 'mcrStatus', width: 90, align: 'center', render: renderDevStatus },
    { title: '통장(PB)', dataIndex: 'pbStatus', width: 90, align: 'center', render: renderDevStatus },
    { title: '명세표(JPR)', dataIndex: 'jprStatus', width: 100, align: 'center', render: renderDevStatus },
    { title: '현금(CHA)', dataIndex: 'chaStatus', width: 90, align: 'center', render: renderDevStatus },
    { title: '수표(CHK)', dataIndex: 'chkStatus', width: 90, align: 'center', render: renderDevStatus },
    { title: '리젝트', dataIndex: 'rjtStatus', width: 80, align: 'center', render: renderDevStatus },
  ];

  return (
    <PageTemplate title="장애내역조회">
      <Row gutter={12} style={{ marginBottom: 12 }}>
        <Col span={6}>
          <Card size="small">
            <Statistic title="장애 건수" value={total} suffix="건" valueStyle={{ color: '#cf1322' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="오늘 발생" value={kpi.todayCount} suffix="건" />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="영향 단말 수 (현재 페이지)" value={kpi.terminals} suffix="대" />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title={`상위 장애코드: ${kpi.topCode[0]}`}
              value={kpi.topCode[1]}
              suffix="건"
            />
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
          <Form.Item name="atmStatus" label="ATM 상태">
            <Select size="small" placeholder="전체" style={{ width: 110 }} allowClear options={ATM_STATUS_OPTIONS} />
          </Form.Item>
          <Form.Item name="errCd" label="장애코드">
            <Input size="small" placeholder="ERR001" style={{ width: 130 }} allowClear />
          </Form.Item>
          <Form.Item name="range" label="기간">
            <RangePicker size="small" />
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

      <Table<AtmMonitor>
        columns={columns}
        dataSource={rows}
        loading={loading}
        size="small"
        bordered
        rowKey={(r) => `${r.termGroupId}-${r.termId}-${r.jnlDate}-${r.jnlTime}`}
        scroll={{ x: 1500 }}
        onRow={(r) => ({
          onClick: () => {
            setDetail(r);
            setDetailOpen(true);
          },
          style: { cursor: 'pointer' },
        })}
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
        title="장애 상세"
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        footer={null}
        width={820}
        destroyOnClose
      >
        {detail ? (
          <Descriptions size="small" bordered column={2}>
            <Descriptions.Item label="영업점/단말">
              {branchLabel(detail.termGroupId)} / {detail.termId}
            </Descriptions.Item>
            <Descriptions.Item label="발생일시">{fmtDateTime(detail.jnlDate, detail.jnlTime)}</Descriptions.Item>
            <Descriptions.Item label="ATM 상태">{renderAtmStatus(detail.atmStatus)}</Descriptions.Item>
            <Descriptions.Item label="에이전트">{detail.agentType ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="은행 장애코드">{detail.errCd ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="기기 장애코드">{detail.atmErrCd ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="처리횟수">{detail.processCount ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="송신 flag">{detail.sendFlag ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="카드(MCR)">{renderDevStatus(detail.mcrStatus)}</Descriptions.Item>
            <Descriptions.Item label="통장(PB)">{renderDevStatus(detail.pbStatus)}</Descriptions.Item>
            <Descriptions.Item label="명세표(JPR)">{renderDevStatus(detail.jprStatus)}</Descriptions.Item>
            <Descriptions.Item label="공급기(SPR)">{renderDevStatus(detail.sprStatus)}</Descriptions.Item>
            <Descriptions.Item label="현금(CHA)">{renderDevStatus(detail.chaStatus)}</Descriptions.Item>
            <Descriptions.Item label="수표(CHK)">{renderDevStatus(detail.chkStatus)}</Descriptions.Item>
            <Descriptions.Item label="리젝트(RJT)">{renderDevStatus(detail.rjtStatus)}</Descriptions.Item>
            <Descriptions.Item label="유지보수(MAN)">{renderDevStatus(detail.manStatus)}</Descriptions.Item>
            <Descriptions.Item label="수신일시" span={2}>
              {fmtDateTime(detail.recvDate, detail.recvTime)}
            </Descriptions.Item>
          </Descriptions>
        ) : (
          <Empty />
        )}
      </Modal>
    </PageTemplate>
  );
};

export default FaultHistoryPage;
