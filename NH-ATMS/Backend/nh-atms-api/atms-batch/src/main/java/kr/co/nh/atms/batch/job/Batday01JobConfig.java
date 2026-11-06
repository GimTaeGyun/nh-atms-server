package kr.co.nh.atms.batch.job;

import kr.co.nh.atms.batch.dto.RunHistoryRow;
import kr.co.nh.atms.batch.dto.StopHistoryRow;
import kr.co.nh.atms.batch.job.support.ElapsedTimeCalculator;
import kr.co.nh.atms.batch.mapper.Batday01Mapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * batday01 : 장애율/가동률 집계 Job — AS-IS atmb batday01 시간 기반 로직 이식.
 * Job 빈 이름 = T_BATCH_JOB.PROC_NAME = "batday01" (크론: 00 00 01 * * ?)
 *
 * AS-IS 로직(대상일 = statDate, 기본 전일):
 *  1) T_ATM_RUN_HISTORY / T_ATM_ERR_GROUP_HISTORY 대상일 삭제 (멱등)
 *  2) 대상 기기(T_ATM 운영중 + T_CORNER)별로
 *     - 운영시간대 결정: T_CORNER.SERVICE_START/END_TIME, 미설정 시 설치장소유형별 기본값
 *       (1:점내 09~16시, 2:점외 08~22시, 3:무인 00~24시 — 가동대상시간 기본값은 AS-IS 값 유지)
 *     - 장애경과시간 산출(T_ATM_STOP_HISTORY):
 *       · 당일 발생/당일 복구, 당일 발생/미복구(운영종료시각으로 보정),
 *         전일 발생/당일 복구(운영시작~복구시각, 이 경우 지연개국 미계상) 3케이스
 *     - 지연개국시간: 개국(FO) 시각 > 운영시작시각 인 경우 그 차이
 *     - 미개국시간: 당일 개국저널 없음 → 가동대상시간 전체
 *     - 실제가동시간 = 가동대상시간 - 총장애시간 - 지연개국시간 - 미개국시간 (음수면 0)
 *     - 거래건수(TR_TYPE != '0131') 집계 후 T_ATM_RUN_HISTORY 적재
 *  3) 기기·장애그룹별 장애건수 T_ATM_ERR_GROUP_HISTORY 적재
 *
 * 디바이스별/장애분류별 장애 건수·시간은 T_FAULT_ANALYSIS 에서 SQL 집계
 * (AS-IS time_elapsed 컬럼 부재 → 발생~복구 일시 차이로 등가 산출, Batday01Mapper.xml 참조).
 */
@Slf4j
@Configuration
public class Batday01JobConfig {

    public static final String JOB_NAME = "batday01";

    @Bean(name = JOB_NAME)
    public Job batday01Job(JobRepository jobRepository,
                           @Qualifier("runHistoryStep") Step runHistoryStep,
                           @Qualifier("errGroupHistoryStep") Step errGroupHistoryStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(runHistoryStep)
                .next(errGroupHistoryStep)
                .build();
    }

    @Bean
    public Step runHistoryStep(JobRepository jobRepository,
                               PlatformTransactionManager transactionManager,
                               @Qualifier("runHistoryTasklet") Tasklet runHistoryTasklet) {
        return new StepBuilder("batday01.runHistoryStep", jobRepository)
                .tasklet(runHistoryTasklet, transactionManager)
                .build();
    }

