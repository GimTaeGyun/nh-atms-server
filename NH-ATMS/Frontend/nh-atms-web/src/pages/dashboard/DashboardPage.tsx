import React, { useCallback, useEffect, useState } from 'react';
import {
  Alert, Button, Card, Col, Progress, Row, Space, Spin, Statistic, Table, Tag,
  Typography, message,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
  ReloadOutlined, DesktopOutlined, CheckCircleOutlined, WarningOutlined,
  CloseCircleOutlined, ToolOutlined, ApiOutlined, SwapOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import { atmMonitorApi } from '@/api/atmMonitorApi';
import { agentStatusApi } from '@/api/agentStatusApi';
import { faultFrequentApi, type FaultFrequent } from '@/api/faultFrequentApi';
import { transactionJournalApi } from '@/api/transactionJournalApi';

const { Title, Text } = Typography;

/** 페이지 1건만 요청해 totalCount 만 얻기 위한 공통 파라미터. */
const COUNT_ONLY = { page: 0, size: 1 } as const;

interface Kpi {
  atmTotal: number;
  atmNormal: number;
  atmWarn: number;
  atmFault: number;
  atmCheck: number;
  agentOnline: number;
  agentOffline: number;
  faultFrequent: number;
  txToday: number;
  txTodayError: number;
}

const EMPTY_KPI: Kpi = {
  atmTotal: 0, atmNormal: 0, atmWarn: 0, atmFault: 0, atmCheck: 0,
  agentOnline: 0, agentOffline: 0, faultFrequent: 0, txToday: 0, txTodayError: 0,
};

const fmtDate = (d?: string) =>
  d && d.length >= 8 ? `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}` : '-';

const DashboardPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [kpi, setKpi] = useState<Kpi>(EMPTY_KPI);
  const [faultRows, setFaultRows] = useState<FaultFrequent[]>([]);
  const [updatedAt, setUpdatedAt] = useState<string>('');

  const today = dayjs().format('YYYYMMDD');

  const fetchAll = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const totalCount = (r: { data: { data: { totalCount: number } } }) => r.data.data.totalCount;
      const [
        atmTotalR, atmNormalR, atmWarnR, atmFaultR, atmCheckR,
        agentOnR, agentOffR, faultR, txTodayR, txTodayErrR, faultListR,
      ] = await Promise.all([
        atmMonitorApi.getList({ ...COUNT_ONLY }),
        atmMonitorApi.getList({ ...COUNT_ONLY, atmStatus: '00' }),
        atmMonitorApi.getList({ ...COUNT_ONLY, atmStatus: '01' }),
        atmMonitorApi.getList({ ...COUNT_ONLY, atmStatus: '02' }),
        atmMonitorApi.getList({ ...COUNT_ONLY, atmStatus: '09' }),
        agentStatusApi.getList({ ...COUNT_ONLY, connectFlag: '1' }),
        agentStatusApi.getList({ ...COUNT_ONLY, connectFlag: '0' }),
        faultFrequentApi.getList({ ...COUNT_ONLY, minFaultCnt: 3 }),
        transactionJournalApi.getList({ ...COUNT_ONLY, fromDate: today, toDate: today }),
        transactionJournalApi.getList({ ...COUNT_ONLY, fromDate: today, toDate: today, errorOnly: true }),
        faultFrequentApi.getList({ page: 0, size: 5, minFaultCnt: 3 }),
      ]);

      setKpi({
        atmTotal: totalCount(atmTotalR),
        atmNormal: totalCount(atmNormalR),
        atmWarn: totalCount(atmWarnR),
        atmFault: totalCount(atmFaultR),
        atmCheck: totalCount(atmCheckR),
        agentOnline: totalCount(agentOnR),
        agentOffline: totalCount(agentOffR),
        faultFrequent: totalCount(faultR),
        txToday: totalCount(txTodayR),
        txTodayError: totalCount(txTodayErrR),
      });
      setFaultRows(faultListR.data.data.content);
      setUpdatedAt(dayjs().format('YYYY-MM-DD HH:mm:ss'));
    } catch {
      setError('대시보드 데이터를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');
      message.error('대시보드 조회 실패');
    } finally {
      setLoading(false);
    }
  }, [today]);

  useEffect(() => {
    fetchAll();
  }, [fetchAll]);

  const normalRate = kpi.atmTotal > 0 ? Math.round((kpi.atmNormal / kpi.atmTotal) * 100) : 0;
  const agentTotal = kpi.agentOnline + kpi.agentOffline;
  const onlineRate = agentTotal > 0 ? Math.round((kpi.agentOnline / agentTotal) * 100) : 0;
  const txErrRate = kpi.txToday > 0 ? Math.round((kpi.txTodayError / kpi.txToday) * 1000) / 10 : 0;

  const faultColumns: ColumnsType<FaultFrequent> = [
    { title: '관리점', dataIndex: 'termGroupId', key: 'termGroupId', width: 110 },
    { title: '기기번호', dataIndex: 'termId', key: 'termId', width: 110 },
    {
      title: '장애건수', dataIndex: 'faultCnt', key: 'faultCnt', width: 100,
      render: (v: number) => <Tag color="red">{v.toLocaleString()}건</Tag>,
    },
    {
      title: '장애율', dataIndex: 'faultRate', key: 'faultRate', width: 90,
      render: (v: number) => `${v ?? 0}%`,
    },
    {
      title: '최근장애일', dataIndex: 'lastFaultDate', key: 'lastFaultDate',
      render: (v?: string) => fmtDate(v),
    },
    { title: '최근에러코드', dataIndex: 'lastErrCd', key: 'lastErrCd', render: (v?: string) => v ?? '-' },
  ];

  return (
    <Spin spinning={loading}>
      <Space direction="vertical" size="large" style={{ width: '100%' }}>
        <Row justify="space-between" align="middle">
          <Col>
            <Title level={4} style={{ margin: 0 }}>운영 대시보드</Title>
            <Text type="secondary">
              {updatedAt ? `최종 업데이트 ${updatedAt}` : 'NH ATMS 관리 시스템'}
            </Text>
          </Col>
          <Col>
            <Button icon={<ReloadOutlined />} onClick={fetchAll}>새로고침</Button>
          </Col>
        </Row>

        {error && <Alert type="error" showIcon message={error} />}

        {/* 상단 KPI 카드 */}
        <Row gutter={[16, 16]}>
          <Col xs={12} sm={12} md={8} lg={6} xl={4}>
            <Card>
              <Statistic
                title="전체 ATM"
                value={kpi.atmTotal}
                prefix={<DesktopOutlined style={{ color: '#1677ff' }} />}
                suffix="대"
              />
            </Card>
          </Col>
          <Col xs={12} sm={12} md={8} lg={6} xl={4}>
            <Card>
              <Statistic
                title="정상"
                value={kpi.atmNormal}
                valueStyle={{ color: '#52c41a' }}
                prefix={<CheckCircleOutlined />}
                suffix="대"
              />
            </Card>
          </Col>
          <Col xs={12} sm={12} md={8} lg={6} xl={4}>
            <Card>
              <Statistic
                title="주의"
                value={kpi.atmWarn}
                valueStyle={{ color: '#faad14' }}
                prefix={<WarningOutlined />}
                suffix="대"
              />
            </Card>
          </Col>
          <Col xs={12} sm={12} md={8} lg={6} xl={4}>
            <Card>
              <Statistic
                title="장애"
                value={kpi.atmFault}
                valueStyle={{ color: '#ff4d4f' }}
                prefix={<CloseCircleOutlined />}
                suffix="대"
              />
            </Card>
          </Col>
          <Col xs={12} sm={12} md={8} lg={6} xl={4}>
            <Card>
              <Statistic
                title="점검"
                value={kpi.atmCheck}
                valueStyle={{ color: '#1677ff' }}
                prefix={<ToolOutlined />}
                suffix="대"
              />
            </Card>
          </Col>
          <Col xs={12} sm={12} md={8} lg={6} xl={4}>
            <Card>
              <Statistic
                title="장애 빈발 ATM"
                value={kpi.faultFrequent}
                valueStyle={{ color: '#ff7a45' }}
                prefix={<WarningOutlined />}
                suffix="대"
              />
            </Card>
          </Col>
        </Row>

        {/* 가동률 / 에이전트 / 거래 현황 */}
        <Row gutter={[16, 16]}>
          <Col xs={24} md={8}>
            <Card title="ATM 정상 가동률">
              <Row align="middle" gutter={16}>
                <Col>
                  <Progress
                    type="dashboard"
                    percent={normalRate}
                    status={normalRate >= 95 ? 'success' : normalRate >= 80 ? 'normal' : 'exception'}
                  />
                </Col>
                <Col flex="auto">
                  <Statistic title="정상" value={kpi.atmNormal} suffix={`/ ${kpi.atmTotal} 대`} />
                  <Text type="secondary">주의 {kpi.atmWarn} · 장애 {kpi.atmFault} · 점검 {kpi.atmCheck}</Text>
                </Col>
              </Row>
            </Card>
          </Col>

          <Col xs={24} md={8}>
            <Card title={<span><ApiOutlined /> 에이전트 접속 현황</span>}>
              <Row align="middle" gutter={16}>
                <Col>
                  <Progress
                    type="dashboard"
                    percent={onlineRate}
                    status={onlineRate >= 95 ? 'success' : onlineRate >= 80 ? 'normal' : 'exception'}
                  />
                </Col>
                <Col flex="auto">
                  <Statistic
                    title="접속"
                    value={kpi.agentOnline}
                    valueStyle={{ color: '#52c41a' }}
                    suffix={`/ ${agentTotal} 대`}
                  />
                  <Text type="secondary">해제 {kpi.agentOffline} 대</Text>
                </Col>
              </Row>
            </Card>
          </Col>

          <Col xs={24} md={8}>
            <Card title={<span><SwapOutlined /> 금일 거래 현황</span>}>
              <Statistic title="총 거래" value={kpi.txToday} suffix="건" />
              <div style={{ marginTop: 12 }}>
                <Statistic
                  title="오류 거래"
                  value={kpi.txTodayError}
                  valueStyle={{ color: kpi.txTodayError > 0 ? '#ff4d4f' : undefined }}
                  suffix={`건 (${txErrRate}%)`}
                />
              </div>
            </Card>
          </Col>
        </Row>

        {/* 장애 빈발 ATM TOP 5 */}
        <Card title="장애 빈발 ATM TOP 5" size="small">
          <Table
            rowKey={(r) => `${r.termGroupId}-${r.termId}`}
            columns={faultColumns}
            dataSource={faultRows}
            pagination={false}
            size="small"
            locale={{ emptyText: '장애 빈발 ATM이 없습니다.' }}
          />
        </Card>
      </Space>
    </Spin>
  );
};

export default DashboardPage;