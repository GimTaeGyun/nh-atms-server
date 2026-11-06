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
import PageTemplate from '@/components/PageTemplate';
import {
  operationJournalApi,
  type OperationJournal,
  type OperationJournalDetail,
} from '@/api/operationJournalApi';
import { useBranchStore } from '@/stores/useBranchStore';

const { RangePicker } = DatePicker;

const OP_CD_OPTIONS = [
  { value: '11', label: '11 - AP시작' },
  { value: '12', label: '12 - AP종료' },
  { value: '21', label: '21 - POWER ON' },
  { value: '22', label: '22 - POWER OFF' },
  { value: '31', label: '31 - 서비스상태전환' },
  { value: '99', label: '99 - 기타' },
];

const OP_CD_LABEL: Record<string, { label: string; color: string }> = {
  '11': { label: 'AP시작', color: 'green' },
  '12': { label: 'AP종료', color: 'orange' },
  '21': { label: 'POWER ON', color: 'green' },
  '22': { label: 'POWER OFF', color: 'red' },
  '31': { label: '서비스상태전환', color: 'blue' },
  '99': { label: '기타', color: 'default' },
};

const renderOpCd = (v?: string) => {
  if (!v) return '-';
  const def = OP_CD_LABEL[v];
  return def ? <Tag color={def.color}>{def.label}</Tag> : <Tag>{v}</Tag>;
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
  operationCd?: string;
  keyword?: string;
  range?: [Dayjs, Dayjs];
}

const OperationJournalPage: React.FC = () => {
  const [form] = Form.useForm<SearchValues>();
  const [data, setData] = useState<OperationJournal[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState<TablePaginationConfig>({
    current: 1,
    pageSize: 20,
    total: 0,
    showSizeChanger: true,
    showTotal: (total) => `총 ${total}건`,
  });
  const [detail, setDetail] = useState<OperationJournalDetail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const branchLabel = useBranchStore((s) => s.label);
  const loadBranches = useBranchStore((s) => s.load);

  const load = async (page = 1, size = 20, values?: SearchValues) => {
    setLoading(true);
    try {
      const v = values ?? form.getFieldsValue();
      const res = await operationJournalApi.getList({
        termGroupId: v.termGroupId || undefined,
        termId: v.termId || undefined,
        operationCd: v.operationCd || undefined,
        keyword: v.keyword || undefined,
        fromDate: v.range?.[0]?.format('YYYYMMDD'),
        toDate: v.range?.[1]?.format('YYYYMMDD'),
        page,
        size,
      });
      const p = res.data.data;
      setData(p.content);
      setPagination((prev) => ({ ...prev, current: page, pageSize: size, total: p.totalCount }));
    } catch (e) {
      message.error('운영저널 조회 실패');
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

  const openDetail = async (r: OperationJournal) => {
    setDetailOpen(true);
    setDetail(null);
    setDetailLoading(true);
    try {
      const res = await operationJournalApi.getDetail(
        r.termGroupId,
        r.termId,
        r.jnlDate,
        r.jnlTime,
      );
      setDetail(res.data.data);
    } catch (e) {
      message.error('상세 조회 실패');
    } finally {
      setDetailLoading(false);
    }
  };

  const columns: ColumnsType<OperationJournal> = [
    { title: '영업점', dataIndex: 'termGroupId', width: 140, render: (v) => branchLabel(v) },
    { title: '단말', dataIndex: 'termId', width: 90 },
    {
      title: '발생일시',
      width: 180,
      render: (_, r) => fmtDateTime(r.jnlDate, r.jnlTime),
    },
    { title: '운영코드', dataIndex: 'operationCd', width: 160, render: renderOpCd },
    { title: '처리내용', dataIndex: 'operationAction', ellipsis: true },
  ];

  return (
    <PageTemplate title="운영저널">
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
        <Form.Item name="operationCd" label="운영코드">
          <Select
            options={OP_CD_OPTIONS}
            allowClear
            placeholder="전체"
            style={{ width: 180 }}
          />
        </Form.Item>
        <Form.Item name="keyword" label="키워드">
          <Input placeholder="처리내용" allowClear style={{ width: 160 }} />
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

      <Table<OperationJournal>
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
        title="운영저널 상세"
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
              label: '운영정보',
              children: detail ? (
                <Descriptions bordered column={2} size="small">
                  <Descriptions.Item label="영업점">{branchLabel(detail.journal.termGroupId)}</Descriptions.Item>
                  <Descriptions.Item label="단말">{detail.journal.termId}</Descriptions.Item>
                  <Descriptions.Item label="발생일시" span={2}>
                    {fmtDateTime(detail.journal.jnlDate, detail.journal.jnlTime)}
                  </Descriptions.Item>
                  <Descriptions.Item label="운영코드">
                    {renderOpCd(detail.journal.operationCd)}
                  </Descriptions.Item>
                  <Descriptions.Item label="처리내용">
                    {detail.journal.operationAction || '-'}
                  </Descriptions.Item>
                </Descriptions>
              ) : (
                <div style={{ minHeight: 200 }}>{detailLoading ? '로딩 중...' : '-'}</div>
              ),
            },
            {
              key: 'raw',
              label: '원본 메시지',
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

export default OperationJournalPage;
