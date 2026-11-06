package kr.co.nh.atms.ifex.service;

import kr.co.nh.atms.ifex.mapper.SequenceNoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 영구 채번 서비스 (구 MG datm.SP_GET_SEQNO / SP_UPDATE_SEQNO 이식).
 *
 * <p>대외 송수신 전문의 기관측 일련번호(atms_sn, BASE_CD_TYPE "004")와
 * 벤더 일련번호(vend_sn, "005") 등은 재기동에도 끊기지 않는 단조증가 값이어야 하므로,
 * 인메모리 {@code GlobalIdGenerator} 가 아닌 T_SEQ_NO_MASTER 기반으로 채번한다.</p>
 *
 * <p>레거시 채번 규약 (SequenceNoUtil):
 * <ul>
 *   <li>송신: atms_sn = generate("004", vendorCd, "netsend_snd"), vend_sn = generate("005", vendorCd, "")</li>
 *   <li>수신: regist_seqno = generate("004", vendorCd, "netrecv_reg") 후 SP_UPDATE_SEQNO 로 벤더 일련번호 동기화</li>
 * </ul>
 * 게이트웨이 전문 헤더 구성 시 {@link #nextFormatted} 로 7자리 일련번호를 채번하여 사용한다.</p>
 */
@Service
@RequiredArgsConstructor
public class SequenceNoService {

    /** SP_GET_SEQNO 의 순환 상한 (7자리). 초과 시 1 로 되돌린다. */
    private static final long MAX_SEQ_NO = 9_999_999L;

    private final SequenceNoMapper mapper;

    /**
     * (기준,컨텐츠) 조합의 다음 일련번호를 채번한다 (구 SP_GET_SEQNO).
     * 행 잠금 후 없으면 1 로 생성, 있으면 +1(9999999 초과 시 1) 한다.
     *
     * @return 새로 채번된 일련번호 (1..9999999)
     */
    @Transactional
    public long next(String baseCdType, String content1, String content2) {
        String c1 = content1 == null ? "" : content1;
        String c2 = content2 == null ? "" : content2;

        Long current = mapper.selectBaseNoForUpdate(baseCdType, c1, c2);
        long seqNo;
        if (current == null) {
            seqNo = 1L;
            mapper.insert(baseCdType, c1, c2, seqNo);
        } else {
            seqNo = current + 1L;
            if (seqNo > MAX_SEQ_NO) {
                seqNo = 1L;
            }
            mapper.updateBaseNo(baseCdType, c1, c2, seqNo);
        }
        return seqNo;
    }

    /**
     * 7자리 0-패딩 일련번호를 채번한다 (구 SequenceNoUtil.generate 규약).
     */
    @Transactional
    public String nextFormatted(String baseCdType, String content1, String content2) {
        return String.format("%07d", next(baseCdType, content1, content2));
    }

    /**
     * 벤더가 통보한 마지막 일련번호(vend_sn)를 동기화한다 (구 SP_UPDATE_SEQNO, BASE_CD_TYPE "005").
     * 존재하면 덮어쓰고, 없으면 신규 생성한다.
     */
    @Transactional
    public void syncVendorSeqNo(String vendorCd, long vendorSeqNo) {
        int updated = mapper.updateVendorBaseNo(vendorCd, vendorSeqNo);
        if (updated == 0) {
            mapper.insertVendor(vendorCd, vendorSeqNo);
        }
    }
}
