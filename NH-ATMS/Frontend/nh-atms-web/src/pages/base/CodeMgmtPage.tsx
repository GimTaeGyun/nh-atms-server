import React, { useCallback, useEffect, useState } from 'react';
import {
  Button,
  Card,
  Col,
  Form,
  Input,
  InputNumber,
  message,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Table,
} from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { PlusOutlined, EditOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import PageTemplate from '@/components/PageTemplate';
import {
  codeApi,
  type CodeGroup,
  type CodeGroupSaveRequest,
  type CodeItem,
  type CodeItemSaveRequest,
} from '@/api/codeApi';

const LANG_OPTIONS = [
  { value: 'KOR', label: '한국어' },
  { value: 'ENG', label: '영어' },
];

// ── 그룹코드 컬럼 ────────────────────────────────────────────────
const groupColumns: ColumnsType<CodeGroup> = [
  { title: '언어',    dataIndex: 'langCd',    width: 70,  align: 'center' },
  { title: '그룹코드', dataIndex: 'groupCd',   width: 100, align: 'center' },
  { title: '그룹명',  dataIndex: 'groupName',   width: 170, align: 'center' },
];

// ── 코드 컬럼 (작업 컬럼은 렌더 시점에 핸들러 바인딩) ──────────────
const buildCodeColumns = (
  onEdit: (record: CodeItem) => void,
  onDelete: (record: CodeItem) => void,
): ColumnsType<CodeItem> => [
  { title: '언어',   dataIndex: 'langCd',  width: 70,  align: 'center' },
  { title: '코드',   dataIndex: 'code',    width: 120, align: 'center' },
  { title: '코드명', dataIndex: 'name',    width: 200 },
  { title: '순서',   dataIndex: 'orderNo', width: 70,  align: 'center' },
  {
    title: '작업',
    width: 140,
    fixed: 'right',
    render: (_, record) => (
      <Space size="small">
        <Button size="small" icon={<EditOutlined />} onClick={() => onEdit(record)}>
          수정
        </Button>
        <Popconfirm
          title="삭제하시겠습니까?"
          okText="삭제"
          cancelText="취소"
          onConfirm={() => onDelete(record)}
        >
          <Button size="small" danger icon={<DeleteOutlined />}>
            삭제
          </Button>
        </Popconfirm>
      </Space>
    ),
  },
];

// ════════════════════════════════════════════════════════════════
const CodeMgmtPage: React.FC = () => {

  // ── 그룹코드 상태 (좌측) ─────────────────────────────────────
  const [groups, setGroups]               = useState<CodeGroup[]>([]);
  const [groupLoading, setGroupLoading]   = useState(false);
  const [selectedGroup, setSelectedGroup] = useState<CodeGroup | null>(null);

  // ── 코드 상태 (우측) ─────────────────────────────────────────
  const [codes, setCodes]               = useState<CodeItem[]>([]);
  const [codeLoading, setCodeLoading]   = useState(false);

  // ── 그룹코드 모달 ─────────────────────────────────────────────
  const [groupModalOpen, setGroupModalOpen] = useState(false);
  const [editingGroup, setEditingGroup]     = useState<CodeGroup | null>(null);
  const [groupForm] = Form.useForm<CodeGroupSaveRequest>();

  // ── 코드 모달 ─────────────────────────────────────────────────
  const [codeModalOpen, setCodeModalOpen] = useState(false);
  const [editingCode, setEditingCode]     = useState<CodeItem | null>(null);
  const [codeForm] = Form.useForm<CodeItemSaveRequest>();

  // ── fetch 함수 ────────────────────────────────────────────────

  const fetchGroups = async () => {
    setGroupLoading(true);
    try {
      const res = await codeApi.getGroups();
      setGroups(res.data.data ?? []);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '그룹코드 조회 실패');
    } finally {
      setGroupLoading(false);
    }
  };

  // useCallback으로 안정된 참조를 유지해 useEffect 의존성 경고 제거
  const fetchCodes = useCallback(async (langCd: string, groupCd: string) => {
    setCodeLoading(true);
    try {
      const res = await codeApi.getCodes(langCd, groupCd);
      setCodes(res.data.data ?? []);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '코드 목록 조회 실패');
    } finally {
      setCodeLoading(false);
    }
  }, []);

  // 마운트 시 그룹코드 목록 조회
  useEffect(() => { fetchGroups(); }, []);

  // 선택 그룹의 langCd/groupCd 원시값이 바뀔 때만 재조회
  // (객체 참조가 아닌 값 비교 → fetchGroups 재실행 후에도 안정적으로 동작)
  useEffect(() => {
    if (selectedGroup?.langCd && selectedGroup?.groupCd) {
      fetchCodes(selectedGroup.langCd, selectedGroup.groupCd);
    } else {
      setCodes([]);
    }
  }, [selectedGroup?.langCd, selectedGroup?.groupCd, fetchCodes]);

  // ── 그룹코드 모달 핸들러 ──────────────────────────────────────

  const openGroupCreate = () => {
    setEditingGroup(null);
    groupForm.resetFields();
    groupForm.setFieldsValue({ langCd: 'KOR' });
    setGroupModalOpen(true);
  };

  const openGroupEdit = async (record: CodeGroup) => {
    try {
      const res = await codeApi.getOneGroup(record.langCd, record.groupCd);
      setEditingGroup(res.data.data);
      groupForm.setFieldsValue(res.data.data);
      setGroupModalOpen(true);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '그룹코드 조회 실패');
    }
  };

  const handleGroupSubmit = async () => {
    const values = await groupForm.validateFields();
    try {
      if (editingGroup) {
        await codeApi.updateGroup(editingGroup.langCd, editingGroup.groupCd, values);
        message.success('수정되었습니다.');
        // 선택된 그룹이 수정된 경우 selectedGroup 동기화
        if (selectedGroup?.groupCd === editingGroup.groupCd) {
          setSelectedGroup({ ...editingGroup, ...values });
        }
      } else {
        await codeApi.createGroup(values);
        message.success('등록되었습니다.');
      }
      setGroupModalOpen(false);
      await fetchGroups();
    } catch (e: any) {
      if (e?.errorFields) return;
      message.error(e?.response?.data?.message ?? '저장 실패');
    }
  };

  const handleGroupDelete = async (record: CodeGroup) => {
    try {
      await codeApi.removeGroup(record.langCd, record.groupCd);
      message.success('삭제되었습니다.');
      if (selectedGroup?.groupCd === record.groupCd) setSelectedGroup(null);
      await fetchGroups();
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '삭제 실패');
    }
  };

  // ── 코드 모달 핸들러 ─────────────────────────────────────────

  const openCodeCreate = () => {
    setEditingCode(null);
    codeForm.resetFields();
    codeForm.setFieldsValue({
      langCd:  selectedGroup?.langCd  ?? 'KOR',
      groupCd: selectedGroup?.groupCd ?? '',
      orderNo: 1,
    });
    setCodeModalOpen(true);
  };

  const openCodeEdit = async (record: CodeItem) => {
    try {
      const res = await codeApi.getOneCode(record.langCd, record.groupCd, record.code);
      setEditingCode(res.data.data);
      codeForm.setFieldsValue(res.data.data);
      setCodeModalOpen(true);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '코드 조회 실패');
    }
  };

  const handleCodeSubmit = async () => {
    const values = await codeForm.validateFields();
    try {
      if (editingCode) {
        await codeApi.updateCode(editingCode.langCd, editingCode.groupCd, editingCode.code, values);
        message.success('수정되었습니다.');
      } else {
        await codeApi.createCode(values);
        message.success('등록되었습니다.');
      }
      setCodeModalOpen(false);
      if (selectedGroup) await fetchCodes(selectedGroup.langCd, selectedGroup.groupCd);
    } catch (e: any) {
      if (e?.errorFields) return;
      message.error(e?.response?.data?.message ?? '저장 실패');
    }
  };

  const handleCodeDelete = async (record: CodeItem) => {
    try {
      await codeApi.removeCode(record.langCd, record.groupCd, record.code);
      message.success('삭제되었습니다.');
      if (selectedGroup) await fetchCodes(selectedGroup.langCd, selectedGroup.groupCd);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '삭제 실패');
    }
  };

  // ── 렌더 ─────────────────────────────────────────────────────

  return (
    <PageTemplate title="코드관리">
      <Row gutter={12}>

        {/* ── 좌측: 그룹코드 목록 (T_CODEGROUP) ──────────────────── */}
        <Col span={11}>
          <Card
            size="small"
            title="그룹코드 목록"
            extra={
              <Space>
                <Button
                  type="primary"
                  size="small"
                  icon={<PlusOutlined />}
                  onClick={openGroupCreate}
                >
                  그룹 등록
                </Button>
                <Button size="small" icon={<ReloadOutlined />} onClick={fetchGroups}>
                  새로고침
                </Button>
              </Space>
            }
          >
            <Table<CodeGroup>
              size="small"
              bordered
              rowKey={(r) => `${r.langCd}_${r.groupCd}`}
              columns={[
                ...groupColumns,
                {
                  title: '작업',
                  width: 120,
                  fixed: 'right',
                  render: (_, record) => (
                    <Space size="small">
                      <Button
                        size="small"
                        icon={<EditOutlined />}
                        onClick={(e) => { e.stopPropagation(); openGroupEdit(record); }}
                      >
                        수정
                      </Button>
                      <Popconfirm
                        title="삭제하시겠습니까?"
                        okText="삭제"
                        cancelText="취소"
                        onConfirm={() => handleGroupDelete(record)}
                      >
                        <Button
                          size="small"
                          danger
                          icon={<DeleteOutlined />}
                          onClick={(e) => e.stopPropagation()}
                        >
                          삭제
                        </Button>
                      </Popconfirm>
                    </Space>
                  ),
                },
              ]}
              dataSource={groups}
              loading={groupLoading}
              pagination={false}
              scroll={{ y: 'calc(100vh - 280px)', x: 400 }}
              locale={{ emptyText: '등록된 그룹코드가 없습니다.' }}
              onRow={(record) => ({
                onClick: () => setSelectedGroup(record),
                style: {
                  cursor: 'pointer',
                  background:
                    selectedGroup?.groupCd === record.groupCd &&
                    selectedGroup?.langCd  === record.langCd
                      ? '#e6f4ff'
                      : undefined,
                },
              })}
            />
          </Card>
        </Col>

        {/* ── 우측: 코드 목록 (T_CODE) ────────────────────────────── */}
        <Col span={13}>
          <Card
            size="small"
            title={
              selectedGroup
                ? `코드 목록 — ${selectedGroup.groupName} (${selectedGroup.groupCd})`
                : '코드 목록'
            }
            extra={
              <Space>
                <Button
                  type="primary"
                  size="small"
                  icon={<PlusOutlined />}
                  onClick={openCodeCreate}
                  disabled={!selectedGroup}
                >
                  코드 등록
                </Button>
                <Button
                  size="small"
                  icon={<ReloadOutlined />}
                  disabled={!selectedGroup}
                  onClick={() =>
                    selectedGroup && fetchCodes(selectedGroup.langCd, selectedGroup.groupCd)
                  }
                >
                  새로고침
                </Button>
              </Space>
            }
          >
            <Table<CodeItem>
              size="small"
              bordered
              rowKey={(r) => `${r.langCd}_${r.groupCd}_${r.code}`}
              columns={buildCodeColumns(openCodeEdit, handleCodeDelete)}
              dataSource={codes}
              loading={codeLoading}
              pagination={{
                pageSize: 15,
                showSizeChanger: true,
                showTotal: (total) => `총 ${total}건`,
              }}
              scroll={{ x: 700 }}
              locale={{
                emptyText: selectedGroup
                  ? '등록된 코드가 없습니다.'
                  : '좌측에서 그룹코드를 선택하세요.',
              }}
            />
          </Card>
        </Col>

      </Row>

      {/* ── 그룹코드 등록/수정 모달 ───────────────────────────────── */}
      <Modal
        title={
          editingGroup
            ? `그룹코드 수정 (${editingGroup.langCd} / ${editingGroup.groupCd})`
            : '그룹코드 등록'
        }
        open={groupModalOpen}
        onCancel={() => setGroupModalOpen(false)}
        onOk={handleGroupSubmit}
        okText="저장"
        cancelText="취소"
        width={440}
        destroyOnHidden
      >
        <Form form={groupForm} layout="vertical" size="small">
          <Form.Item
            name="langCd"
            label="언어코드"
            rules={[{ required: true, message: '언어코드를 선택하세요' }]}
          >
            <Select options={LANG_OPTIONS} disabled={!!editingGroup} />
          </Form.Item>
          <Form.Item
            name="groupCd"
            label="그룹코드"
            rules={[{ required: true, message: '그룹코드를 입력하세요' }]}
          >
            <Input maxLength={50} disabled={!!editingGroup} placeholder="예) ATM_STATUS" />
          </Form.Item>
          <Form.Item
            name="groupName"
            label="그룹명"
            rules={[{ required: true, message: '그룹명을 입력하세요' }]}
          >
            <Input maxLength={100} placeholder="예) ATM 상태 코드" />
          </Form.Item>
        </Form>
      </Modal>

      {/* ── 코드 등록/수정 모달 ────────────────────────────────────── */}
      <Modal
        title={
          editingCode
            ? `코드 수정 (${editingCode.groupCd} / ${editingCode.code})`
            : '코드 등록'
        }
        open={codeModalOpen}
        onCancel={() => setCodeModalOpen(false)}
        onOk={handleCodeSubmit}
        okText="저장"
        cancelText="취소"
        width={440}
        destroyOnHidden
      >
        <Form form={codeForm} layout="vertical" size="small">
          <Space style={{ width: '100%' }} size="middle">
            <Form.Item name="langCd" label="언어코드" style={{ flex: 1 }}>
              <Select options={LANG_OPTIONS} disabled />
            </Form.Item>
            <Form.Item name="groupCd" label="그룹코드" style={{ flex: 2 }}>
              <Input disabled />
            </Form.Item>
          </Space>
          <Form.Item
            name="code"
            label="코드"
            rules={[{ required: true, message: '코드를 입력하세요' }]}
          >
            <Input maxLength={50} disabled={!!editingCode} placeholder="예) ACTIVE" />
          </Form.Item>
          <Form.Item
            name="name"
            label="코드명"
            rules={[{ required: true, message: '코드명을 입력하세요' }]}
          >
            <Input maxLength={100} placeholder="예) 가동중" />
          </Form.Item>
          <Form.Item name="orderNo" label="순서">
            <InputNumber min={1} max={999} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

    </PageTemplate>
  );
};

export default CodeMgmtPage;
