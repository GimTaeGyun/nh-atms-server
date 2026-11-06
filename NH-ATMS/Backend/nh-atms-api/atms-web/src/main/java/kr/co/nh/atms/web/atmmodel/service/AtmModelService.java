package kr.co.nh.atms.web.atmmodel.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.atmmodel.dto.AtmModelSearchDto;
import kr.co.nh.atms.web.atmmodel.entity.AtmModel;
import kr.co.nh.atms.web.atmmodel.mapper.AtmModelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AtmModelService {

    private final AtmModelMapper mapper;

    public PageResult<AtmModel> search(AtmModelSearchDto search) {
        return PageResult.of(mapper.selectList(search), mapper.countList(search), search);
    }

    public AtmModel get(String modelCd) {
        AtmModel m = mapper.selectOne(modelCd);
        if (m == null) throw new BusinessException("ATM_MODEL_001", "기기모델 정보를 찾을 수 없습니다.");
        return m;
    }

    @Transactional
    public void create(AtmModel model) {
        if (mapper.selectOne(model.getModelCd()) != null)
            throw new BusinessException("ATM_MODEL_002", "이미 등록된 모델코드입니다.");
        mapper.insert(model);
    }

    @Transactional
    public void update(AtmModel model) {
        if (mapper.update(model) == 0)
            throw new BusinessException("ATM_MODEL_001", "기기모델 정보를 찾을 수 없습니다.");
    }

    @Transactional
    public void delete(String modelCd) {
        if (mapper.delete(modelCd) == 0)
            throw new BusinessException("ATM_MODEL_001", "기기모델 정보를 찾을 수 없습니다.");
    }
}
