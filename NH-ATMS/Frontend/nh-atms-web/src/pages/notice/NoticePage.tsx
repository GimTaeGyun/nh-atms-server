import React, { useEffect, useState } from 'react';
import {
  Button,
  Form,
  Input,
  Modal,
  Pagination,
  Popconfirm,
  Space,
  Spin,
  Upload,
  message,
} from 'antd';
import {
  DownOutlined,
  UpOutlined,
  EditOutlined,
  DeleteOutlined,
  DownloadOutlined,
  PaperClipOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import type { UploadFile } from 'antd/es/upload/interface';
import { useAuthStore } from '@/stores/useAuthStore';
import { noticeApi, type Notice, type AttachedFile } from '@/api/noticeApi';

const CAT_CD                    = '1';
const PAGE_SIZE                 = 5;
const DEPT_CODE_VALUE           = '00001';        // TODO: 코드관리 부서코드 연동 후 실제 코드로 교체 — DB 저장값
const DEPT_CODE_DISPLAY_LABEL   = '부서코드삽입예정'; // TODO: 코드관리 연동 후 코드→명칭 변환으로 교체 — 화면 표시값
const ALLOWED_EXT = ['pdf', 'hwp', 'hwpx', 'xlsx', 'xls', 'docx', 'zip',
                     'jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'];

/* YYYYMMDD(HH24MISS) → YYYY-MM-DD */
const fmtDate = (d?: string) =>
  d && d.length >= 8 ? `${d.slice(0, 4)}-${d.slice(4, 6)}-${d.slice(6, 8)}` : (d ?? '');

/* 7일 이내 NEW 여부 */
const isNew = (enterDate?: string) => {
  if (!enterDate || enterDate.length < 8) return false;
  const d = new Date(`${enterDate.slice(0, 4)}-${enterDate.slice(4, 6)}-${enterDate.slice(6, 8)}`);
  return (Date.now() - d.getTime()) / 86400000 <= 7;
};

const fileSizeStr = (bytes: number) =>
  bytes < 1024 * 1024
    ? `${(bytes / 1024).toFixed(1)} KB`
    : `${(bytes / 1024 / 1024).toFixed(1)} MB`;

/* ── 등록/수정 모달 ─────────────────────────────────────────── */
interface EditModalProps {
  open: boolean;
  mode: 'create' | 'edit';
  initial?: Notice;
  existingFiles?: AttachedFile[];
  onClose: () => void;
  onSaved: () => void;
}

const EditModal: React.FC<EditModalProps> = ({
  open, mode, initial, existingFiles, onClose, onSaved,
}) => {
  const [form]            = Form.useForm();
  const [files, setFiles] = useState<UploadFile[]>([]);
  const [saving, setSaving] = useState(false);
  const user = useAuthStore((s) => s.user);

  useEffect(() => {
    if (!open) return;
    form.setFieldsValue(
      mode === 'edit' && initial
        ? { subject: initial.subject, content: initial.content }
        : { subject: '', content: '' },
    );
    setFiles([]);
  }, [open, mode, initial, form]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setSaving(true);
      const rawFiles = files.map((f) => f.originFileObj as File).filter(Boolean);

      if (mode === 'create') {
        await noticeApi.create(
          {
            catCd: CAT_CD,
            ...values,
            enterUserId:      user?.userId ?? '',
            enterUserName:    user?.userName ?? '',
            enterTermGroupId: DEPT_CODE_VALUE,
            useYn: 'Y',
          },
          rawFiles,
        );
        message.success('공지사항이 등록되었습니다.');
      } else {
        await noticeApi.update(initial!.catCd, initial!.seqNo, { ...values }, rawFiles);
        message.success('공지사항이 수정되었습니다.');
      }
      onSaved();
    } catch (e: any) {
      if (e?.response) message.error(e.response.data?.message ?? '저장 실패');
    } finally {
      setSaving(false);
    }
  };

  const beforeUpload = (file: File) => {
    if (files.length >= 3) {
      message.warning('최대 3개까지 첨부 가능합니다.');
      return Upload.LIST_IGNORE;
    }
    const ext = file.name.split('.').pop()?.toLowerCase() ?? '';
    if (!ALLOWED_EXT.includes(ext)) {
      message.error(`허용되지 않는 파일 형식입니다. (${ALLOWED_EXT.join(', ')})`);
      return Upload.LIST_IGNORE;
    }
    return false;
  };

  return (
    <Modal
      title={mode === 'create' ? '공지사항 등록' : '공지사항 수정'}
      open={open} onCancel={onClose} onOk={handleOk}
      okText={mode === 'create' ? '등록' : '저장'} cancelText="취소"
      confirmLoading={saving} width={680} destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Form.Item name="subject" label="제목" rules={[{ required: true, message: '제목을 입력하세요.' }]}>
          <Input maxLength={200} />
        </Form.Item>
        <Form.Item name="content" label="내용" rules={[{ required: true, message: '내용을 입력하세요.' }]}>
          <Input.TextArea rows={8} />
        </Form.Item>
        <Form.Item label="첨부파일 (최대 3개 / PDF·HWP·XLSX·DOCX·ZIP·JPG·PNG 등)">
          {mode === 'edit' && existingFiles && existingFiles.length > 0 && (
            <div style={{ marginBottom: 8 }}>
              {existingFiles.map((f) => (
                <div key={f.fileNo} style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
                  <PaperClipOutlined style={{ color: '#1677ff' }} />
                  <span style={{ fontSize: 13 }}>{f.attachedFileName}</span>
                  <span style={{ color: '#aaa', fontSize: 12 }}>({fileSizeStr(f.attachedFileSize)})</span>
                </div>
              ))}
            </div>
          )}
          <Upload
            fileList={files}
            beforeUpload={beforeUpload}
            onChange={({ fileList }) => setFiles(fileList)}
            multiple
          >
            <Button icon={<PlusOutlined />}>파일 선택</Button>
          </Upload>
        </Form.Item>
      </Form>
    </Modal>
  );
};

