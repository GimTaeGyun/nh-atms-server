import React, { useEffect, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Input,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Upload,
  message,
} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import type { UploadFile } from 'antd/es/upload/interface';
import type { RcFile } from 'antd/es/upload';
import {
  CloudUploadOutlined,
  ReloadOutlined,
  SearchOutlined,
  UploadOutlined,
} from '@ant-design/icons';
import PageTemplate from '@/components/PageTemplate';
import { useCodeStore } from '@/stores/useCodeStore';
import { useBranchStore } from '@/stores/useBranchStore';
import {
  downloadApi,
  type DownloadDevice,
  type PackageType,
} from '@/api/downloadApi';

const ATM_CO_GROUP = '050'; // 기기업체

const errMsgOf = (e: unknown, fallback: string) =>
  (e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? fallback;

const PKG_TYPE_LABEL: Record<PackageType, string> = { A: 'AP', G: '일반', M: '매체' };
const APP_TYPE_OPTS = (Object.keys(PKG_TYPE_LABEL) as PackageType[]).map((t) => ({
  value: t,
  label: PKG_TYPE_LABEL[t],
}));
const EXEC_TYPE_OPTS = [
  { value: '0', label: '개별다운로드' },
  { value: '1', label: '일반다운로드' },
];


const keyOf = (d: { termGroupId: string; termId: string }) => `${d.termGroupId}|${d.termId}`;

interface SearchValues {
  termGroupId?: string;
  branchCd?: string;
  cornerCd?: string;
  termId?: string;
  atmCoCd?: string;
}

/**
 * 개별다운로드관리.
 * 특정 기기를 선택(현재버전 vs 최종버전 비교)한 뒤, 화면에서 ZIP 파일을
 * 직접 업로드하여 그 기기들에만 즉시 배포한다. (MG 정합 : 자체 파일 업로드, 결재 무관)
 */
const IndividualDownloadPage: React.FC = () => {
  const [searchForm] = Form.useForm<SearchValues>();
  const [rows, setRows] = useState<DownloadDevice[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([]);
  const [selectedRows, setSelectedRows] = useState<DownloadDevice[]>([]);
  const [distOpen, setDistOpen] = useState(false);

  const { ensure, maps } = useCodeStore();
  const branchLabel = useBranchStore((s) => s.label);
  const loadBranches = useBranchStore((s) => s.load);

  useEffect(() => {
    ensure(ATM_CO_GROUP);
    loadBranches();
  }, [ensure, loadBranches]);

  const atmCoOpts = [
    { value: '', label: '전체' },
    ...Object.entries(maps[ATM_CO_GROUP] ?? {}).map(([code, nm]) => ({ value: code, label: `${nm}(${code})` })),
  ];

  const fetchList = async (p = page, s = size) => {
    const v = searchForm.getFieldsValue();
    setLoading(true);
    try {
      const res = await downloadApi.getDevices({
        termGroupId: v.termGroupId || undefined,
        branchCd: v.branchCd || undefined,
        cornerCd: v.cornerCd || undefined,
        termId: v.termId || undefined,
        atmCoCd: v.atmCoCd || undefined,
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
    fetchList(1, size);
  }, []); // eslint-disable-line

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
    const np = pg.current ?? 1;
    const ns = pg.pageSize ?? size;
    setPage(np);
    setSize(ns);
    fetchList(np, ns);
  };

  const openDistribute = () => {
    if (selectedRows.length === 0) {
      message.warning('배포할 기기를 1대 이상 선택하세요.');
      return;
    }
    setDistOpen(true);
  };

  const onDistributed = () => {
    setDistOpen(false);
    setSelectedKeys([]);
    setSelectedRows([]);
    fetchList(page, size);
  };

  const columns: ColumnsType<DownloadDevice> = [
    { title: '번호', key: 'no', width: 56, align: 'center', render: (_v, _r, i) => (page - 1) * size + i + 1 },
    {
      title: '금고',
      dataIndex: 'termGroupId',
      width: 150,
      render: (c?: string) => branchLabel(c) ?? c ?? '-',
    },
    {
      title: '본지점',
      dataIndex: 'branchCd',
      width: 150,
      render: (c?: string) => (c ? branchLabel(c) : '-'),
    },
    { title: '코너', dataIndex: 'cornerCd', width: 90, align: 'center', render: (v) => v ?? '-' },
    { title: '기번', dataIndex: 'termId', width: 90, align: 'center' },
    {
      title: '기기업체',
      dataIndex: 'atmCoCd',
      width: 110,
      render: (c?: string) => maps[ATM_CO_GROUP]?.[c ?? ''] ?? c ?? '-',
    },
    { title: '모델명', dataIndex: 'modelName', width: 130, render: (v, r) => v ?? r.modelCd ?? '-' },
    { title: '기기IP', dataIndex: 'atmIp', width: 130, align: 'center', render: (v) => v ?? '-' },
    {
      title: '접속',
      dataIndex: 'connectFlag',
      width: 70,
      align: 'center',
      render: (f?: string) =>
        f === 'Y' || f === '1' ? <Tag color="green">접속</Tag> : <Tag color="default">미접속</Tag>,
    },
    { title: 'AP버전', dataIndex: 'appVersion', width: 110, align: 'center', render: (v) => v ?? '-' },
    {
      title: '현재 다운로드버전',
      dataIndex: 'apDownVer',
      width: 140,
      align: 'center',
      render: (cur?: string, r?: DownloadDevice) => {
        const last = r?.lastAppVer;
        const stale = !!cur && !!last && cur !== last;
        return <span style={{ color: stale ? '#cf1322' : undefined }}>{cur ?? '-'}</span>;
      },
    },
    {
      title: '최종 다운로드버전',
      dataIndex: 'lastAppVer',
      width: 140,
      align: 'center',
      render: (v) => v ?? '-',
    },
    { title: '에이전트버전', dataIndex: 'agentVer', width: 120, align: 'center', render: (v) => v ?? '-' },
  ];

  return (
    <PageTemplate title="개별다운로드관리">
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={searchForm} layout="inline" onFinish={handleSearch}>
          <Form.Item name="termGroupId" label="금고(영업점)">
            <Input style={{ width: 130 }} placeholder="금고코드" allowClear />
          </Form.Item>
          <Form.Item name="branchCd" label="본지점">
            <Input style={{ width: 120 }} placeholder="본지점코드" allowClear />
          </Form.Item>
          <Form.Item name="termId" label="기번">
            <Input style={{ width: 110 }} placeholder="기번" allowClear />
          </Form.Item>
          <Form.Item name="atmCoCd" label="기기업체" initialValue="">
            <Select style={{ width: 170 }} options={atmCoOpts} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" icon={<SearchOutlined />} htmlType="submit">조회</Button>
              <Button icon={<ReloadOutlined />} onClick={handleReset}>초기화</Button>
              <Button type="primary" ghost icon={<CloudUploadOutlined />} onClick={openDistribute}>
                개별배포{selectedRows.length > 0 ? ` (${selectedRows.length})` : ''}
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Table<DownloadDevice>
        rowKey={keyOf}
        size="small"
        columns={columns}
        dataSource={rows}
        loading={loading}
        scroll={{ x: 1700 }}
        rowSelection={{
          selectedRowKeys: selectedKeys,
          onChange: (keys, sRows) => {
            setSelectedKeys(keys);
            setSelectedRows(sRows);
          },
        }}
        pagination={{
          current: page,
          pageSize: size,
          total,
          showSizeChanger: true,
          showTotal: (t) => `총 ${t}건`,
        }}
        onChange={handleTableChange}
      />

      <DistributeModal
        open={distOpen}
        targets={selectedRows}
        onClose={() => setDistOpen(false)}
        onDistributed={onDistributed}
      />
    </PageTemplate>
  );
};

// ── 개별배포 모달 : ZIP 파일 직접 업로드 후 선택 기기에 배포(결재 무관) ──────
interface DistributeModalProps {
  open: boolean;
  targets: DownloadDevice[];
  onClose: () => void;
  onDistributed: () => void;
}

interface UploadFormValues {
  appType: PackageType;
  appVer: string;
  execType?: string;
  downloadPath?: string;
  applyMemo?: string;
}

const DistributeModal: React.FC<DistributeModalProps> = ({ open, targets, onClose, onDistributed }) => {
  const [form] = Form.useForm<UploadFormValues>();
  const [fileList, setFileList] = useState<UploadFile[]>([]);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [sending, setSending] = useState(false);

  // MG 화면선택(수동) 방식과 동일 : 기종 제한 없이 선택 기기 전부 배포.
  // atmCoCd/modelCd는 마스터 기록용 대표값(첫 기기)일 뿐, 일치 검증은 하지 않는다.
  const atmCoCd = targets[0]?.atmCoCd;
  const modelCd = targets[0]?.modelCd;

  useEffect(() => {
    if (!open) return;
    setFileList([]);
    setSelectedFile(null);
    form.resetFields();
  }, [open]); // eslint-disable-line

  const beforeUpload = (file: RcFile) => {
    if (!file.name.toLowerCase().endsWith('.zip')) {
      message.error('ZIP 파일만 업로드할 수 있습니다.');
      return Upload.LIST_IGNORE;
    }
    setSelectedFile(file);
    setFileList([file as unknown as UploadFile]);
    return false; // 자동 업로드 방지(직접 전송)
  };

  const handleSend = async () => {
    if (!selectedFile) {
      message.warning('배포할 ZIP 파일을 선택하세요.');
      return;
    }
    let values: UploadFormValues;
    try {
      values = await form.validateFields();
    } catch {
      return;
    }
    setSending(true);
    try {
      const res = await downloadApi.uploadIndividual(selectedFile, {
        appType: values.appType,
        atmCoCd,
        modelCd,
        appVer: values.appVer,
        execType: values.execType,
        downloadPath: values.downloadPath,
        applyMemo: values.applyMemo,
        targets: targets.map((t) => ({ termGroupId: t.termGroupId, termId: t.termId })),
      });
      message.success(res.data.message ?? `${res.data.data}대 기기에 배포했습니다.`);
      onDistributed();
    } catch (e: unknown) {
      message.error(errMsgOf(e, '개별 배포 실패'));
    } finally {
      setSending(false);
    }
  };

  return (
    <Modal
      title="개별 다운로드 배포"
      open={open}
      onCancel={onClose}
      onOk={handleSend}
      confirmLoading={sending}
      okText="배포"
      cancelText="취소"
      width={560}
      destroyOnClose
    >
      <p style={{ marginBottom: 12 }}>
        선택 기기 <b>{targets.length}</b>대에 업로드한 파일을 배포합니다.
      </p>
      <Form form={form} layout="vertical" initialValues={{ execType: '0', downloadPath: 'D:\\LGDOWN' }}>
        <Form.Item label="배포파일(ZIP)" required>
          <Upload
            accept=".zip"
            beforeUpload={beforeUpload}
            onRemove={() => {
              setSelectedFile(null);
              setFileList([]);
            }}
            fileList={fileList}
            maxCount={1}
          >
            <Button icon={<UploadOutlined />}>ZIP 파일 선택</Button>
          </Upload>
        </Form.Item>
        <Form.Item name="appType" label="파일유형" rules={[{ required: true, message: '파일유형을 선택하세요.' }]}>
          <Select options={APP_TYPE_OPTS} placeholder="파일유형 선택" style={{ width: 200 }} />
        </Form.Item>
        <Form.Item name="appVer" label="다운로드버전" rules={[{ required: true, message: '다운로드버전을 입력하세요.' }]}>
          <Input placeholder="예) 1.0.0" style={{ width: 200 }} />
        </Form.Item>
        <Form.Item name="execType" label="다운로드종류">
          <Select options={EXEC_TYPE_OPTS} style={{ width: 200 }} />
        </Form.Item>
        <Form.Item name="downloadPath" label="다운로드경로">
          <Input placeholder="D:\LGDOWN" />
        </Form.Item>
        <Form.Item name="applyMemo" label="적용메모">
          <Input.TextArea rows={2} maxLength={200} placeholder="(선택) 적용 사유/메모" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default IndividualDownloadPage;
