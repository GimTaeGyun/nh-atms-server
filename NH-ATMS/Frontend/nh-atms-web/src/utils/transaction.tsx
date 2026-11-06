import React from 'react';
import { Tag } from 'antd';

/**
 * 거래저널(T_TRANSACTION_JOURNAL) 공용 표시 유틸.
 * 거래종류 코드(TR_TYPE)는 ATM 시뮬레이터/에이전트가 전송하는 실제 코드 기준.
 *   WD0001 출금 / WD0002 출금취소 / DP0001 입금 / TR0001 이체 / BL0001 잔액조회
 */
export const TR_TYPE_LABEL: Record<string, { label: string; color: string }> = {
  WD0001: { label: '출금', color: 'red' },
  WD0002: { label: '출금취소', color: 'volcano' },
  DP0001: { label: '입금', color: 'green' },
  TR0001: { label: '이체', color: 'blue' },
  BL0001: { label: '잔액조회', color: 'default' },
};

/** 거래종류 선택 옵션 (체크박스/셀렉트 공용) */
export const TR_TYPE_OPTIONS = [
  { value: 'WD0001', label: '출금' },
  { value: 'DP0001', label: '입금' },
  { value: 'TR0001', label: '이체' },
  { value: 'BL0001', label: '잔액조회' },
  { value: 'WD0002', label: '출금취소' },
];

/** 은행코드 → 은행명 (시뮬레이터 banks 기준 + 주요 은행) */
export const BANK_LABEL: Record<string, string> = {
  '011': '농협',
  '012': '농축협',
  '020': '우리',
  '088': '신한',
  '004': '국민',
  '003': '기업',
  '081': '하나',
  '023': 'SC',
};

export const renderTrType = (v?: string): React.ReactNode => {
  if (!v) return '-';
  const def = TR_TYPE_LABEL[v];
  return def ? <Tag color={def.color}>{def.label}</Tag> : <Tag>{v}</Tag>;
};

export const renderTrResult = (v?: string): React.ReactNode => {
  if (!v) return '-';
  if (v === '0000') return <Tag color="green">정상</Tag>;
  return <Tag color="red">실패 {v}</Tag>;
};

export const fmtAmount = (v?: number | null): string =>
  v == null ? '-' : Number(v).toLocaleString();

/** YYYYMMDD + HHMMSS → 'YYYY-MM-DD HH:mm:ss' */
export const fmtDateTime = (d?: string, t?: string): string => {
  if (!d || d.length < 8) return '-';
  const ymd = `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}`;
  if (!t || t.length < 6) return ymd;
  return `${ymd} ${t.substring(0, 2)}:${t.substring(2, 4)}:${t.substring(4, 6)}`;
};

/** 영업점코드 + 단말번호 → '00107-404' 형태 기번 표기 */
export const fmtTermId = (termGroupId?: string, termId?: string): string => {
  if (!termGroupId && !termId) return '-';
  return `${termGroupId ?? ''}-${termId ?? ''}`;
};

/** 은행코드 + 계좌번호 + 예금주명 → '농협 / 홍길동 302-****-1234' */
export const fmtAccount = (bankCd?: string, accountNo?: string, name?: string): string => {
  const bank = bankCd ? BANK_LABEL[bankCd] ?? bankCd : '';
  const parts = [bank, name, accountNo].filter((s) => s != null && s !== '');
  return parts.length ? parts.join(' ') : '-';
};
