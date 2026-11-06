import React, { useEffect, useState } from 'react';
import {
  Button,
  Card,
  Checkbox,
  Form,
  Input,
  Radio,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Tooltip,
  message,
} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { ReloadOutlined, SearchOutlined, SendOutlined } from '@ant-design/icons';
import PageTemplate from '@/components/PageTemplate';
import {
  remoteControlApi,
  type RemoteCommand,
  type RemoteCommandIssueRequest,
  type RemoteCommandType,
} from '@/api/remoteControlApi';
import { useBranchStore } from '@/stores/useBranchStore';

const { TextArea } = Input;

const CMD_LABEL: Record<string, string> = {
  STAT: '기기상태조회',
  IMGC: '이미지/화면캡쳐',
  DLD: '다운로드',
  FCOL: '파일수집',
  RST: '장치리셋',
  RBT: '재시작/재부팅',
  POFF: '전원OFF',
  STOP: '중지/폐국',
  OPEN_OP: '개국',
  JMOD: '저널운영모드',
  MSG: '메시지전송',
  TMOD: '거래모드',
  CLS: '마감',
  RCMD: '원격CMD실행',
  GDL: '일반다운로드',
};

const STATUS_META: Record<string, { label: string; color: string }> = {
  W: { label: '대기', color: 'default' },
  A: { label: '수령', color: 'processing' },
  S: { label: '성공', color: 'success' },
  F: { label: '실패', color: 'error' },
  T: { label: '타임아웃', color: 'warning' },
  C: { label: '취소', color: 'default' },
};

const fmtTs = (v?: string) => (v ? v.replace('T', ' ').substring(0, 19) : '-');

