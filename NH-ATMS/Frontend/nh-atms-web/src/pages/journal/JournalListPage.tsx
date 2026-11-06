import React, { useEffect, useState } from 'react';
import {
  Button,
  DatePicker,
  Descriptions,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  message,
} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import { useNavigate } from 'react-router-dom';
import PageTemplate from '@/components/PageTemplate';
import { journalApi, type Journal, type JournalDetail } from '@/api/journalApi';
import { useBranchStore } from '@/stores/useBranchStore';

const { RangePicker } = DatePicker;

const JNL_KIND_OPTIONS = [
  { value: '11', label: '11 - AP시작' },
  { value: '12', label: '12 - 서비스' },
  { value: '21', label: '21 - ATM 상태' },
  { value: '22', label: '22 - ATM 장애' },
  { value: '31', label: '31 - 마감' },
  { value: '41', label: '41 - 시재' },
  { value: '52', label: '52 - 운영' },
  { value: '71', label: '71 - 거래' },
];

const JNL_KIND_LABEL: Record<string, { label: string; color: string }> = {
  '11': { label: 'AP시작', color: 'green' },
  '12': { label: '서비스', color: 'blue' },
  '21': { label: 'ATM 상태', color: 'cyan' },
  '22': { label: 'ATM 장애', color: 'red' },
  '31': { label: '마감', color: 'orange' },
  '41': { label: '시재', color: 'gold' },
  '52': { label: '운영', color: 'purple' },
  '71': { label: '거래', color: 'magenta' },
};

const renderJnlKind = (v?: string) => {
  if (!v) return '-';
  const def = JNL_KIND_LABEL[v];
  return def ? <Tag color={def.color}>{`${v} ${def.label}`}</Tag> : <Tag>{v}</Tag>;
};

const fmtDateTime = (d?: string, t?: string) => {
  if (!d || d.length < 8) return '-';
  const ymd = `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}`;
  if (!t || t.length < 6) return ymd;
  return `${ymd} ${t.substring(0, 2)}:${t.substring(2, 4)}:${t.substring(4, 6)}`;
};

interface SearchValues {
  termGroupId?: string;
  termId?: string;
  jnlTypeCd?: string;
  range?: [Dayjs, Dayjs];
}

