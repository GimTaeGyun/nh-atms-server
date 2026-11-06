import React, { useEffect, useState } from 'react';
import {
  Button,
  Card,
  Form,
  Popconfirm,
  Select,
  Space,
  Table,
  Tag,
  message,
} from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import {
  CheckOutlined,
  CloudUploadOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  StopOutlined,
} from '@ant-design/icons';
import PageTemplate from '@/components/PageTemplate';
import { useCodeStore } from '@/stores/useCodeStore';
import {
  downloadApi,
  type DownloadPackage,
  type PackageType,
  type ConfirmStatus,
} from '@/api/downloadApi';
import DownloadPackageModal from './DownloadPackageModal';

const ATM_CO_GROUP = '050'; // 기기업체

const errMsgOf = (e: unknown, fallback: string) =>
  (e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? fallback;

const PKG_TYPE_META: Record<PackageType, { label: string; color: string }> = {
  A: { label: 'AP', color: 'blue' },
  G: { label: '일반', color: 'geekblue' },
  M: { label: '매체', color: 'purple' },
};

const CONFIRM_META: Record<ConfirmStatus, { label: string; color: string }> = {
  N: { label: '미결재', color: 'default' },
  Y: { label: '결재완료', color: 'green' },
  R: { label: '반려', color: 'red' },
};

const fmtDate = (d?: string) => (!d || d.length < 8 ? (d ?? '-') : `${d.slice(0, 4)}-${d.slice(4, 6)}-${d.slice(6, 8)}`);
const fmtTime = (t?: string) => (!t || t.length < 4 ? (t ?? '-') : `${t.slice(0, 2)}:${t.slice(2, 4)}`);
const fmtSize = (n?: number) => (n == null ? '-' : n.toLocaleString());
const fmtDt = (s?: string) => (s ? s.replace('T', ' ').slice(0, 19) : '-');

interface SearchValues {
  atmCoCd?: string;
  pkgType?: string;
  confirmStatus?: string;
}

/**
 * 일괄다운로드관리.
 * 기종(업체+모델)별 다운로드 패키지를 등록·결재(승인)하고,
 * 결재완료된 패키지를 해당 기종 전체 기기에 일괄 배포한다.
 */
const BatchDownloadPage: React.FC = () => {
  const [searchForm] = Form.useForm<SearchValues>();
  const [rows, setRows] = useState<DownloadPackage[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);
  const [modalOpen, setModalOpen] = useState(false);

  const { ensure, maps } = useCodeStore();

  useEffect(() => {
    ensure(ATM_CO_GROUP);
  }, [ensure]);

  const atmCoOpts = [
    { value: '', label: '전체' },
    ...Object.entries(maps[ATM_CO_GROUP] ?? {}).map(([code, nm]) => ({ value: code, label: `${nm}(${code})` })),
  ];

  const fetchList = async (p = page, s = size) => {
    const v = searchForm.getFieldsValue();
    setLoading(true);
    try {
      const res = await downloadApi.getPackages({
        atmCoCd: v.atmCoCd || undefined,
        pkgType: v.pkgType || undefined,
        confirmStatus: v.confirmStatus || undefined,
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

  const handleConfirm = async (pkg: DownloadPackage) => {
    try {
      await downloadApi.confirm(pkg.pkgId);
      message.success('결재 처리되었습니다.');
      fetchList(page, size);
    } catch (e: unknown) {
      message.error(errMsgOf(e, '결재 실패'));
    }
  };

  const handleReject = async (pkg: DownloadPackage) => {
    try {
      await downloadApi.reject(pkg.pkgId);
      message.success('반려 처리되었습니다.');
      fetchList(page, size);
    } catch (e: unknown) {
      message.error(errMsgOf(e, '반려 실패'));
    }
  };

  const handleDistribute = async (pkg: DownloadPackage) => {
    try {
      const res = await downloadApi.distributeBatch(pkg.pkgId);
      message.success(res.data.message ?? `${res.data.data}대 기기에 일괄 배포했습니다.`);
      fetchList(page, size);
    } catch (e: unknown) {
      message.error(errMsgOf(e, '일괄 배포 실패'));
    }
  };

  const columns: ColumnsType<DownloadPackage> = [
    {
      title: '번호',
      key: 'no',
      width: 60,
      align: 'center',
      render: (_v, _r, i) => (page - 1) * size + i + 1,
    },
    {
      title: '유형',
      dataIndex: 'pkgType',
      width: 80,
      align: 'center',
      render: (t: PackageType) => <Tag color={PKG_TYPE_META[t]?.color}>{PKG_TYPE_META[t]?.label ?? t}</Tag>,
    },
    {
      title: '기기업체',
      dataIndex: 'atmCoCd',
      width: 120,
      render: (c?: string) => maps[ATM_CO_GROUP]?.[c ?? ''] ?? c ?? '-',
    },
    { title: '모델명', dataIndex: 'modelName', width: 140, render: (v, r) => v ?? r.modelCd },
    { title: '최종버전', dataIndex: 'downVer', width: 110, align: 'center' },
    { title: '파일명', dataIndex: 'fileName', width: 180, ellipsis: true },
    { title: '파일크기', dataIndex: 'fileSize', width: 110, align: 'right', render: fmtSize },
    { title: '적용일', dataIndex: 'applyDate', width: 110, align: 'center', render: fmtDate },
    { title: '적용시각', dataIndex: 'applyTime', width: 90, align: 'center', render: fmtTime },
    {
      title: '결재여부',
      dataIndex: 'confirmStatus',
      width: 90,
      align: 'center',
      render: (s: ConfirmStatus) => <Tag color={CONFIRM_META[s]?.color}>{CONFIRM_META[s]?.label ?? s}</Tag>,
    },
    { title: '결재자', dataIndex: 'confirmUserId', width: 100, align: 'center', render: (v) => v ?? '-' },
    { title: '결재일시', dataIndex: 'confirmDt', width: 160, align: 'center', render: fmtDt },
    {
      title: '작업',
      key: 'action',
      width: 230,
      fixed: 'right',
      render: (_v, r) => (
        <Space size="small">
          {r.confirmStatus !== 'Y' && (
            <Popconfirm title="이 패키지를 결재(승인)하시겠습니까?" onConfirm={() => handleConfirm(r)} okText="결재" cancelText="취소">
              <Button size="small" icon={<CheckOutlined />}>결재</Button>
            </Popconfirm>
          )}
          {r.confirmStatus === 'N' && (
            <Popconfirm title="이 패키지를 반려하시겠습니까?" onConfirm={() => handleReject(r)} okText="반려" cancelText="취소">
              <Button size="small" danger icon={<StopOutlined />}>반려</Button>
            </Popconfirm>
          )}
          {r.confirmStatus === 'Y' && (
            <Popconfirm
              title={`${r.modelName ?? r.modelCd} 기종 전체에 일괄 배포하시겠습니까?`}
              onConfirm={() => handleDistribute(r)}
              okText="배포"
              cancelText="취소"
            >
              <Button size="small" type="primary" icon={<CloudUploadOutlined />}>일괄배포</Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <PageTemplate title="일괄다운로드관리">
      <Card size="small" style={{ marginBottom: 12 }}>
        <Form form={searchForm} layout="inline" onFinish={handleSearch}>
          <Form.Item name="atmCoCd" label="기기업체" initialValue="">
            <Select style={{ width: 180 }} options={atmCoOpts} />
          </Form.Item>
          <Form.Item name="pkgType" label="유형" initialValue="">
            <Select
              style={{ width: 120 }}
              options={[
                { value: '', label: '전체' },
                { value: 'A', label: 'AP' },
                { value: 'G', label: '일반' },
                { value: 'M', label: '매체' },
              ]}
            />
          </Form.Item>
          <Form.Item name="confirmStatus" label="결재여부" initialValue="">
            <Select
              style={{ width: 120 }}
              options={[
                { value: '', label: '전체' },
                { value: 'N', label: '미결재' },
                { value: 'Y', label: '결재완료' },
                { value: 'R', label: '반려' },
              ]}
            />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" icon={<SearchOutlined />} htmlType="submit">조회</Button>
              <Button icon={<ReloadOutlined />} onClick={handleReset}>초기화</Button>
              <Button icon={<PlusOutlined />} onClick={() => setModalOpen(true)}>패키지 등록</Button>
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Table<DownloadPackage>
        rowKey="pkgId"
        size="small"
        columns={columns}
        dataSource={rows}
        loading={loading}
        scroll={{ x: 1700 }}
        pagination={{
          current: page,
          pageSize: size,
          total,
          showSizeChanger: true,
          showTotal: (t) => `총 ${t}건`,
        }}
        onChange={handleTableChange}
      />

      <DownloadPackageModal open={modalOpen} onClose={() => setModalOpen(false)} onSaved={() => fetchList(1, size)} />
    </PageTemplate>
  );
};

export default BatchDownloadPage;