const errMsgOf = (e: unknown, fallback: string) =>
  (e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? fallback;

interface SearchValues {
  termGroupId?: string;
  termId?: string;
}

const RemoteControlPage: React.FC = () => {
  const [searchForm] = Form.useForm<SearchValues>();
  const [rows, setRows] = useState<RemoteCommand[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [issuing, setIssuing] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const branchLabel = useBranchStore((s) => s.label);
  const loadBranches = useBranchStore((s) => s.load);

  // 탭별 입력 상태
  const [jmod, setJmod] = useState('E'); // E=전자, EP=전자+종이, P=종이
  const [tmodes, setTmodes] = useState<string[]>(['CASH_OUT', 'CASH_IN']);
  const [fileType, setFileType] = useState('ALL');
  const [msgText, setMsgText] = useState('');
  const [rcmdText, setRcmdText] = useState('');
  const [rcmdTimeout, setRcmdTimeout] = useState(60);

  const fetchList = async (p = page, s = size) => {
    const v = searchForm.getFieldsValue();
    setLoading(true);
    try {
      const res = await remoteControlApi.getList({
        termGroupId: v.termGroupId,
        termId: v.termId,
        page: p,
        size: s,
      });
      const data = res.data.data;
      setRows(data.content);
      setTotal(data.totalCount);
    } catch (e) {
      message.error(errMsgOf(e, '조회 실패'));
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

  const handleReset = () => {
    searchForm.resetFields();
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

  /** 대상 기기 검증 후 명령 발행 */
  const issue = async (
    cmdType: RemoteCommandType,
    payload?: Record<string, unknown>,
    timeoutSec?: number,
  ) => {
    const { termGroupId, termId } = searchForm.getFieldsValue();
    if (!termGroupId?.trim() || !termId?.trim()) {
      message.warning('영업점(금고)과 기번을 먼저 입력하세요.');
      return;
    }
    const body: RemoteCommandIssueRequest = {
      termGroupId: termGroupId.trim(),
      termId: termId.trim(),
      cmdType,
      payload,
      timeoutSec,
    };
    setIssuing(true);
    try {
      const res = await remoteControlApi.issue(body);
      message.success(`[${CMD_LABEL[cmdType]}] 명령 발행 완료 (${res.data.data.cmdId})`);
      setPage(1);
      fetchList(1, size);
    } catch (e) {
      message.error(errMsgOf(e, '명령 발행 실패'));
    } finally {
      setIssuing(false);
    }
  };

  const columns: ColumnsType<RemoteCommand> = [
    { title: '명령ID', dataIndex: 'cmdId', width: 170, align: 'center' },
    {
      title: '영업점',
      dataIndex: 'termGroupId',
      width: 150,
      align: 'center',
      render: (v) => branchLabel(v),
    },
    { title: '기번', dataIndex: 'termId', width: 90, align: 'center' },
    {
      title: '명령유형',
      dataIndex: 'cmdType',
      width: 130,
      align: 'center',
      render: (v: string) => CMD_LABEL[v] ?? v,
    },
    {
      title: '상태',
      dataIndex: 'status',
      width: 90,
      align: 'center',
      render: (v: string) => {
        const m = STATUS_META[v] ?? { label: v, color: 'default' };
        return <Tag color={m.color}>{m.label}</Tag>;
      },
    },
    {
      title: '결과',
      width: 220,
      ellipsis: true,
      render: (_, r) =>
        r.errMsg ? (
          <Tooltip title={r.errMsg}>
            <span style={{ color: '#cf1322' }}>{r.errMsg}</span>
          </Tooltip>
        ) : (
          r.resultData ?? '-'
        ),
    },
    { title: '발행자', dataIndex: 'regUserId', width: 100, align: 'center' },
    { title: '발행일시', width: 170, align: 'center', render: (_, r) => fmtTs(r.issuedAt) },
    { title: '완료일시', width: 170, align: 'center', render: (_, r) => fmtTs(r.completedAt) },
  ];

  const tabItems = [
    {
      key: 'remote',
      label: '원격제어',
      children: (
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <Card size="small" title="기기 / 시스템 제어">
            <Space wrap>
              <Button loading={issuing} onClick={() => issue('STAT')}>
                기기상태조회
              </Button>
              <Button loading={issuing} onClick={() => issue('RST')}>
                장치리셋
              </Button>
              <Button loading={issuing} onClick={() => issue('RBT')}>
                재시작/재부팅
              </Button>
              <Button danger loading={issuing} onClick={() => issue('POFF')}>
                전원OFF
              </Button>
              <Button loading={issuing} onClick={() => issue('OPEN_OP')}>
                개국
              </Button>
              <Button danger loading={issuing} onClick={() => issue('STOP')}>
                폐국
              </Button>
              <Button loading={issuing} onClick={() => issue('CLS')}>
                마감
              </Button>
            </Space>
          </Card>

          <Card size="small" title="저널운영모드 (JMOD)">
            <Space>
              <Radio.Group value={jmod} onChange={(e) => setJmod(e.target.value)}>
                <Radio value="E">전자저널</Radio>
                <Radio value="EP">전자+종이</Radio>
                <Radio value="P">종이저널</Radio>
              </Radio.Group>
              <Button
                type="primary"
                loading={issuing}
                onClick={() => issue('JMOD', { mode: jmod })}
              >
                적용
              </Button>
            </Space>
          </Card>

          <Card size="small" title="거래모드 (TMOD)">
            <Space>
              <Checkbox.Group
                value={tmodes}
                onChange={(v) => setTmodes(v as string[])}
                options={[
                  { label: '현금출금', value: 'CASH_OUT' },
                  { label: '현금입금', value: 'CASH_IN' },
                  { label: '오만원권', value: 'BILL_50K' },
                  { label: '수표', value: 'CHECK' },
                  { label: '통장', value: 'PASSBOOK' },
                ]}
              />
              <Button
                type="primary"
                loading={issuing}
                onClick={() => issue('TMOD', { modes: tmodes })}
              >
                적용
              </Button>
            </Space>
          </Card>
        </Space>
      ),
    },
    {
      key: 'file',
      label: '자동화기기 파일수집',
      children: (
        <Card size="small" title="파일수집 (FCOL)">
          <Space>
            <Select
              style={{ width: 220 }}
              value={fileType}
              onChange={setFileType}
              options={[
                { value: 'ALL', label: '전체' },
                { value: 'AP_LOG', label: 'AP Log' },
                { value: 'AGENT_LOG', label: 'Agent Log' },
                { value: 'AGENT_INI', label: 'eztams.agent.ini' },
              ]}
            />
            <Button
              type="primary"
              loading={issuing}
              onClick={() => issue('FCOL', { fileType })}
            >
              파일수집 요청
            </Button>
          </Space>
        </Card>
      ),
    },
    {
      key: 'message',
      label: '메시지전송',
      children: (
        <Card size="small" title="메시지전송 (MSG)">
          <Space direction="vertical" style={{ width: '100%' }}>
            <TextArea
              rows={5}
              maxLength={500}
              showCount
              value={msgText}
              onChange={(e) => setMsgText(e.target.value)}
              placeholder="자동화기기 화면에 표시할 메시지를 입력하세요."
            />
            <Button
              type="primary"
              icon={<SendOutlined />}
              loading={issuing}
              disabled={!msgText.trim()}
              onClick={() => issue('MSG', { message: msgText.trim() })}
            >
              전송
            </Button>
          </Space>
        </Card>
      ),
    },
    {
      key: 'customer-image',
      label: '고객이미지확인',
      children: (
        <Card size="small" title="고객이미지 확인 (IMGC)">
          <Space>
            <Button loading={issuing} onClick={() => issue('IMGC', { kind: 'customer', target: 'last' })}>
              마지막 거래 이미지 요청
            </Button>
            <Button loading={issuing} onClick={() => issue('IMGC', { kind: 'customer', target: 'live' })}>
              실시간 이미지 요청
            </Button>
          </Space>
        </Card>
      ),
    },
    {
      key: 'screen-capture',
      label: '자동기화면캡쳐',
      children: (
        <Card size="small" title="화면 캡쳐 (IMGC)">
          <Button
            type="primary"
            loading={issuing}
            onClick={() => issue('IMGC', { kind: 'screen' })}
          >
            화면캡쳐 요청
          </Button>
        </Card>
      ),
    },
    {
      key: 'remote-cmd',
      label: '원격CMD실행',
      children: (
        <Card size="small" title="원격 CMD 실행 (RCMD)">
          <Space direction="vertical" style={{ width: '100%' }}>
            <Space>
              <Input
                style={{ width: 360 }}
                value={rcmdText}
                onChange={(e) => setRcmdText(e.target.value)}
                placeholder="실행할 명령어"
              />
              <span>타임아웃</span>
              <Select
                style={{ width: 110 }}
                value={rcmdTimeout}
                onChange={setRcmdTimeout}
                options={[
                  { value: 60, label: '1분' },
                  { value: 120, label: '2분' },
                  { value: 180, label: '3분' },
                  { value: 240, label: '4분' },
                  { value: 300, label: '5분' },
                ]}
              />
            </Space>
            <Button
              type="primary"
              loading={issuing}
              disabled={!rcmdText.trim()}
              onClick={() => issue('RCMD', { command: rcmdText.trim() }, rcmdTimeout)}
            >
              실행
            </Button>
          </Space>
        </Card>
      ),
    },
  ];

  return (
    <PageTemplate title="기기 원격제어">
      <Form form={searchForm} layout="inline" style={{ marginBottom: 16 }} onFinish={handleSearch}>
        <Form.Item name="termGroupId" label="영업점(금고)">
          <Input placeholder="영업점코드" allowClear style={{ width: 140 }} />
        </Form.Item>
        <Form.Item name="termId" label="기번">
          <Input placeholder="단말번호" allowClear style={{ width: 130 }} />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
              조회
            </Button>
            <Button icon={<ReloadOutlined />} onClick={handleReset}>
              초기화
            </Button>
          </Space>
        </Form.Item>
      </Form>

      <Card size="small" style={{ marginBottom: 16 }}>
        <Tabs items={tabItems} />
      </Card>

      <Table<RemoteCommand>
        rowKey="cmdId"
        size="small"
        bordered
        loading={loading}
        columns={columns}
        dataSource={rows}
        scroll={{ x: 1300 }}
        title={() => (
          <Space style={{ justifyContent: 'space-between', width: '100%' }}>
            <strong>처리결과 / 발행이력</strong>
            <Button size="small" icon={<ReloadOutlined />} onClick={() => fetchList(page, size)}>
              새로고침
            </Button>
          </Space>
        )}
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

export default RemoteControlPage;
