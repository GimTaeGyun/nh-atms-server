import React, { useEffect, useState } from 'react';
import {
  DatePicker,
  Form,
  Input,
  InputNumber,
  Modal,
  Radio,
  Select,
  TimePicker,
  message,
} from 'antd';
import dayjs from 'dayjs';
import { useCodeStore } from '@/stores/useCodeStore';
import { atmModelApi, type AtmModel } from '@/api/atmModelApi';
import {
  downloadApi,
  type DownloadPackageSaveRequest,
  type PackageType,
} from '@/api/downloadApi';

const ATM_CO_GROUP = '050'; // 기기업체

const errMsgOf = (e: unknown, fallback: string) =>
  (e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? fallback;

interface Props {
  open: boolean;
  onClose: () => void;
  onSaved: () => void;
}

interface FormValues {
  pkgType: PackageType;
  atmCoCd: string;
  modelCd: string;
  downVer: string;
  pkgName?: string;
  fileName: string;
  fileSize?: number;
  filePath?: string;
  downloadPath?: string;
  execCmd?: string;
  applyDate?: dayjs.Dayjs;
  applyTime?: dayjs.Dayjs;
  crcValue?: string;
}

/**
 * 다운로드 패키지 등록 모달.
 * (구)MG 의 3종 등록 팝업(AP일반/일반/AP)을 단일 화면으로 통합한다.
 * 유형(AP/일반/매체)은 상단 라디오 하나로만 구분한다.
 */
const DownloadPackageModal: React.FC<Props> = ({ open, onClose, onSaved }) => {
  const [form] = Form.useForm<FormValues>();
  const [saving, setSaving] = useState(false);
  const [models, setModels] = useState<AtmModel[]>([]);

  const { ensure, maps } = useCodeStore();
  const atmCoCd = Form.useWatch('atmCoCd', form);

  useEffect(() => {
    ensure(ATM_CO_GROUP);
  }, [ensure]);

  useEffect(() => {
    if (open) {
      form.resetFields();
      form.setFieldsValue({ pkgType: 'A', downloadPath: 'D:\\LGDOWN' });
      setModels([]);
    }
  }, [open, form]);

  // 업체 선택 시 모델 목록 로드
  useEffect(() => {
    if (!atmCoCd) {
      setModels([]);
      return;
    }
    (async () => {
      try {
        const res = await atmModelApi.getList({ atmCoCd, useYn: 'Y', page: 1, size: 500 });
        setModels(res.data.data.content);
      } catch {
        setModels([]);
      }
    })();
  }, [atmCoCd]);

  const atmCoOpts = Object.entries(maps[ATM_CO_GROUP] ?? {}).map(([code, nm]) => ({
    value: code,
    label: `${nm}(${code})`,
  }));

  const handleModelChange = (modelCd: string) => {
    const m = models.find((x) => x.modelCd === modelCd);
    if (m?.lastAppVer) form.setFieldValue('downVer', m.lastAppVer);
    if (m?.downloadPath) form.setFieldValue('downloadPath', m.downloadPath);
    if (m?.exeFilePath) form.setFieldValue('execCmd', m.exeFilePath);
  };

  const handleSubmit = async () => {
    try {
      const v = await form.validateFields();
      const body: DownloadPackageSaveRequest = {
        pkgName: v.pkgName,
        pkgType: v.pkgType,
        atmCoCd: v.atmCoCd,
        modelCd: v.modelCd,
        downVer: v.downVer,
        fileName: v.fileName,
        fileSize: v.fileSize,
        filePath: v.filePath,
        downloadPath: v.downloadPath,
        execCmd: v.execCmd,
        applyDate: v.applyDate ? v.applyDate.format('YYYYMMDD') : undefined,
        applyTime: v.applyTime ? v.applyTime.format('HHmm') : undefined,
        crcValue: v.crcValue,
      };
      setSaving(true);
      await downloadApi.registerPackage(body);
      message.success('패키지가 등록되었습니다. (결재 대기)');
      onSaved();
      onClose();
    } catch (e) {
      if ((e as { errorFields?: unknown }).errorFields) return; // 폼 검증 에러
      message.error(errMsgOf(e, '패키지 등록 실패'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      title="다운로드 패키지 등록"
      open={open}
      onCancel={onClose}
      onOk={handleSubmit}
      confirmLoading={saving}
      okText="등록"
      cancelText="취소"
      width={620}
      destroyOnClose
    >
      <Form form={form} layout="vertical" requiredMark>
        <Form.Item
          name="pkgType"
          label="유형"
          rules={[{ required: true, message: '유형을 선택하세요.' }]}
          tooltip="AP=응용프로그램, 일반=일반 다운로드, 매체=매체(이미지 등) 파일"
        >
          <Radio.Group>
            <Radio.Button value="A">AP</Radio.Button>
            <Radio.Button value="G">일반</Radio.Button>
            <Radio.Button value="M">매체</Radio.Button>
          </Radio.Group>
        </Form.Item>

        <Form.Item
          name="atmCoCd"
          label="기기업체"
          rules={[{ required: true, message: '기기업체를 선택하세요.' }]}
        >
          <Select
            placeholder="기기업체 선택"
            options={atmCoOpts}
            showSearch
            optionFilterProp="label"
            onChange={() => form.setFieldValue('modelCd', undefined)}
          />
        </Form.Item>

        <Form.Item
          name="modelCd"
          label="모델"
          rules={[{ required: true, message: '모델을 선택하세요.' }]}
        >
          <Select
            placeholder={atmCoCd ? '모델 선택' : '기기업체를 먼저 선택하세요'}
            disabled={!atmCoCd}
            options={models.map((m) => ({ value: m.modelCd, label: `${m.modelName}(${m.modelCd})` }))}
            showSearch
            optionFilterProp="label"
            onChange={handleModelChange}
          />
        </Form.Item>

        <Form.Item
          name="downVer"
          label="다운로드버전"
          rules={[{ required: true, message: '다운로드버전을 입력하세요.' }]}
          tooltip="10자리 = 업체코드(2) + 모델코드(3) + 일련번호(5)"
        >
          <Input placeholder="예) 1440900081" maxLength={10} />
        </Form.Item>

        <Form.Item name="pkgName" label="패키지명">
          <Input placeholder="운영자 식별용 명칭(선택)" maxLength={100} />
        </Form.Item>

        <Form.Item
          name="fileName"
          label="파일명"
          rules={[{ required: true, message: '파일명을 입력하세요.' }]}
        >
          <Input placeholder="예) 1440900081.zip" maxLength={200} />
        </Form.Item>

        <Form.Item name="fileSize" label="파일크기(byte)">
          <InputNumber style={{ width: '100%' }} min={0} placeholder="선택" />
        </Form.Item>

        <Form.Item name="filePath" label="서버 보관 경로">
          <Input placeholder="서버 내 파일 보관 경로(선택)" maxLength={300} />
        </Form.Item>

        <Form.Item
          name="downloadPath"
          label="기기내 다운로드 경로"
          rules={[{ required: true, message: '다운로드 경로를 입력하세요.' }]}
        >
          <Input placeholder="D:\LGDOWN" maxLength={300} />
        </Form.Item>

        <Form.Item name="execCmd" label="실행 CMD">
          <Input placeholder="적용 실행 명령(선택)" maxLength={500} />
        </Form.Item>

        <Form.Item label="적용 예약" style={{ marginBottom: 0 }}>
          <Form.Item name="applyDate" style={{ display: 'inline-block', width: '50%', paddingRight: 8 }}>
            <DatePicker style={{ width: '100%' }} placeholder="적용일(선택)" />
          </Form.Item>
          <Form.Item name="applyTime" style={{ display: 'inline-block', width: '50%' }}>
            <TimePicker style={{ width: '100%' }} format="HH:mm" placeholder="적용시각(선택)" />
          </Form.Item>
        </Form.Item>

        <Form.Item name="crcValue" label="CRC">
          <Input placeholder="파일 무결성 CRC(선택)" maxLength={20} />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default DownloadPackageModal;
