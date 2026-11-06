import React, { useEffect, useState } from 'react';
import { Button, DatePicker, Form, Input, Select, Space, Table, Tag, message } from 'antd';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import dayjs, { type Dayjs } from 'dayjs';
import PageTemplate from '@/components/PageTemplate';
import { loginHistApi, type LoginHist } from '@/api/loginHistApi';

const { RangePicker } = DatePicker;

interface SearchValues {
  userId?: string;
  loginResult?: string;
  range?: [Dayjs, Dayjs];
}

const LoginHistPage: React.FC = () => {
  const [form] = Form.useForm<SearchValues>();
  const [rows, setRows] = useState<LoginHist[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(20);

  const fetchList = async (p = page, s = size) => {
    const v = form.getFieldsValue();
    setLoading(true);
    try {
      const res = await loginHistApi.getList({
        userId: v.userId,
        loginResult: v.loginResult,
        fromDt: v.range?.[0]?.format('YYYY-MM-DD'),
        toDt: v.range?.[1]?.format('YYYY-MM-DD'),
        page: p,
        size: s,
      });
      const data = res.data.data;
      setRows(data.content);
      setTotal(data.totalCount);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '조회 실패');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // 초기: 최근 7일 기본값
    form.setFieldsValue({ range: [dayjs().subtract(7, 'day'), dayjs()] });
    fetchList(1, size);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleSearch = () => {
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

  const columns: ColumnsType<LoginHist> = [
    { title: 'SEQ', dataIndex: 'loginSeq', width: 90, align: 'right' },
    { title: '사용자 ID', dataIndex: 'userId', width: 140 },
    {
      title: '결과',
      dataIndex: 'loginResult',
      width: 80,
      align: 'center',
      render: (v) =>
        v === 'S' ? <Tag color="green">성공</Tag> : <Tag color="red">실패</Tag>,
    },
    {
      title: '로그인 일시',
      dataIndex: 'loginDt',
      width: 180,
      render: (v) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    {
      title: '로그아웃 일시',
      dataIndex: 'logoutDt',
      width: 180,
      render: (v) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
    { title: 'IP', dataIndex: 'loginIp', width: 140 },
    { title: '실패사유', dataIndex: 'failReason', width: 200, render: (v) => v || '-' },
    { title: 'User-Agent', dataIndex: 'userAgent', ellipsis: true },
  ];

  return (
    <PageTemplate title="로그인이력">
      <div className="search-panel" style={{ marginBottom: 8 }}>
        <Form form={form} layout="inline" onFinish={handleSearch} style={{ gap: 8, flexWrap: 'wrap' }}>
          <Form.Item name="userId" label="사용자 ID">
            <Input size="small" placeholder="ID 검색" style={{ width: 160 }} allowClear />
          </Form.Item>
          <Form.Item name="loginResult" label="결과">
            <Select
              size="small"
              placeholder="전체"
              style={{ width: 120 }}
              allowClear
              options={[
                { value: 'S', label: '성공' },
                { value: 'F', label: '실패' },
              ]}
            />
          </Form.Item>
          <Form.Item name="range" label="기간">
            <RangePicker size="small" />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" size="small" icon={<SearchOutlined />} htmlType="submit" loading={loading}>
                조회
              </Button>
              <Button
                size="small"
                icon={<ReloadOutlined />}
                onClick={() => {
                  form.resetFields();
                  form.setFieldsValue({ range: [dayjs().subtract(7, 'day'), dayjs()] });
                  setPage(1);
                  fetchList(1, size);
                }}
              >
                초기화
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </div>

      <Table<LoginHist>
        columns={columns}
        dataSource={rows}
        loading={loading}
        size="small"
        bordered
        rowKey="loginSeq"
        scroll={{ x: 1300 }}
        pagination={{
          current: page,
          pageSize: size,
          total,
          showSizeChanger: true,
          pageSizeOptions: ['10', '20', '50', '100'],
          showTotal: (t, r) => `${r[0]}-${r[1]} / 총 ${t}건`,
        }}
        onChange={handleTableChange}
      />
    </PageTemplate>
  );
};

export default LoginHistPage;
