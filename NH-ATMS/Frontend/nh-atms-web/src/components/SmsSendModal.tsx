import React, { useEffect, useState } from 'react';
import { Descriptions, Form, Input, Modal, message } from 'antd';
import { smsApi } from '@/api/smsApi';
import { useBranchStore } from '@/stores/useBranchStore';

/** SMS 전송 대상 기기 */
export interface SmsTarget {
  termGroupId: string;
  termId: string;
}

interface SmsSendModalProps {
  open: boolean;
  target: SmsTarget | null;
  onClose: () => void;
  onSuccess?: () => void;
}

interface FormValues {
  recvPhone: string;
  sendPhone: string;
  msgContent: string;
}

const errMsgOf = (e: unknown, fallback: string) =>
  (e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? fallback;

/** 휴대폰/전화번호 형식 (숫자/하이픈) */
const PHONE_RULE = {
  pattern: /^[0-9-]{9,20}$/,
  message: '숫자와 하이픈만 입력하세요.',
};

/**
 * SMS 전송 팝업.
 * 기기유지보수접수 화면에서 사용.
 */
const SmsSendModal: React.FC<SmsSendModalProps> = ({ open, target, onClose, onSuccess }) => {
  const [form] = Form.useForm<FormValues>();
  const [submitting, setSubmitting] = useState(false);
  const branchLabel = useBranchStore((s) => s.label);

  useEffect(() => {
    if (open) form.resetFields();
  }, [open, form]);

  const handleOk = async () => {
    const v = await form.validateFields();
    setSubmitting(true);
    try {
      await smsApi.send({
        termGroupId: target?.termGroupId,
        termId: target?.termId,
        recvPhone: v.recvPhone,
        sendPhone: v.sendPhone,
        msgContent: v.msgContent,
      });
      message.success('SMS 전송이 등록되었습니다.');
      onSuccess?.();
      onClose();
    } catch (e: unknown) {
      message.error(errMsgOf(e, 'SMS 전송 실패'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title="SMS 전송"
      open={open}
      onOk={handleOk}
      onCancel={onClose}
      okText="전송"
      cancelText="취소"
      confirmLoading={submitting}
      width={520}
      destroyOnClose
    >
      <Descriptions size="small" bordered column={1} style={{ marginBottom: 16 }}>
        <Descriptions.Item label="금고(영업점)">
          {target ? branchLabel(target.termGroupId) : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="기번(단말)">{target?.termId ?? '-'}</Descriptions.Item>
      </Descriptions>

      <Form form={form} layout="vertical">
        <Form.Item name="recvPhone" label="휴대폰번호" rules={[{ required: true, message: '휴대폰번호를 입력하세요.' }, PHONE_RULE]}>
          <Input placeholder="010-1234-5678" allowClear />
        </Form.Item>
        <Form.Item name="sendPhone" label="회신번호" rules={[{ required: true, message: '회신번호를 입력하세요.' }, PHONE_RULE]}>
          <Input placeholder="02-123-4567" allowClear />
        </Form.Item>
        <Form.Item name="msgContent" label="전송내용" rules={[{ required: true, message: '전송내용을 입력하세요.' }]}>
          <Input.TextArea rows={4} maxLength={500} showCount placeholder="전송할 메시지를 입력하세요." />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default SmsSendModal;
