import React, { useEffect, useState } from 'react';
import {
  Button,
  Form,
  Input,
  Modal,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import {
  SearchOutlined,
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ReloadOutlined,
  HistoryOutlined,
  QuestionCircleOutlined,
  FileExcelOutlined,
} from '@ant-design/icons';
import * as XLSX from 'xlsx';
import PageTemplate from '@/components/PageTemplate';
import {
  batchJobApi,
  type BatchJob,
  type BatchJobSaveRequest,
  type BatchHistory,
} from '@/api/batchJobApi';

const { Text, Paragraph } = Typography;

// ── 표시 포맷 헬퍼 ───────────────────────────────────────────────
const fmtDate = (v?: string) =>
  v && v.length === 8 ? `${v.slice(0, 4)}-${v.slice(4, 6)}-${v.slice(6, 8)}` : '-';
const fmtTime = (v?: string) =>
  v && v.length === 6 ? `${v.slice(0, 2)}:${v.slice(2, 4)}:${v.slice(4, 6)}` : '-';

const renderJobStatus = (v?: string) =>
  v === 'RUNNING' ? <Tag color="processing">실행중</Tag> : <Tag>정지중</Tag>;

const renderUseFlag = (v?: string) =>
  v === 'Y' ? <Tag color="green">사용</Tag> : <Tag color="default">미사용</Tag>;

const renderHistStatus = (v?: string) => {
  if (v === 'SUCCESS') return <Tag color="green">성공</Tag>;
  if (v === 'FAIL') return <Tag color="red">실패</Tag>;
  if (v === 'RUNNING') return <Tag color="processing">실행중</Tag>;
  return <Tag>{v ?? '-'}</Tag>;
};

const BatchJobPage: React.FC = () => {
  const [searchForm] = Form.useForm();
  const [editForm] = Form.useForm<BatchJobSaveRequest>();

  const [rows, setRows] = useState<BatchJob[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [selected, setSelected] = useState<BatchJob | null>(null);

  // 등록/수정 모달
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<BatchJob | null>(null);
  const [cronCheck, setCronCheck] = useState<{ valid: boolean; next: string } | null>(null);

  // 실행이력 모달
  const [histOpen, setHistOpen] = useState(false);
  const [histProc, setHistProc] = useState('');
  const [histRows, setHistRows] = useState<BatchHistory[]>([]);
  const [histTotal, setHistTotal] = useState(0);
  const [histPage, setHistPage] = useState(1);
  const [histLoading, setHistLoading] = useState(false);

  // Cron 설명 모달
  const [cronHelpOpen, setCronHelpOpen] = useState(false);

  const fetchList = async (p = page, s = size) => {
    const v = searchForm.getFieldsValue();
    setLoading(true);
    try {
      const res = await batchJobApi.getList({ ...v, page: p, size: s });
      const data = res.data.data;
      setRows(data.content);
      setTotal(data.totalCount);
      setSelected(null);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '조회 실패');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchList(1, size);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ── 등록/수정 ──────────────────────────────────────────────────
  const openCreate = () => {
    setEditing(null);
    setCronCheck(null);
    editForm.resetFields();
    editForm.setFieldsValue({ useFlag: 'N', sortOrder: 0, cronExpr: '0 0 1 * * ?' });
    setModalOpen(true);
  };

  const openEdit = async (procName?: string) => {
    const target = procName ?? selected?.procName;
    if (!target) {
      message.warning('수정할 배치잡을 선택하세요.');
      return;
    }
    try {
      const res = await batchJobApi.getOne(target);
      setEditing(res.data.data);
      setCronCheck(null);
      editForm.setFieldsValue(res.data.data as BatchJobSaveRequest);
      setModalOpen(true);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '조회 실패');
    }
  };

  const checkCron = async () => {
    const expr = editForm.getFieldValue('cronExpr');
    if (!expr) {
      message.warning('Cron 표현식을 입력하세요.');
      return;
    }
    try {
      const res = await batchJobApi.validateCron(expr);
      setCronCheck(res.data.data);
      if (!res.data.data.valid) message.error('유효하지 않은 Cron 표현식입니다.');
    } catch {
      message.error('Cron 검증 실패');
    }
  };

  const handleSubmit = async () => {
    const values = await editForm.validateFields();
    try {
      if (editing?.procName) {
        await batchJobApi.update(editing.procName, values);
        message.success('수정되었습니다.');
      } else {
        await batchJobApi.create(values);
        message.success('등록되었습니다.');
      }
      setModalOpen(false);
      fetchList(page, size);
    } catch (e: any) {
      if (e?.errorFields) return;
      message.error(e?.response?.data?.message ?? '저장 실패');
    }
  };

  const handleDelete = async (procName?: string) => {
    const target = procName ?? selected?.procName;
    if (!target) {
      message.warning('삭제할 배치잡을 선택하세요.');
      return;
    }
    try {
      await batchJobApi.remove(target);
      message.success('삭제되었습니다.');
      fetchList(page, size);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '삭제 실패');
    }
  };

  // ── 재실행(수동 실행) ──────────────────────────────────────────
  const handleRun = async (procName: string) => {
    try {
      const res = await batchJobApi.run(procName);
      const h = res.data.data;
      message.success(
        `[${procName}] 실행 완료 — ${h.statusCd} (대상 ${h.targetCnt ?? 0} / 처리 ${h.procCnt ?? 0} / 에러 ${h.errCnt ?? 0})`,
      );
      fetchList(page, size);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '실행 실패');
    }
  };

  // ── 실행이력 ──────────────────────────────────────────────────
  const openHistory = async (procName?: string) => {
    const target = procName ?? selected?.procName;
    if (!target) {
      message.warning('이력을 조회할 배치잡을 선택하세요.');
      return;
    }
    setHistProc(target);
    setHistOpen(true);
    fetchHistory(target, 1);
  };

  const fetchHistory = async (procName: string, p: number) => {
    setHistLoading(true);
    try {
      const res = await batchJobApi.getHistory(procName, p, 20);
      const data = res.data.data;
      setHistRows(data.content);
      setHistTotal(data.totalCount);
      setHistPage(p);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '이력 조회 실패');
    } finally {
      setHistLoading(false);
    }
  };

  // ── 엑셀 내보내기 ─────────────────────────────────────────────
  const handleExcel = () => {
    if (rows.length === 0) {
      message.warning('내보낼 데이터가 없습니다.');
      return;
    }
    const data = rows.map((r, i) => ({
      번호: (page - 1) * size + i + 1,
      프로세스명: r.procName,
      작업설명: r.jobDesc ?? '',
      'Cron Expression': r.cronExpr,
      호스트: r.hostName ?? '',
      작업상태: r.jobStatus === 'RUNNING' ? '실행중' : '정지중',
      시작일: fmtDate(r.lastStartDate),
      시작시간: fmtTime(r.lastStartTime),
      종료일: fmtDate(r.lastEndDate),
      종료시간: fmtTime(r.lastEndTime),
      사용여부: r.useFlag === 'Y' ? '사용' : '미사용',
      정렬: r.sortOrder ?? 0,
    }));
    const ws = XLSX.utils.json_to_sheet(data);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, '배치작업관리');
    XLSX.writeFile(wb, `배치작업관리_${new Date().toISOString().slice(0, 10)}.xlsx`);
  };

  const columns: ColumnsType<BatchJob> = [
    {
      title: '번호',
      width: 60,
      align: 'center',
      fixed: 'left',
      render: (_, __, idx) => (page - 1) * size + idx + 1,
    },
    {
      title: '재실행',
      width: 80,
      align: 'center',
      fixed: 'left',
      render: (_, r) => (
        <Popconfirm
          title={`[${r.procName}] 지금 실행하시겠습니까?`}
          okText="실행"
          cancelText="취소"
          onConfirm={() => handleRun(r.procName)}
        >
          <Button size="small" icon={<ReloadOutlined />} onClick={(e) => e.stopPropagation()}>
            재실행
          </Button>
        </Popconfirm>
      ),
    },
    { title: '프로세스명', dataIndex: 'procName', width: 130 },
    { title: '작업설명', dataIndex: 'jobDesc', width: 280, render: (v) => v || '-' },
    { title: 'Cron Expression', dataIndex: 'cronExpr', width: 150, align: 'center' },
    { title: '호스트', dataIndex: 'hostName', width: 130, align: 'center', render: (v) => v || '전체' },
    { title: '작업상태', dataIndex: 'jobStatus', width: 90, align: 'center', render: renderJobStatus },
    { title: '시작일', width: 110, align: 'center', render: (_, r) => fmtDate(r.lastStartDate) },
    { title: '시작시간', width: 90, align: 'center', render: (_, r) => fmtTime(r.lastStartTime) },
    { title: '종료일', width: 110, align: 'center', render: (_, r) => fmtDate(r.lastEndDate) },
    { title: '종료시간', width: 90, align: 'center', render: (_, r) => fmtTime(r.lastEndTime) },
    { title: '사용여부', dataIndex: 'useFlag', width: 90, align: 'center', render: renderUseFlag },
    { title: '정렬', dataIndex: 'sortOrder', width: 70, align: 'center' },
  ];

  const histColumns: ColumnsType<BatchHistory> = [
    { title: '이력ID', dataIndex: 'histId', width: 80, align: 'center' },
    { title: '실행유형', dataIndex: 'runType', width: 90, align: 'center' },
    { title: '결과', dataIndex: 'statusCd', width: 80, align: 'center', render: renderHistStatus },
    { title: '시작', width: 170, align: 'center', render: (_, r) => `${fmtDate(r.startDate)} ${fmtTime(r.startTime)}` },
    { title: '종료', width: 170, align: 'center', render: (_, r) => `${fmtDate(r.endDate)} ${fmtTime(r.endTime)}` },
    { title: '소요(ms)', dataIndex: 'delayTime', width: 90, align: 'right', render: (v) => v ?? '-' },
    { title: '대상', dataIndex: 'targetCnt', width: 80, align: 'right', render: (v) => v ?? 0 },
    { title: '처리', dataIndex: 'procCnt', width: 80, align: 'right', render: (v) => v ?? 0 },
    { title: '에러', dataIndex: 'errCnt', width: 80, align: 'right', render: (v) => v ?? 0 },
    { title: '호스트', dataIndex: 'hostName', width: 130, align: 'center', render: (v) => v || '-' },
    { title: '에러메시지', dataIndex: 'errMsg', width: 240, render: (v) => v || '-' },
  ];

  return (
    <PageTemplate title="배치작업관리">
      <div className="search-panel" style={{ marginBottom: 8 }}>
        <Form
          form={searchForm}
          layout="inline"
          onFinish={() => { setPage(1); fetchList(1, size); }}
          style={{ gap: 8, flexWrap: 'wrap' }}
        >
          <Form.Item name="procName" label="프로세스명">
            <Input size="small" placeholder="프로세스명" style={{ width: 140 }} allowClear />
          </Form.Item>
          <Form.Item name="jobDesc" label="작업설명">
            <Input size="small" placeholder="설명 검색" style={{ width: 160 }} allowClear />
          </Form.Item>
          <Form.Item name="hostName" label="호스트">
            <Input size="small" placeholder="호스트" style={{ width: 130 }} allowClear />
          </Form.Item>
          <Form.Item name="useFlag" label="사용여부">
            <Select
              size="small"
              placeholder="전체"
              style={{ width: 100 }}
              allowClear
              options={[
                { value: 'Y', label: '사용' },
                { value: 'N', label: '미사용' },
              ]}
            />
          </Form.Item>
          <Form.Item>
            <Space wrap>
              <Button type="primary" size="small" htmlType="submit" icon={<SearchOutlined />} loading={loading}>
                조회(F2)
              </Button>
              <Button size="small" icon={<HistoryOutlined />} onClick={() => openHistory()}>
                실행이력
              </Button>
              <Button size="small" icon={<EditOutlined />} onClick={() => openEdit()}>
                수정
              </Button>
              <Popconfirm
                title="선택한 배치잡을 삭제하시겠습니까?"
                okText="삭제"
                cancelText="취소"
                onConfirm={() => handleDelete()}
              >
                <Button size="small" danger icon={<DeleteOutlined />}>
                  삭제
                </Button>
              </Popconfirm>
              <Button size="small" icon={<QuestionCircleOutlined />} onClick={() => setCronHelpOpen(true)}>
                Cron 설명
              </Button>
              <Button type="primary" size="small" icon={<PlusOutlined />} onClick={openCreate}>
                신규등록
              </Button>
              <Button size="small" icon={<FileExcelOutlined />} onClick={handleExcel}>
                엑셀
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </div>

      <Table<BatchJob>
        columns={columns}
        dataSource={rows}
        loading={loading}
        size="small"
        bordered
        rowKey="procName"
        scroll={{ x: 1700 }}
        rowSelection={{
          type: 'radio',
          selectedRowKeys: selected ? [selected.procName] : [],
          onChange: (_, selectedRows) => setSelected(selectedRows[0] ?? null),
        }}
        onRow={(r) => ({
          onClick: () => setSelected(r),
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
        onChange={(pg: TablePaginationConfig) => {
          const np = pg.current ?? 1;
          const ns = pg.pageSize ?? size;
          setPage(np);
          setSize(ns);
          fetchList(np, ns);
        }}
      />

      {/* 등록/수정 모달 */}
      <Modal
        title={editing ? `배치작업 수정 (${editing.procName})` : '배치작업 신규등록'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={handleSubmit}
        okText="저장"
        cancelText="취소"
        width={560}
        destroyOnHidden
      >
        <Form form={editForm} layout="vertical" size="small">
          <Form.Item
            name="procName"
            label="프로세스명"
            rules={[{ required: true, message: '필수' }]}
          >
            <Input maxLength={30} disabled={!!editing} placeholder="예: batday01" />
          </Form.Item>
          <Form.Item name="jobDesc" label="작업설명">
            <Input maxLength={200} placeholder="작업 설명" />
          </Form.Item>
          <Form.Item
            name="cronExpr"
            label="Cron Expression (초 분 시 일 월 요일)"
            rules={[{ required: true, message: '필수' }]}
            extra={
              cronCheck
                ? cronCheck.valid
                  ? <Text type="success">유효함 · 다음 실행: {cronCheck.next || '-'}</Text>
                  : <Text type="danger">유효하지 않은 표현식</Text>
                : undefined
            }
          >
            <Input
              maxLength={64}
              placeholder="0 0 1 * * ?"
              addonAfter={<a onClick={checkCron}>검증</a>}
            />
          </Form.Item>
          <Form.Item name="hostName" label="호스트 (비우면 모든 호스트에서 실행)">
            <Input maxLength={50} placeholder="예: atmap01t" />
          </Form.Item>
          <Space style={{ width: '100%' }} size="middle">
            <Form.Item name="useFlag" label="사용여부" style={{ flex: 1 }}>
              <Select
                options={[
                  { value: 'Y', label: '사용' },
                  { value: 'N', label: '미사용' },
                ]}
              />
            </Form.Item>
            <Form.Item name="sortOrder" label="정렬순서" style={{ flex: 1 }}>
              <Input type="number" />
            </Form.Item>
          </Space>
        </Form>
      </Modal>

      {/* 실행이력 모달 */}
      <Modal
        title={`실행이력 — ${histProc}`}
        open={histOpen}
        onCancel={() => setHistOpen(false)}
        footer={null}
        width={1100}
        destroyOnHidden
      >
        <Table<BatchHistory>
          columns={histColumns}
          dataSource={histRows}
          loading={histLoading}
          size="small"
          bordered
          rowKey="histId"
          scroll={{ x: 1400 }}
          pagination={{
            current: histPage,
            pageSize: 20,
            total: histTotal,
            showTotal: (t) => `총 ${t}건`,
            onChange: (p) => fetchHistory(histProc, p),
          }}
        />
      </Modal>

      {/* Cron 설명 모달 */}
      <Modal
        title="Cron Expression 설명"
        open={cronHelpOpen}
        onCancel={() => setCronHelpOpen(false)}
        footer={<Button onClick={() => setCronHelpOpen(false)}>닫기</Button>}
        width={560}
      >
        <Paragraph>
          Spring Cron 은 <Text code>초 분 시 일 월 요일</Text> 6개 필드로 구성됩니다.
        </Paragraph>
        <Paragraph>
          <ul style={{ margin: 0, paddingLeft: 18 }}>
            <li><Text code>*</Text> : 모든 값</li>
            <li><Text code>?</Text> : 일/요일 중 미지정</li>
            <li><Text code>0 0 1 * * ?</Text> : 매일 01:00:00</li>
            <li><Text code>0 5 1 * * ?</Text> : 매일 01:05:00</li>
            <li><Text code>0 0 2 * * MON</Text> : 매주 월요일 02:00:00</li>
            <li><Text code>0 0/10 * * * ?</Text> : 10분마다</li>
            <li><Text code>0 0 0 1 * ?</Text> : 매월 1일 00:00:00</li>
          </ul>
        </Paragraph>
      </Modal>
    </PageTemplate>
  );
};

export default BatchJobPage;