const JournalListPage: React.FC = () => {
  const [form] = Form.useForm<SearchValues>();
  const navigate = useNavigate();
  const [data, setData] = useState<Journal[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState<TablePaginationConfig>({
    current: 1,
    pageSize: 20,
    total: 0,
    showSizeChanger: true,
    showTotal: (total) => `총 ${total}건`,
  });
  const [detail, setDetail] = useState<JournalDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const branchLabel = useBranchStore((s) => s.label);
  const loadBranches = useBranchStore((s) => s.load);

  const load = async (page = 1, size = 20, values?: SearchValues) => {
    setLoading(true);
    try {
      const v = values ?? form.getFieldsValue();
      const res = await journalApi.getList({
        termGroupId: v.termGroupId || undefined,
        termId: v.termId || undefined,
        jnlTypeCd: v.jnlTypeCd || undefined,
        fromDate: v.range?.[0]?.format('YYYYMMDD'),
        toDate: v.range?.[1]?.format('YYYYMMDD'),
        page,
        size,
      });
      const p = res.data.data;
      setData(p.content);
      setPagination((prev) => ({ ...prev, current: page, pageSize: size, total: p.totalCount }));
    } catch (e) {
      message.error('저널 조회 실패');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadBranches();
    form.setFieldsValue({ range: [dayjs().subtract(7, 'day'), dayjs()] });
    load(1, 20, { range: [dayjs().subtract(7, 'day'), dayjs()] });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const openDetail = async (r: Journal) => {
    setDetailOpen(true);
    setDetail(null);
    setDetailLoading(true);
    try {
      const res = await journalApi.getDetail(r.termGroupId, r.termId, r.jnlDate, r.jnlTime);
      setDetail(res.data.data);
    } catch (e) {
      message.error('상세 조회 실패');
    } finally {
      setDetailLoading(false);
    }
  };

  const gotoTyped = (j: Journal) => {
    // 전용 페이지가 있는 jnl_kind 는 해당 페이지로 이동
    if (j.jnlTypeCd === '71') {
      navigate('/journal/transaction');
    } else if (j.jnlTypeCd === '52') {
      navigate('/journal/operation');
    } else {
      message.info('이 jnl_kind 는 전용 화면이 없어 원본 본문만 제공합니다.');
    }
  };

  const columns: ColumnsType<Journal> = [
    { title: '영업점', dataIndex: 'termGroupId', width: 140, render: (v) => branchLabel(v) },
    { title: '단말', dataIndex: 'termId', width: 90 },
    {
      title: '발생일시',
      width: 180,
      render: (_, r) => fmtDateTime(r.jnlDate, r.jnlTime),
    },
    { title: '종류', dataIndex: 'jnlTypeCd', width: 140, render: renderJnlKind },
    { title: 'Agent', dataIndex: 'agentType', width: 80 },
    { title: 'Seq', dataIndex: 'seqNo', width: 90 },
    {
      title: '수신',
      width: 180,
      render: (_, r) => fmtDateTime(r.recvDate, r.recvTime),
    },
  ];

  return (
    <PageTemplate title="저널조회 (통합)">
      <Form
        form={form}
        layout="inline"
        onFinish={(v) => load(1, pagination.pageSize ?? 20, v)}
        style={{ marginBottom: 16, gap: 8, flexWrap: 'wrap' }}
      >
        <Form.Item name="range" label="기간">
          <RangePicker />
        </Form.Item>
        <Form.Item name="termGroupId" label="영업점">
          <Input placeholder="00107" allowClear style={{ width: 110 }} />
        </Form.Item>
        <Form.Item name="termId" label="단말">
          <Input placeholder="404" allowClear style={{ width: 100 }} />
        </Form.Item>
        <Form.Item name="jnlTypeCd" label="종류">
          <Select
            options={JNL_KIND_OPTIONS}
            allowClear
            placeholder="전체"
            style={{ width: 180 }}
          />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
              조회
            </Button>
            <Button
              icon={<ReloadOutlined />}
              onClick={() => {
                form.resetFields();
                form.setFieldsValue({ range: [dayjs().subtract(7, 'day'), dayjs()] });
                load(1, pagination.pageSize ?? 20, {
                  range: [dayjs().subtract(7, 'day'), dayjs()],
                });
              }}
            >
              초기화
            </Button>
          </Space>
        </Form.Item>
      </Form>

      <Table<Journal>
        rowKey={(r) => `${r.termGroupId}-${r.termId}-${r.jnlDate}-${r.jnlTime}`}
        columns={columns}
        dataSource={data}
        loading={loading}
        pagination={pagination}
        onChange={(p) => load(p.current ?? 1, p.pageSize ?? 20)}
        size="small"
        onRow={(r) => ({
          onClick: () => openDetail(r),
          style: { cursor: 'pointer' },
        })}
      />

      <Modal
        title="저널 상세 (공통 헤더 + 원본 본문)"
        open={detailOpen}
        onCancel={() => setDetailOpen(false)}
        footer={
          detail?.journal?.jnlTypeCd === '71' || detail?.journal?.jnlTypeCd === '52' ? (
            <Button type="primary" onClick={() => detail && gotoTyped(detail.journal)}>
              전용 화면 열기
            </Button>
          ) : null
        }
        width={900}
        destroyOnClose
      >
        <Tabs
          items={[
            {
              key: 'head',
              label: '헤더 (T_JOURNAL)',
              children: detail ? (
                <Descriptions bordered column={2} size="small">
                  <Descriptions.Item label="영업점">{branchLabel(detail.journal.termGroupId)}</Descriptions.Item>
                  <Descriptions.Item label="단말">{detail.journal.termId}</Descriptions.Item>
                  <Descriptions.Item label="발생일시" span={2}>
                    {fmtDateTime(detail.journal.jnlDate, detail.journal.jnlTime)}
                  </Descriptions.Item>
                  <Descriptions.Item label="종류">
                    {renderJnlKind(detail.journal.jnlTypeCd)}
                  </Descriptions.Item>
                  <Descriptions.Item label="Agent">
                    {detail.journal.agentType || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="Seq">{detail.journal.seqNo || '-'}</Descriptions.Item>
                  <Descriptions.Item label="인코딩">
                    {detail.journal.jnlWriteTypeCd || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="수신일시" span={2}>
                    {fmtDateTime(detail.journal.recvDate, detail.journal.recvTime)}
                  </Descriptions.Item>
                </Descriptions>
              ) : (
                <div style={{ minHeight: 200 }}>{detailLoading ? '로딩 중...' : '-'}</div>
              ),
            },
            {
              key: 'raw',
              label: '원본 본문 (T_JOURNALDATA)',
              children: (
                <pre
                  style={{
                    background: '#0f172a',
                    color: '#e2e8f0',
                    padding: 12,
                    borderRadius: 6,
                    maxHeight: 480,
                    overflow: 'auto',
                    fontFamily: 'D2Coding, Consolas, monospace',
                    fontSize: 12,
                    margin: 0,
                  }}
                >
                  {detail?.journalText || (detailLoading ? '로딩 중...' : '(원본 없음)')}
                </pre>
              ),
            },
          ]}
        />
      </Modal>
    </PageTemplate>
  );
};

export default JournalListPage;
