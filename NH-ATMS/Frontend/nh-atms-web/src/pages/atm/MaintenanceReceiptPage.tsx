import React, { useEffect, useMemo, useState } from 'react';
import {
  Button,
  Card,
  Col,
  DatePicker,
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
import {
  SearchOutlined,
  ReloadOutlined,
  AlertOutlined,
  MessageOutlined,
  StopOutlined,
} from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import PageTemplate from '@/components/PageTemplate';
import FaultReceiptModal, { type FaultTarget } from '@/components/FaultReceiptModal';
import SmsSendModal, { type SmsTarget } from '@/components/SmsSendModal';
import { faultMgmtApi, type FaultManagement } from '@/api/faultMgmtApi';
import { useBranchStore } from '@/stores/useBranchStore';

/**
 * 기기유지보수접수 (/atm/maintenance)
 *  - 데이터: T_FAULT_MANAGEMENT (출동요청/장애접수 이력)
 *  - 행 선택 후 [장애접수] → 기기장애접수 팝업
 *  - 행 선택 후 [SMS전송] → SMS 전송 팝업
 *  - 행 선택 후 [출동취소] → 접수 취소
 */

const { RangePicker } = DatePicker;

interface SearchValues {
  termGroupId?: string;
  termId?: string;
  faultStatus?: string;
  vendorCd?: string;
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

const MaintenanceReceiptPage: React.FC = () => {
  const [form] = Form.useForm<SearchValues>();
  const [rows, setRows] = useState<FaultManagement[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([]);
  const [selectedRow, setSelectedRow] = useState<FaultManagement | null>(null);
  const [faultOpen, setFaultOpen] = useState(false);
  const [smsOpen, setSmsOpen] = useState(false);
  const branchLabel = useBranchStore((s) => s.label);
  const loadBranches = useBranchStore((s) => s.load);

  const kpi = useMemo(() => {
    const req = rows.filter((r) => r.faultStatus === 'O').length;
    const prog = rows.filter((r) => r.faultStatus === 'P').length;
    const done = rows.filter((r) => r.faultStatus === 'C').length;
    const cancel = rows.filter((r) => r.faultStatus === 'X').length;
    return { req, prog, done, cancel };
  }, [rows]);

  const fetchList = async (p = page, s = size) => {
    const v = form.getFieldsValue();
    setLoading(true);
    try {
      const res = await faultMgmtApi.getList({
        termGroupId: v.termGroupId,
        termId: v.termId,
        faultStatus: v.faultStatus,
        vendorCd: v.vendorCd,
        fromDate: v.range?.[0]?.format('YYYYMMDD'),
        toDate: v.range?.[1]?.format('YYYYMMDD'),
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

  const requireSelection = (): boolean => {
    if (!selectedRow) {
      message.warning('대상을 목록에서 선택하세요.');
      return false;
    }
    return true;
  };

  const handleCancel = () => {
    if (!requireSelection() || !selectedRow) return;
    if (selectedRow.faultStatus !== 'O' && selectedRow.faultStatus !== 'P') {
      message.warning('출동요청/진행 상태만 취소할 수 있습니다.');
      return;
    }
    Modal.confirm({
      title: '출동취소',
      content: `선택한 접수(#${selectedRow.faultSeq})를 취소하시겠습니까?`,
      okText: '취소요청',
      cancelText: '닫기',
      okButtonProps: { danger: true },
      onOk: async () => {
        try {
          await faultMgmtApi.cancel(selectedRow.faultSeq);
          message.success('출동이 취소되었습니다.');
          fetchList(page, size);
        } catch (e: unknown) {
          const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message;
          message.error(msg ?? '출동취소 실패');
        }
      },
    });
  };

  const target: FaultTarget & SmsTarget | null = selectedRow
    ? { termGroupId: selectedRow.termGroupId, termId: selectedRow.termId }
    : null;

  const columns: ColumnsType<FaultManagement> = [
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
  ];

  return (
    <PageTemplate title="기기유지보수접수">
      <Row gutter={12} style={{ marginBottom: 12 }}>
        <Col span={6}>
          <Card size="small">
            <Statistic title="출동요청" value={kpi.req} suffix="건" valueStyle={{ color: '#d48806' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="진행" value={kpi.prog} suffix="건" valueStyle={{ color: '#1677ff' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="완료" value={kpi.done} suffix="건" valueStyle={{ color: '#3f8600' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="취소" value={kpi.cancel} suffix="건" />
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
          <Form.Item name="faultStatus" label="상태">
            <Select size="small" placeholder="전체" style={{ width: 110 }} allowClear options={STATUS_OPTIONS} />
          </Form.Item>
          <Form.Item name="range" label="출동요청일">
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

      <div style={{ marginBottom: 8 }}>
        <Space>
          <Button type="primary" danger icon={<AlertOutlined />} onClick={() => requireSelection() && setFaultOpen(true)}>
            장애접수
          </Button>
          <Button icon={<MessageOutlined />} onClick={() => requireSelection() && setSmsOpen(true)}>
            SMS전송
          </Button>
          <Button icon={<StopOutlined />} danger onClick={handleCancel}>
            출동취소
          </Button>
        </Space>
        {selectedRow && (
          <span style={{ marginLeft: 12, color: '#888' }}>
            선택: #{selectedRow.faultSeq} / {branchLabel(selectedRow.termGroupId)} / {selectedRow.termId}
          </span>
        )}
      </div>

      <Table<FaultManagement>
        columns={columns}
        dataSource={rows}
        loading={loading}
        size="small"
        bordered
        rowKey={(r) => String(r.faultSeq)}
        scroll={{ x: 1500 }}
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
            setSelectedKeys([String(r.faultSeq)]);
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
        target={target}
        onClose={() => setFaultOpen(false)}
        onSuccess={() => fetchList(page, size)}
      />
      <SmsSendModal open={smsOpen} target={target} onClose={() => setSmsOpen(false)} />
    </PageTemplate>
  );
};

export default MaintenanceReceiptPage;
