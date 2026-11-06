import api from './axiosInstance';

interface ApiResponse<T> {
  success: boolean;
  message: string;
  code: string;
  data: T;
}

interface PageResult<T> {
  content: T[];
  totalCount: number;
  page: number;
  size: number;
  totalPages: number;
}

export interface AtmModel {
  modelCd: string;
  modelName: string;
  atmCoCd?: string;
  atmTypeCd?: string;
  lastAppVer?: string;
  lastMwiVer?: string;
  jnlWriteMode?: string;
  lastAgentVer?: string;
  maxAmount?: number;
  dataSupported?: string;
  sprImgSupported?: string;
  faceImgSupported?: string;
  handImgSupported?: string;
  mcrImgSupported?: string;
  chkFrontImgSupported?: string;
  chkBackImgSupported?: string;
  apLogDir?: string;
  agentLogDir?: string;
  jnlIndexDir?: string;
  useYn?: string;
  downloadPath?: string;
  remoteYn?: string;
  exeFilePath?: string;
  osCd?: string;
  bankModelCd?: string;
  dotSupported?: string;
  earphoneSupported?: string;
  zoomSupported?: string;
  wheelchairSupported?: string;
}

export interface AtmModelSearchParams {
  modelCd?: string;
  modelName?: string;
  atmCoCd?: string;
  atmTypeCd?: string;
  useYn?: string;
  page?: number;
  size?: number;
}

export const atmModelApi = {
  getList: (params?: AtmModelSearchParams) =>
    api.get<ApiResponse<PageResult<AtmModel>>>('/atm-model', { params }),

  getOne: (modelCd: string) =>
    api.get<ApiResponse<AtmModel>>(`/atm-model/${modelCd}`),

  create: (body: AtmModel) =>
    api.post<ApiResponse<void>>('/atm-model', body),

  update: (modelCd: string, body: AtmModel) =>
    api.put<ApiResponse<void>>(`/atm-model/${modelCd}`, body),

  delete: (modelCd: string) =>
    api.delete<ApiResponse<void>>(`/atm-model/${modelCd}`),
};
