package kr.co.nh.atms.journal.mapper;

import kr.co.nh.atms.journal.dto.ApDeployInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * AP 자동배포 버전 조회 매퍼 (T_AP_LAST_VERSION + T_DOWNLOAD_SERVER).
 *
 * 에이전트 기동(open) 시 해당 기기(기종)의 배포 대상 AP 버전/파일 정보를 조회한다.
 * 레거시 atmr AgentMapper.selectApLastVersion 이식:
 *   - CONFIRM_STEP='1'(승인) 이면 승인버전과 동일한 배포본
 *   - 미승인이면 승인버전 직전(MAX < LAST_DOWN_VER)의 배포본
 * 결과 없으면 null → 호출부에서 빈 파일 항목 fallback.
 */
@Mapper
public interface ApVersionMapper {

    ApDeployInfo selectApLastVersion(@Param("termGroupId") String termGroupId,
                                     @Param("termId") String termId);
}