    @Bean
    public Step errGroupHistoryStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager,
                                    @Qualifier("errGroupHistoryTasklet") Tasklet errGroupHistoryTasklet) {
        return new StepBuilder("batday01.errGroupHistoryStep", jobRepository)
                .tasklet(errGroupHistoryTasklet, transactionManager)
                .build();
    }

    /** Step 1) 기기별 가동률 산출 → T_ATM_RUN_HISTORY 적재 */
    @Bean
    @StepScope
    public Tasklet runHistoryTasklet(Batday01Mapper mapper,
                                     @Value("#{jobParameters['statDate']}") String statDate) {
        return (contribution, chunkContext) -> {
            int deleted = mapper.deleteRunHistory(statDate);
            List<RunHistoryRow> rows = mapper.selectFaultData(statDate);
            int inserted = 0;

            for (RunHistoryRow rh : rows) {
                normalize(rh);
                inserted += processOne(mapper, statDate, rh);
            }

            ExecutionContext jobCtx = contribution.getStepExecution()
                    .getJobExecution().getExecutionContext();
            jobCtx.putLong("targetCnt", rows.size());
            jobCtx.putLong("procCnt", jobCtx.getLong("procCnt", 0L) + inserted);

            log.info("[batday01] 가동률 집계 baseDate={} 삭제={} 대상={} 적재={}",
                    statDate, deleted, rows.size(), inserted);
            return RepeatStatus.FINISHED;
        };
    }

    /** Step 2) 기기·장애그룹별 장애건수 → T_ATM_ERR_GROUP_HISTORY 적재 */
    @Bean
    @StepScope
    public Tasklet errGroupHistoryTasklet(Batday01Mapper mapper,
                                          @Value("#{jobParameters['statDate']}") String statDate) {
        return (contribution, chunkContext) -> {
            int deleted = mapper.deleteErrGroupHistory(statDate);
            int inserted = mapper.insertErrGroupHistory(statDate);

            ExecutionContext jobCtx = contribution.getStepExecution()
                    .getJobExecution().getExecutionContext();
            jobCtx.putLong("procCnt", jobCtx.getLong("procCnt", 0L) + inserted);

            log.info("[batday01] 장애그룹 집계 baseDate={} 삭제={} 적재={}", statDate, deleted, inserted);
            return RepeatStatus.FINISHED;
        };
    }

    /** 기기 1대의 가동시간 산출 + 적재 — AS-IS batday01.run() 루프 본문 이식 */
    private int processOne(Batday01Mapper mapper, String baseDate, RunHistoryRow rh) {
        long elapsedTime = 0;
        long totalTime = 0;
        long notOpenTime = 0;
        boolean delayOpen = false;

        String serviceStartTime = rh.getServiceStartTime() + "00";  // HHmm → HHmmss
        String serviceEndTime = rh.getServiceEndTime() + "00";

        if (serviceStartTime.length() < 4) {
            // 운영시간 미설정 → 설치장소유형별 기본값 (가동대상시간 기본값은 AS-IS 원본 값 유지)
            switch (rh.getLocationTypeCd().charAt(0)) {
                case '1' -> { serviceStartTime = "090000"; serviceEndTime = "160000"; totalTime = 86400; } // 점내
                case '2' -> { serviceStartTime = "080000"; serviceEndTime = "220000"; totalTime = 28800; } // 점외
                case '3' -> { serviceStartTime = "000000"; serviceEndTime = "240000"; totalTime = 86400; } // 무인점포
                default  -> { serviceStartTime = "080000"; serviceEndTime = "220000"; totalTime = 86400; }
            }
        } else {
            if ("240000".equals(serviceStartTime)) {
                serviceStartTime = "000000";
            }
            if ("000000".equals(serviceEndTime)) {
                serviceEndTime = "240000";
            }
            // 가동대상시간 산출
            boolean midnightEnd = false;
            if (serviceStartTime.compareTo(serviceEndTime) < 0) {
                // 09:00 ~ 17:00 형
                if ("240000".equals(serviceEndTime)) {
                    serviceEndTime = "235959";
                    midnightEnd = true;
                }
                totalTime = ElapsedTimeCalculator
                        .between(baseDate, serviceStartTime, baseDate, serviceEndTime).getSeconds();
                if (midnightEnd) totalTime += 1;
            } else {
                // 07:00~02:00 형 → 07:00~24:00 + 00:00~02:00
                totalTime = ElapsedTimeCalculator.between(baseDate, "000000", baseDate, serviceEndTime).getSeconds()
                        + ElapsedTimeCalculator.between(baseDate, serviceStartTime, baseDate, "235959").getSeconds()
                        + 1;
            }
        }

        if (rh.getOpenTime().compareTo(serviceStartTime) > 0) {
            delayOpen = true;
        } else if (rh.getOpenTime().length() < 6) {
            // 미개국기기 — AS-IS obtainNotOpenTime() 은 0 고정(일시운영중지 테이블 미사용)
            notOpenTime = totalTime;
        }

        Map<String, Object> param = new HashMap<>();
        param.put("termGroupId", rh.getTermGroupId());
        param.put("termId", rh.getTermId());
        param.put("baseDate", baseDate);

        // 1. 배치 대상일에 발생한 장애시간 산출
        for (StopHistoryRow ath : mapper.selectStopHistoryOfDay(param)) {
            normalize(ath);
            if (!ath.getJnlDate().equals(ath.getFinishedDate())) {
                // 당일 발생 장애 중 미복구 장애 → 복구시각을 운영종료시각으로 보정
                ath.setFinishedDate(ath.getJnlDate());
                ath.setFinishedTime(serviceEndTime);
            }
            long t = ElapsedTimeCalculator.elapsedInServiceTime(
                    serviceStartTime, serviceEndTime,
                    ath.getFinishedDate(), ath.getFinishedTime(),
                    ath.getJnlDate(), ath.getJnlTime()).getSeconds();
            if (t <= 0) continue;
            elapsedTime += t;
        }

        // 3. 대상일 이전 발생 장애 중 대상일 복구 장애 장애시간 산출
        for (StopHistoryRow ath : mapper.selectStopHistoryBefore(param)) {
            normalize(ath);
            delayOpen = false; // 전일장애가 당일 복구된 경우 지연개국 미계상 (AS-IS 동일)
            long t = ElapsedTimeCalculator.elapsedInServiceTime(
                    serviceStartTime, serviceEndTime,
                    baseDate, ath.getFinishedTime(),
                    baseDate, serviceStartTime).getSeconds();
            elapsedTime += t;
        }

        // 4. 지연개국시간
        rh.setDelayOpenTime(0);
        if (delayOpen) {
            rh.setDelayOpenTime(ElapsedTimeCalculator
                    .between(baseDate, serviceStartTime, baseDate, rh.getOpenTime()).getSeconds());
        }

        rh.setTotalTargetTime(totalTime);
        // 실제가동시간 = 가동대상시간 - 총장애시간 - 지연개국시간 - 미개국시간
        rh.setTotalRunTime(Math.max(0,
                rh.getTotalTargetTime() - elapsedTime - rh.getDelayOpenTime() - notOpenTime));

        // 5. 당일 거래건수
        rh.setTransactionCnt(mapper.countTransactions(param));

        // 6. 적재
        return mapper.insertRunHistory(rh);
    }

    /** AS-IS Struct.stringMemberNullToEmpty 이식 — 문자열 null → "" (설치장소유형은 "-") */
    private void normalize(RunHistoryRow rh) {
        rh.setBaseDate(nvl(rh.getBaseDate()));
        rh.setTermGroupId(nvl(rh.getTermGroupId()));
        rh.setTermId(nvl(rh.getTermId()));
        rh.setCornerCd(nvl(rh.getCornerCd()));
        rh.setCornerTypeCd(nvl(rh.getCornerTypeCd()));
        rh.setOperationTypeCd(nvl(rh.getOperationTypeCd()));
        rh.setAreaCd(nvl(rh.getAreaCd()));
        rh.setServiceStartTime(nvl(rh.getServiceStartTime()).trim());
        rh.setServiceEndTime(nvl(rh.getServiceEndTime()).trim());
        rh.setAtmCoCd(nvl(rh.getAtmCoCd()));
        rh.setModelCd(nvl(rh.getModelCd()));
        rh.setSecurityProviderCd(nvl(rh.getSecurityProviderCd()));
        rh.setServiceProviderCd(nvl(rh.getServiceProviderCd()));
        rh.setOpenTime(nvl(rh.getOpenTime()).trim());
        String loc = nvl(rh.getLocationTypeCd()).trim();
        rh.setLocationTypeCd(loc.isEmpty() ? "-" : loc);
    }

    private void normalize(StopHistoryRow ath) {
        ath.setJnlDate(nvl(ath.getJnlDate()));
        ath.setJnlTime(nvl(ath.getJnlTime()));
        ath.setFinishedDate(nvl(ath.getFinishedDate()));
        ath.setFinishedTime(nvl(ath.getFinishedTime()));
        ath.setDeviceId(nvl(ath.getDeviceId()));
        ath.setErrGroupCd(nvl(ath.getErrGroupCd()));
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}
