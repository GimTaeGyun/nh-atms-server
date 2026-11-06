import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Col,
  Form,
  Input,
  Row,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  message,
} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { SearchOutlined, ReloadOutlined, AlertOutlined } from '@ant-design/icons';
import PageTemplate from '@/components/PageTemplate';
import FaultReceiptModal, { type FaultTarget } from '@/components/FaultReceiptModal';
import { atmMonitorApi, type AtmMonitor } from '@/api/atmMonitorApi';
import { useBranchStore } from '@/stores/useBranchStore';

/**
 * 실시간모니터링 (/atm/realtime-monitor)
 *  - 데이터: T_ATMMONITOR (실시간 기기 상태)
 *  - 행을 선택하고 [장애접수] 버튼 → 기기장애접수 팝업
 */

interface SearchValues {
  termGroupId?: string;
  termId?: string;
  atmStatus?: string;
  errorOnly?: boolean;
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

const rowKeyOf = (r: AtmMonitor) => `${r.termGroupId}-${r.termId}-${r.jnlDate}-${r.jnlTime}`;

const RealtimeMonitorPage: React.FC = () => {
  const [form] = Form.useForm<SearchValues>();
  const [rows, setRows] = useState<AtmMonitor[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([]);
  const [selectedRow, setSelectedRow] = useState<AtmMonitor | null>(null);
  const [faultOpen, setFaultOpen] = useState(false);
  const branchLabel = useBranchStore((s) => s.label);
  const loadBranches = useBranchStore((s) => s.load);

  const kpi = useMemo(() => {
    const normal = rows.filter((r) => !r.atmStatus || r.atmStatus === '00').length;
    const warn = rows.filter((r) => r.atmStatus === '01').length;
    const fault = rows.filter((r) => r.atmStatus === '02').length;
    const check = rows.filter((r) => r.atmStatus === '09').length;
    return { normal, warn, fault, check };
  }, [rows]);

  const fetchList = async (p = page, s = size) => {
    const v = form.getFieldsValue();
    setLoading(true);
    try {
      const res = await atmMonitorApi.getList({
        termGroupId: v.termGroupId,
        termId: v.termId,
        atmStatus: v.atmStatus,
        errorOnly: v.errorOnly,
        page: p,
        size: s,
      });
      const data = res.data.data;
      setRows(data.content);
      setTotal(data.totalCount);
      setSelectedKeys([]);
      setSelectedRow(null);
    } catch (e: unknown) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message;
      message.error(msg ?? '조회 실패');
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

  const handleTableChange = (pg: TablePaginationConfig) => {
    const newPage = pg.current ?? 1;
    const newSize = pg.pageSize ?? size;
    setPage(newPage);
    setSize(newSize);
    fetchList(newPage, newSize);
  };

  const openFault = () => {
    if (!selectedRow) {
      message.warning('장애접수할 기기를 목록에서 선택하세요.');
      return;
    }
    setFaultOpen(true);
  };

  const faultTarget: FaultTarget | null = selectedRow
    ? { termGroupId: selectedRow.termGroupId, termId: selectedRow.termId }
    : null;

  const columns: ColumnsType<AtmMonitor> = [
    { title: '영업점(금고)', dataIndex: 'termGroupId', width: 150, align: 'center', render: (v) => branchLabel(v) },
    { title: '기번(단말)', dataIndex: 'termId', width: 100, align: 'center' },
    { title: '상태', dataIndex: 'atmStatus', width: 90, align: 'center', render: renderAtmStatus },
    { title: '발생일시', width: 170, render: (_, r) => fmtDateTime(r.jnlDate, r.jnlTime) },
    {
      title: '은행 장애코드',
      dataIndex: 'errCd',
      width: 120,
      align: 'center',
      render: (v?: string) => (v ? <Tag color="red">{v}</Tag> : '-'),
    },
    {
      title: '기기 장애코드',
      dataIndex: 'atmErrCd',
      width: 120,
      align: 'center',
      render: (v?: string) => (v ? <Tag color="volcano">{v}</Tag> : '-'),
    },
    { title: '카드(MCR)', dataIndex: 'mcrStatus', width: 90, align: 'center', render: renderDevStatus },
    { title: '통장(PB)', dataIndex: 'pbStatus', width: 90, align: 'center', render: renderDevStatus },
    { title: '명세표(JPR)', dataIndex: 'jprStatus', width: 100, align: 'center', render: renderDevStatus },
    { title: '현금(CHA)', dataIndex: 'chaStatus', width: 90, align: 'center', render: renderDevStatus },
    { title: '수표(CHK)', dataIndex: 'chkStatus', width: 90, align: 'center', render: renderDevStatus },
  ];

  return (
    <PageTemplate title="실시간모니터링">
      <Row gutter={12} style={{ marginBottom: 12 }}>
        <Col span={6}>
          <Card size="small">
            <Statistic title="정상" value={kpi.normal} suffix="대" valueStyle={{ color: '#3f8600' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="주의" value={kpi.warn} suffix="대" valueStyle={{ color: '#d48806' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="장애" value={kpi.fault} suffix="대" valueStyle={{ color: '#cf1322' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="점검" value={kpi.check} suffix="대" valueStyle={{ color: '#1677ff' }} />
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
          <Form.Item name="atmStatus" label="상태">
            <Select size="small" placeholder="전체" style={{ width: 110 }} allowClear options={ATM_STATUS_OPTIONS} />
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

      <div style={{ marginBottom: 8 }}>
        <Button type="primary" danger icon={<AlertOutlined />} disabled={!selectedRow} onClick={openFault}>
          장애접수
        </Button>
        {selectedRow && (
          <span style={{ marginLeft: 12, color: '#888' }}>
            선택: {branchLabel(selectedRow.termGroupId)} / {selectedRow.termId}
          </span>
        )}
      </div>

      <Table<AtmMonitor>
        columns={columns}
        dataSource={rows}
        loading={loading}
        size="small"
        bordered
        rowKey={rowKeyOf}
        scroll={{ x: 1400 }}
        rowSelection={{
          type: 'radio',
          selectedRowKeys: selectedKeys,
          onChange: (keys, selectedRows) => {
            setSelectedKeys(keys);
            setSelectedRow(selectedRows[0] ?? null);
          },
        }}
        onRow={(r) => ({
          onClick: () => {
            setSelectedKeys([rowKeyOf(r)]);
            setSelectedRow(r);
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

      <FaultReceiptModal
        open={faultOpen}
        target={faultTarget}
        onClose={() => setFaultOpen(false)}
        onSuccess={() => fetchList(page, size)}
      />
    </PageTemplate>
  );
};

export default RealtimeMonitorPage;
