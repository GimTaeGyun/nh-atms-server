import React, { useEffect, useState } from 'react';
import { Descriptions, Form, Input, Modal, Select, message } from 'antd';
import { faultMgmtApi } from '@/api/faultMgmtApi';
import { useBranchStore } from '@/stores/useBranchStore';

/** 장애접수 대상 기기 */
export interface FaultTarget {
  termGroupId: string;
  termId: string;
}

interface FaultReceiptModalProps {
  open: boolean;
  target: FaultTarget | null;
  onClose: () => void;
  /** 접수 성공 시 호출 (목록 새로고침 등) */
  onSuccess?: () => void;
}

const FAULT_TYPE_OPTIONS = [
  { value: 'HW', label: '하드웨어 장애' },
  { value: 'SW', label: '소프트웨어 장애' },
  { value: 'NW', label: '네트워크 장애' },
  { value: 'CASH', label: '현금/시재 장애' },
  { value: 'ETC', label: '기타' },
];

const VENDOR_OPTIONS = [
  { value: 'V001', label: '효성티앤에스' },
  { value: 'V002', label: '청호이지캐쉬' },
  { value: 'V003', label: 'LG CNS' },
  { value: 'V099', label: '기타 협력업체' },
];

interface FormValues {
  faultType?: string;
  errCd?: string;
  vendorCd?: string;
  faultDesc?: string;
}

const errMsgOf = (e: unknown, fallback: string) =>
  (e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? fallback;

/**
 * 기기장애접수(출동요청) 팝업.
 * 실시간모니터링 / 기기유지보수접수 화면에서 공통 사용.
 */
const FaultReceiptModal: React.FC<FaultReceiptModalProps> = ({ open, target, onClose, onSuccess }) => {
  const [form] = Form.useForm<FormValues>();
  const [submitting, setSubmitting] = useState(false);
  const branchLabel = useBranchStore((s) => s.label);

  useEffect(() => {
    if (open) form.resetFields();
  }, [open, form]);

  const handleOk = async () => {
    if (!target) return;
    const v = await form.validateFields();
    setSubmitting(true);
    try {
      await faultMgmtApi.receive({
        termGroupId: target.termGroupId,
        termId: target.termId,
        faultType: v.faultType,
        errCd: v.errCd,
        vendorCd: v.vendorCd,
        faultDesc: v.faultDesc,
      });
      message.success('장애접수가 등록되었습니다.');
      onSuccess?.();
      onClose();
    } catch (e: unknown) {
      message.error(errMsgOf(e, '장애접수 실패'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title="기기장애접수"
      open={open}
      onOk={handleOk}
      onCancel={onClose}
      okText="장애접수"
      cancelText="취소"
      confirmLoading={submitting}
      width={560}
      destroyOnClose
    >
      <Descriptions size="small" bordered column={1} style={{ marginBottom: 16 }}>
        <Descriptions.Item label="금고(영업점)">
          {target ? branchLabel(target.termGroupId) : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="기번(단말)">{target?.termId ?? '-'}</Descriptions.Item>
      </Descriptions>

      <Form form={form} layout="vertical">
        <Form.Item name="faultType" label="출동요청사유" rules={[{ required: true, message: '사유를 선택하세요.' }]}>
          <Select placeholder="장애 유형 선택" options={FAULT_TYPE_OPTIONS} />
        </Form.Item>
        <Form.Item name="errCd" label="장애코드">
          <Input placeholder="예: ERR001 (선택)" allowClear />
        </Form.Item>
        <Form.Item name="vendorCd" label="출동업체">
          <Select placeholder="협력업체 선택 (선택)" options={VENDOR_OPTIONS} allowClear />
        </Form.Item>
        <Form.Item name="faultDesc" label="출동요청내용" rules={[{ required: true, message: '내용을 입력하세요.' }]}>
          <Input.TextArea rows={4} maxLength={500} showCount placeholder="장애 증상 및 요청 사항을 입력하세요." />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default FaultReceiptModal;
