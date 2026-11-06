import React, { useEffect, useRef, useState } from 'react';
import { Empty, Image, Space, Spin, Tag, Typography } from 'antd';
import { transactionJournalApi, type JournalImage } from '@/api/transactionJournalApi';

const IMAGE_TYPE_LABEL: Record<string, string> = {
  '1': '카드',
  '2': '통장',
  '3': '명세표',
};

interface LoadedImage extends JournalImage {
  url?: string;
  error?: boolean;
}

interface Props {
  images: JournalImage[];
}

/**
 * 저널 첨부 이미지 미리보기
 *  - 메타데이터(imageType/imagePath)만 받아 백엔드(/api/file/journal-image/download)에서
 *    실제 JPEG 바이너리를 blob 으로 받아 미리보기한다.
 *  - 언마운트/이미지 교체 시 ObjectURL 을 해제한다.
 */
const JournalImageGallery: React.FC<Props> = ({ images }) => {
  const [items, setItems] = useState<LoadedImage[]>([]);
  const [loading, setLoading] = useState(false);
  const urlsRef = useRef<string[]>([]);

  useEffect(() => {
    let cancelled = false;
    // 이전 ObjectURL 정리
    urlsRef.current.forEach((u) => URL.revokeObjectURL(u));
    urlsRef.current = [];

    if (!images?.length) {
      setItems([]);
      return;
    }

    setLoading(true);
    (async () => {
      const loaded = await Promise.all(
        images.map(async (img): Promise<LoadedImage> => {
          try {
            const res = await transactionJournalApi.getImageBlob(
              img.termGroupId,
              img.termId,
              img.jnlDate,
              img.jnlTime,
              img.imageType,
            );
            const url = URL.createObjectURL(res.data);
            urlsRef.current.push(url);
            return { ...img, url };
          } catch {
            return { ...img, error: true };
          }
        }),
      );
      if (!cancelled) {
        setItems(loaded);
        setLoading(false);
      } else {
        loaded.forEach((l) => l.url && URL.revokeObjectURL(l.url));
      }
    })();

    return () => {
      cancelled = true;
      urlsRef.current.forEach((u) => URL.revokeObjectURL(u));
      urlsRef.current = [];
    };
  }, [images]);

  if (!images?.length) {
    return <Empty description="첨부 이미지 없음 (T_JOURNALIMAGE)" />;
  }

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 24 }}>
        <Spin tip="이미지 불러오는 중..." />
      </div>
    );
  }

  return (
    <Image.PreviewGroup>
      <Space size={16} wrap align="start">
        {items.map((img) => (
          <div
            key={`${img.imageType}-${img.imagePath}`}
            style={{ textAlign: 'center', width: 200 }}
          >
            <div style={{ marginBottom: 4 }}>
              <Tag color="blue">{IMAGE_TYPE_LABEL[img.imageType] ?? img.imageType}</Tag>
            </div>
            {img.error || !img.url ? (
              <div
                style={{
                  width: 200,
                  height: 140,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  border: '1px dashed #d9d9d9',
                  borderRadius: 4,
                  color: '#999',
                  fontSize: 12,
                }}
              >
                이미지 로드 실패
              </div>
            ) : (
              <Image
                src={img.url}
                width={200}
                height={140}
                style={{ objectFit: 'contain', border: '1px solid #f0f0f0', borderRadius: 4 }}
              />
            )}
            <Typography.Text
              type="secondary"
              ellipsis={{ tooltip: img.imagePath }}
              style={{ display: 'block', fontSize: 11, marginTop: 4 }}
            >
              {img.imagePath}
            </Typography.Text>
          </div>
        ))}
      </Space>
    </Image.PreviewGroup>
  );
};

export default JournalImageGallery;
