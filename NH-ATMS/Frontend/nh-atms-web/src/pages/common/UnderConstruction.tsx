import React from 'react';
import { Result } from 'antd';
import { useLocation } from 'react-router-dom';

const UnderConstruction: React.FC = () => {
  const { pathname } = useLocation();
  return (
    <Result
      status="info"
      title="준비 중인 화면입니다"
      subTitle={`경로: ${pathname}`}
      style={{ paddingTop: 80 }}
    />
  );
};

export default UnderConstruction;
