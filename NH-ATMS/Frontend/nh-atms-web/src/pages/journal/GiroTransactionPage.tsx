import React, { useEffect, useState } from 'react';
import { Button, DatePicker, Form, Input, Space, Table, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import PageTemplate from '@/components/PageTemplate';

// 지로거래상세 (T_GIROTRANSACTION / 거래유형 1003)
interface GiroTransaction {
  jnlDate: string;
  termGroupId: string;
  termId: string;
  jnlTime: string;
  giroNo: string;
  payAmount: number;
  feeAmount: number;
  result: string;
}

const GiroTransactionPage: React.FC = () => {
  const [form] = Form.useForm();
  const [rows, setRows] = useState<GiroTransaction[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchList = async () => {
    setLoading(true);
    try {
      // TODO: 백엔드 API 연동 (GET /api/journal/giro)
      setRows([]);
      message.info('지로(공과금) 조회 API 연동 예정입니다.');
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '조회 실패');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void fetchList();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleReset = () => {
    form.resetFields();
    void fetchList();
  };

  const columns: ColumnsType<GiroTransaction> = [
    { title: '거래일자', dataIndex: 'jnlDate', width: 110, align: 'center' },
    { title: '영업점코드', dataIndex: 'termGroupId', width: 100, align: 'center' },
    { title: '단말번호', dataIndex: 'termId', width: 120, align: 'center' },
    { title: '거래시각', dataIndex: 'jnlTime', width: 100, align: 'center' },
    { title: '지로번호', dataIndex: 'giroNo', width: 140, align: 'center' },
    { title: '납부금액', dataIndex: 'payAmount', width: 130, align: 'right' },
    { title: '수수료', dataIndex: 'feeAmount', width: 110, align: 'right' },
    { title: '처리결과', dataIndex: 'result', width: 90, align: 'center' },
  ];

  return (
    <PageTemplate title="지로/공과금조회">
      <Form form={form} layout="inline" style={{ marginBottom: 16 }} onFinish={fetchList}>
        <Form.Item name="jnlDate" label="거래일자" initialValue={dayjs()}>
          <DatePicker style={{ width: 140 }} />
        </Form.Item>
        <Form.Item name="termGroupId" label="영업점코드">
          <Input placeholder="영업점코드" allowClear style={{ width: 120 }} />
        </Form.Item>
        <Form.Item name="giroNo" label="지로번호">
          <Input placeholder="지로번호" allowClear style={{ width: 140 }} />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>조회</Button>
            <Button icon={<ReloadOutlined />} onClick={handleReset}>초기화</Button>
          </Space>
        </Form.Item>
      </Form>

      <Table<GiroTransaction>
        rowKey={(r) => `${r.jnlDate}-${r.termGroupId}-${r.termId}-${r.jnlTime}`}
        size="small"
        bordered
        loading={loading}
        columns={columns}
        dataSource={rows}
        scroll={{ x: 900 }}
        pagination={{ pageSize: 20, showSizeChanger: true, showTotal: (t) => `총 ${t}건` }}
      />
    </PageTemplate>
  );
};

export default GiroTransactionPage;
