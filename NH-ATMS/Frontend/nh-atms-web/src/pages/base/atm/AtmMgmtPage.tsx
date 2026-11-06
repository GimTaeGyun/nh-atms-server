import React from 'react';
import PageTemplate from '@/components/PageTemplate';
import AtmModelTab from './AtmModelTab';

const AtmMgmtPage: React.FC = () => (
  <PageTemplate title="기기관리">
    <AtmModelTab />
  </PageTemplate>
);

export default AtmMgmtPage;
