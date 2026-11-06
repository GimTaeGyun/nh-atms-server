import React, { useEffect, useState } from 'react';
import { Form, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import { useTabStore } from '@/stores/useTabStore';
import { downloadExcel } from '@/utils/excelDownload';
import SearchForm from '@/components/SearchForm';
import DataTable from '@/components/DataTable';
import PageTemplate from '@/components/PageTemplate';
import type { SearchField } from '@/types/search';
import {
  transactionJournalApi,
  type TransactionJournal,
  type TransactionJournalSearchParams,
} from '@/api/transactionJournalApi';
import { renderTrType, renderTrResult, fmtAmount, fmtDateTime, fmtTermId, fmtAccount } from '@/utils/transaction';

const PAGE_KEY = 'tx-cardless';

interface PageState {
  values: Record<string, unknown>;
  page: number;
  size: number;
  searched: boolean;
}

type Row = TransactionJournal & { key: string; [k: string]: unknown };

const fields: SearchField[] = [
  { name: 'termGroupId', label: '사무소', type: 'input', placeholder: '영업점코드(전체)', width: 140 },
  { name: 'termId', label: '기번', type: 'input', placeholder: '단말번호', width: 120 },
  { name: 'dateRange', label: '거래일자', type: 'rangePicker' },
];

const CardlessPage: React.FC = () => {
  const { getPageState, setPageState } = useTabStore();
  const [form] = Form.useForm();

  const saved = getPageState(PAGE_KEY) as PageState | undefined;
  const [rows, setRows] = useState<Row[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(saved?.page || 1);
  const [size, setSize] = useState(saved?.size || 10);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(saved?.searched || false);

  const buildParams = (p: number, s: number): TransactionJournalSearchParams => {
    const v = form.getFieldsValue();
    return {
      termGroupId: v.termGroupId || undefined,
      termId: v.termId || undefined,
      fromDate: Array.isArray(v.dateRange) ? v.dateRange[0].format('YYYYMMDD') : undefined,
      toDate: Array.isArray(v.dateRange) ? v.dateRange[1].format('YYYYMMDD') : undefined,
      page: p,
      size: s,
    };
  };

  const fetchData = async (p: number, s: number) => {
    setLoading(true);
    try {
      const res = await transactionJournalApi.getList(buildParams(p, s));
      const result = res.data.data;
      const list: Row[] = (result.content ?? []).map((r, i) => ({
        ...r,
        key: `${r.termGroupId}-${r.termId}-${r.jnlDate}-${r.jnlTime}-${i}`,
      }));
      setRows(list);
      setTotal(result.totalCount ?? 0);
      setPage(p);
      setSize(s);
      setSearched(true);
      const v = form.getFieldsValue();
      const stored: Record<string, unknown> = { ...v };
      if (Array.isArray(v.dateRange)) {
        stored.dateRange = [v.dateRange[0].format('YYYY-MM-DD'), v.dateRange[1].format('YYYY-MM-DD')];
      }
      setPageState(PAGE_KEY, { values: stored, page: p, size: s, searched: true });
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '거래내역 조회에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (saved?.values) {
      const v = { ...saved.values } as Record<string, unknown>;
      if (Array.isArray(v.dateRange)) {
        v.dateRange = [dayjs(v.dateRange[0] as string), dayjs(v.dateRange[1] as string)];
      }
      form.setFieldsValue(v);
      if (saved.searched) void fetchData(saved.page || 1, saved.size || 10);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = () => void fetchData(1, size);
  const handlePageChange = (p: number, s: number) => void fetchData(p, s);

  const columns: ColumnsType<Row> = [
    { title: '순번', key: 'no', width: 60, align: 'center', render: (_v, _r, i) => (page - 1) * size + i + 1 },
    { title: '거래종류', dataIndex: 'trType', key: 'trType', width: 90, align: 'center', render: (v) => renderTrType(v) },
    { title: '거래일시', key: 'date', width: 170, align: 'center', render: (_v, r) => fmtDateTime(r.jnlDate, r.jnlTime) },
    { title: '기번', key: 'termId', width: 120, align: 'center', render: (_v, r) => fmtTermId(r.termGroupId, r.termId) },
    { title: '일련번호', dataIndex: 'trNo', key: 'trNo', width: 90, align: 'center', render: (v) => v || '-' },
    { title: '계좌은행/예금주명 계좌번호', key: 'account', width: 260, render: (_v, r) => fmtAccount(r.bankCd, r.accountNo, r.transferName) },
    { title: '거래금액', key: 'amt', width: 120, align: 'right', render: (_v, r) => fmtAmount(r.requestAmount ?? r.dispenseAmount) },
    { title: '수수료', dataIndex: 'surcharge', key: 'surcharge', width: 90, align: 'right', render: (v) => fmtAmount(v) },
    { title: '거래상태', dataIndex: 'trResult', key: 'trResult', width: 100, align: 'center', render: (v) => renderTrResult(v) },
    { title: '합계구분', dataIndex: 'totalTypeCd', key: 'totalTypeCd', width: 80, align: 'center', render: (v) => v || '-' },
  ];

  const handleExcel = () => {
    if (rows.length === 0) return;
    downloadExcel(
      [
        { title: '거래종류', dataIndex: 'trType' },
        { title: '거래일자', dataIndex: 'jnlDate' },
        { title: '거래시간', dataIndex: 'jnlTime' },
        { title: '영업점코드', dataIndex: 'termGroupId' },
        { title: '단말번호', dataIndex: 'termId' },
        { title: '일련번호', dataIndex: 'trNo' },
        { title: '은행코드', dataIndex: 'bankCd' },
        { title: '계좌번호', dataIndex: 'accountNo' },
        { title: '거래금액', dataIndex: 'requestAmount' },
        { title: '수수료', dataIndex: 'surcharge' },
        { title: '거래상태', dataIndex: 'trResult' },
      ],
      rows,
      '무통장무카드거래',
    );
  };

  return (
    <PageTemplate title="무통장/무카드 거래">
      <SearchForm form={form} fields={fields} onSearch={handleSearch} onExcel={handleExcel} loading={loading} />
      <DataTable
        columns={columns}
        dataSource={rows}
        visible={searched}
        loading={loading}
        scrollX={1250}
        pageSize={size}
        total={total}
        current={page}
        onChange={handlePageChange}
      />
    </PageTemplate>
  );
};

export default CardlessPage;
