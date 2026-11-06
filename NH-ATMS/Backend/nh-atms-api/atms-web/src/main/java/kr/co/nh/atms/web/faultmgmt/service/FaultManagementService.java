package kr.co.nh.atms.web.faultmgmt.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.faultmgmt.dto.FaultManagementSearchDto;
import kr.co.nh.atms.web.faultmgmt.dto.FaultReceiptRequest;
import kr.co.nh.atms.web.faultmgmt.entity.FaultManagement;
import kr.co.nh.atms.web.faultmgmt.mapper.FaultManagementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 기기장애접수(출동요청) 서비스.
 * 운영자가 실시간 모니터링/유지보수 화면에서 장애접수를 하면
 * T_FAULT_MANAGEMENT(FAULT_STATUS='O')에 적재된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FaultManagementService {

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter T = DateTimeFormatter.ofPattern("HHmmss");

    private final FaultManagementMapper mapper;

    /** 장애접수 이력 조회. */
    public PageResult<FaultManagement> search(FaultManagementSearchDto s) {
        return PageResult.of(mapper.selectList(s), mapper.countList(s), s);
    }

    /** 장애접수 단건 조회. */
    public FaultManagement get(Long faultSeq) {
        FaultManagement f = mapper.selectOne(faultSeq);
        if (f == null) throw new BusinessException("FLT_001", "장애접수 정보가 없습니다.");
        return f;
    }

    /** 기기장애접수(출동요청) 등록. */
    @Transactional
    public FaultManagement receive(FaultReceiptRequest req) {
        LocalDateTime now = LocalDateTime.now();
        FaultManagement fault = FaultManagement.builder()
                .termGroupId(req.getTermGroupId().trim())
                .termId(req.getTermId().trim())
                .faultType(req.getFaultType())
                .errCd(req.getErrCd())
                .vendorCd(req.getVendorCd())
                .faultDesc(req.getFaultDesc())
                .faultStatus("O")
                .callDate(now.format(D))
                .callTime(now.format(T))
                .build();
        mapper.insert(fault);
        log.info("[fault-receipt] received faultSeq={} term={}/{} type={} vendor={}",
                fault.getFaultSeq(), fault.getTermGroupId(), fault.getTermId(),
                fault.getFaultType(), fault.getVendorCd());
        return fault;
    }

    /** 출동취소(O/P → X). */
    @Transactional
    public void cancel(Long faultSeq) {
        int n = mapper.cancel(faultSeq);
        if (n == 0) throw new BusinessException("FLT_002", "취소 가능한 상태가 아닙니다.");
        log.info("[fault-receipt] cancelled faultSeq={}", faultSeq);
    }
}