/* ── 메인 페이지 ─────────────────────────────────────────────── */
const NoticePage: React.FC = () => {
  const [notices, setNotices]     = useState<Notice[]>([]);
  const [total, setTotal]         = useState(0);
  const [page, setPage]           = useState(1);
  const [loading, setLoading]     = useState(false);

  const [openSeqNo, setOpenSeqNo]       = useState<number | null>(null);
  const [hoverSeqNo, setHoverSeqNo]     = useState<number | null>(null);
  const [hitMap, setHitMap]             = useState<Record<number, number>>({});
  const [fileMap, setFileMap]           = useState<Record<number, AttachedFile[]>>({});
  const [fileLoading, setFileLoading]   = useState(false);

  const [editOpen, setEditOpen]         = useState(false);
  const [editMode, setEditMode]         = useState<'create' | 'edit'>('create');
  const [editTarget, setEditTarget]     = useState<Notice | undefined>();

  const user = useAuthStore((s) => s.user);

  /* ── 목록 조회 ──────────────────────────────────────────────── */
  const fetchList = async (p = page) => {
    setLoading(true);
    try {
      const res  = await noticeApi.getList({ catCd: CAT_CD, page: p, size: PAGE_SIZE });
      const data = res.data.data;
      setNotices(data?.content ?? []);
      setTotal(data?.totalCount ?? 0);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '목록 조회 실패');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchList(1); }, []); // eslint-disable-line

  const handlePageChange = (p: number) => {
    setPage(p);
    setOpenSeqNo(null);
    fetchList(p);
  };

  /* ── 아코디언 토글 ──────────────────────────────────────────── */
  const toggleAccordion = async (notice: Notice) => {
    const { catCd, seqNo } = notice;

    if (openSeqNo === seqNo) { setOpenSeqNo(null); return; }
    setOpenSeqNo(seqNo);

    /* 조회수 증가 */
    try {
      const res = await noticeApi.increaseHit(catCd, seqNo);
      setHitMap((prev) => ({ ...prev, [seqNo]: res.data.data.hit }));
    } catch { /* 무시 */ }

    /* 첨부파일 — 캐시 없고 FLAG = Y 일 때만 조회 */
    if (fileMap[seqNo] === undefined && notice.attachedFileFlag === 'Y') {
      setFileLoading(true);
      try {
        const res = await noticeApi.getFiles(catCd, seqNo);
        setFileMap((prev) => ({ ...prev, [seqNo]: res.data.data ?? [] }));
      } catch {
        setFileMap((prev) => ({ ...prev, [seqNo]: [] }));
      } finally {
        setFileLoading(false);
      }
    }
  };

  /* ── 삭제 ────────────────────────────────────────────────────── */
  const handleDelete = async (catCd: string, seqNo: number) => {
    try {
      await noticeApi.delete(catCd, seqNo);
      message.success('삭제되었습니다.');
      setOpenSeqNo(null);
      setFileMap((prev) => { const n = { ...prev }; delete n[seqNo]; return n; });
      fetchList(page);
    } catch (e: any) {
      message.error(e?.response?.data?.message ?? '삭제 실패');
    }
  };

  const openCreate = () => {
    setEditMode('create');
    setEditTarget(undefined);
    setEditOpen(true);
  };
  const openEdit = (n: Notice) => {
    setEditMode('edit');
    setEditTarget(n);
    setEditOpen(true);
  };

  /* ── 렌더 ───────────────────────────────────────────────────── */
  return (
    <div style={{ padding: '16px 24px' }}>
      {/* 흰 카드 */}
      <div style={{
        position: 'relative',
        background: '#fff',
        borderRadius: 12,
        boxShadow: '0 2px 12px rgba(0,0,0,0.08)',
        padding: '32px 40px 28px',
        minHeight: 600,
      }}>
        {/* 신규등록 버튼 */}
        <div style={{ position: 'absolute', top: 24, right: 28 }}>
          <Button icon={<PlusOutlined />} onClick={openCreate}>신규등록 +</Button>
        </div>

        {/* 제목 */}
        <div style={{ textAlign: 'center', marginBottom: 28 }}>
          <h2 style={{ fontSize: 26, fontWeight: 700, margin: 0 }}>공지사항</h2>
          <p style={{ color: '#1677ff', margin: '6px 0 0', fontSize: 13 }}>
            NH농협 ATMS 소식을 전해드립니다
          </p>
        </div>

        {/* 총건수 */}
        <div style={{ fontWeight: 600, marginBottom: 12 }}>전체 {total} 건</div>

        {/* 아코디언 목록 */}
        <Spin spinning={loading}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {notices.map((notice) => {
              const isOpen  = openSeqNo === notice.seqNo;
              const isHover = hoverSeqNo === notice.seqNo && !isOpen;
              const hit     = hitMap[notice.seqNo] ?? notice.hit ?? 0;
              const isMine  = notice.enterUserId === user?.userId;
              const files   = fileMap[notice.seqNo] ?? [];

              return (
                <div
                  key={notice.seqNo}
                  onMouseEnter={() => setHoverSeqNo(notice.seqNo)}
                  onMouseLeave={() => setHoverSeqNo(null)}
                  style={{
                    border: isHover
                      ? '1.5px solid #4096ff'
                      : '1px solid #e8e8e8',
                    borderRadius: 8,
                    background: '#fff',
                    overflow: 'hidden',
                    transition: 'border-color 0.15s, box-shadow 0.15s',
                    boxShadow: isHover
                      ? '0 2px 10px rgba(22,119,255,0.12)'
                      : 'none',
                  }}
                >
                  {/* 헤더 */}
                  <div
                    style={{
                      display: 'flex', alignItems: 'center',
                      padding: '16px 20px', cursor: 'pointer', userSelect: 'none',
                    }}
                    onClick={() => toggleAccordion(notice)}
                  >
                    <span style={{
                      width: 32, flexShrink: 0,
                      color: isHover ? '#4096ff' : '#555',
                      fontSize: 14, fontWeight: 800,
                      transition: 'color 0.15s',
                    }}>
                      {notice.seqNo}
                    </span>
                    <div style={{ flex: 1 }}>
                      <div style={{
                        fontWeight: isHover ? 700 : 600,
                        fontSize: 15,
                        color: isHover ? '#1677ff' : '#1a1a1a',
                        letterSpacing: '-0.2px',
                        transition: 'color 0.15s',
                      }}>
                        {isNew(notice.enterDate) && (
                          <span style={{
                            background: '#1677ff', color: '#fff', fontSize: 10,
                            borderRadius: 3, padding: '1px 6px', marginRight: 7,
                            fontWeight: 700, letterSpacing: 0,
                          }}>NEW</span>
                        )}
                        [공지] {notice.subject}
                      </div>
                      <div style={{
                        fontSize: 12,
                        color: '#555',
                        marginTop: 4,
                        fontWeight: 700,
                      }}>
                        {fmtDate(notice.enterDate)}
                      </div>
                    </div>
                    {isOpen
                      ? <UpOutlined style={{ color: '#888', fontSize: 13 }} />
                      : <DownOutlined style={{ color: isHover ? '#4096ff' : '#ccc', fontSize: 13, transition: 'color 0.15s' }} />}
                  </div>

                  {/* 펼침 본문 */}
                  {isOpen && (
                    <div style={{ borderTop: '1px solid #f0f0f0', padding: '14px 20px 18px' }}>
                      {/* 수정/삭제 | 메타 */}
                      <div style={{
                        display: 'flex', justifyContent: 'space-between',
                        alignItems: 'center', marginBottom: 14,
                      }}>
                        <Space size={6}>
                          {isMine && (
                            <>
                              <Button
                                size="small" icon={<EditOutlined />}
                                onClick={() => openEdit(notice)}
                              >
                                수정
                              </Button>
                              <Popconfirm
                                title="공지사항을 삭제하시겠습니까?"
                                onConfirm={() => handleDelete(notice.catCd, notice.seqNo)}
                                okText="삭제" cancelText="취소"
                              >
                                <Button size="small" danger icon={<DeleteOutlined />}>삭제</Button>
                              </Popconfirm>
                            </>
                          )}
                        </Space>
                        <Space size={14} style={{ fontSize: 12, color: '#555', fontWeight: 500 }}>
                          <span>
                            등록자 <b style={{ color: '#1a1a1a', fontWeight: 700 }}>{notice.enterUserName || notice.enterUserId}</b>
                          </span>
                          {notice.enterTermGroupId && (
                            <span>부서 <b style={{ color: '#1a1a1a', fontWeight: 700 }}>{DEPT_CODE_DISPLAY_LABEL}</b></span>
                          )}
                          <span>조회수 <b style={{ color: '#1677ff', fontWeight: 700 }}>{hit}</b></span>
                        </Space>
                      </div>

                      {/* 본문 */}
                      <div style={{
                        whiteSpace: 'pre-wrap', lineHeight: 1.9,
                        color: '#222', minHeight: 40,
                        fontSize: 14, fontWeight: 500,
                        letterSpacing: '-0.1px',
                      }}>
                        {notice.content}
                      </div>

                      {/* 첨부파일 */}
                      {notice.attachedFileFlag === 'Y' && (
                        <Spin spinning={fileLoading}>
                          {files.length > 0 && (
                            <div style={{
                              marginTop: 16, padding: '10px 14px',
                              background: '#f8f8f8', borderRadius: 6,
                              border: '1px solid #efefef',
                            }}>
                              {files.map((f) => (
                                <div
                                  key={f.fileNo}
                                  style={{
                                    display: 'flex', alignItems: 'center',
                                    justifyContent: 'space-between', padding: '5px 0',
                                  }}
                                >
                                  <Space>
                                    <span style={{ color: '#1677ff', fontWeight: 700, fontSize: 13 }}>
                                      | 첨부파일
                                    </span>
                                    <span
                                      style={{ color: '#1677ff', cursor: 'pointer' }}
                                      onClick={() => noticeApi.download(f.catCd, f.seqNo, f.fileNo, f.attachedFileName)}
                                    >
                                      {f.attachedFileName}
                                    </span>
                                    <span style={{ color: '#bbb', fontSize: 12 }}>
                                      ({fileSizeStr(f.attachedFileSize)})
                                    </span>
                                  </Space>
                                  <Button
                                    size="small" icon={<DownloadOutlined />}
                                    onClick={() => noticeApi.download(f.catCd, f.seqNo, f.fileNo, f.attachedFileName)}
                                  >
                                    다운로드
                                  </Button>
                                </div>
                              ))}
                            </div>
                          )}
                        </Spin>
                      )}
                    </div>
                  )}
                </div>
              );
            })}

            {!loading && notices.length === 0 && (
              <div style={{ textAlign: 'center', padding: '60px 0', color: '#bbb' }}>
                등록된 공지사항이 없습니다.
              </div>
            )}
          </div>
        </Spin>

        {/* 페이지네이션 */}
        <div style={{ display: 'flex', justifyContent: 'center', marginTop: 32 }}>
          <Pagination
            current={page} pageSize={PAGE_SIZE} total={total}
            onChange={handlePageChange} showSizeChanger={false}
            itemRender={(_, type, el) => {
              if (type === 'prev') return <span style={{ fontSize: 13 }}>{'< 이전'}</span>;
              if (type === 'next') return <span style={{ fontSize: 13 }}>{'다음 >'}</span>;
              return el;
            }}
          />
        </div>
      </div>

      {/* 등록/수정 모달 */}
      <EditModal
        open={editOpen}
        mode={editMode}
        initial={editTarget}
        existingFiles={editTarget ? fileMap[editTarget.seqNo] : undefined}
        onClose={() => setEditOpen(false)}
        onSaved={() => { setEditOpen(false); fetchList(page); }}
      />
    </div>
  );
};

export default NoticePage;
