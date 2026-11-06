package kr.co.nh.atms.batch.job.support;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * batday01 장애경과시간 계산 유틸.
 *
 * AS-IS atmb batday01 의 f_GetElapsetime / f_getElapsedTime(MySQL 함수의 Java 등가 구현)을
 * 그대로 이식 — 기기 운영시간대(주간형 08:00~22:00 / 심야형 07:00~02:00)를 반영하여
 * 장애 발생~복구 구간 중 운영시간대와 겹치는 경과시간(초)을 산출한다.
 *
 * 케이스:
 *  - 당일 발생 + 당일 복구  : 발생/복구 시각을 운영시간대로 클리핑
 *  - 당일 발생 + 미복구     : 호출 측에서 복구시각을 운영종료시각으로 보정 후 호출
 *  - 전일 발생 + 당일 복구  : 운영시작시각~복구시각 구간으로 호출
 */
public final class ElapsedTimeCalculator {

    private static final DateTimeFormatter DTF_YMDHMS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DTF_YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private ElapsedTimeCalculator() {
    }

    /**
     * 두 일시(yyyyMMdd + HHmmss) 간 경과시간(절대값).
     * AS-IS f_GetElapsetime 이식.
     * "240000"(운영종료 24:00 표기)은 익일 00:00 으로 해석한다 —
     * java.time 은 24시 표기를 허용하지 않으므로 등가 변환.
     */
    public static Duration between(String sdate, String stime, String edate, String etime) {
        return Duration.between(parse(sdate, stime), parse(edate, etime)).abs();
    }

    private static LocalDateTime parse(String date, String time) {
        if ("240000".equals(time)) {
            return LocalDate.parse(date, DTF_YMD).plusDays(1).atStartOfDay();
        }
        return LocalDateTime.parse(date + time, DTF_YMDHMS);
    }

    /**
     * 기기 운영시간대를 반영한 장애경과시간(초 단위 Duration).
     * AS-IS f_getElapsedTime 이식 — 인자 순서/의미 동일.
     *
     * @param startTime 운영시작시간(HHmmss)
     * @param endTime   운영종료시간(HHmmss)
     * @param edate     장애 복구일(yyyyMMdd)
     * @param etime     장애 복구시간(HHmmss)
     * @param sdate     장애 발생일(yyyyMMdd)
     * @param stime     장애 발생시간(HHmmss)
     */
    public static Duration elapsedInServiceTime(String startTime, String endTime,
                                                String edate, String etime,
                                                String sdate, String stime) {
        String vStartTime = "000000";
        String vEndTime = "235959";
        Duration elapsedTime1 = Duration.ZERO;
        Duration elapsedTime2 = Duration.ZERO;
        Duration elapsedTime3 = Duration.ZERO;
        Duration elapsedTime4;

        if (startTime.compareTo(endTime) < 0) {
            // 08:00 ~ 22:00 형(주간 운영)
            if (sdate.compareTo(edate) == 0) {
                // 대상일에 발생하여 대상일에 복구
                if (startTime.compareTo(stime) <= 0 && stime.compareTo(endTime) <= 0) {
                    // 장애발생시간이 기기 운영시간 범위 내
                    if (etime.compareTo(endTime) <= 0) {
                        // 장애복구시간이 기기 운영시간 범위 내
                        elapsedTime4 = between(sdate, stime, edate, etime);
                    } else {
                        // 장애복구시간이 운영종료 이후 → 운영종료시간으로 보정
                        elapsedTime4 = between(sdate, stime, edate, endTime);
                    }
                } else if (stime.compareTo(startTime) < 0) {
                    // 장애발생시간이 기기 운영시작시간 이전
                    if (etime.compareTo(startTime) <= 0) {
                        elapsedTime4 = Duration.ZERO;
                    } else if (startTime.compareTo(etime) <= 0 && etime.compareTo(endTime) <= 0) {
                        elapsedTime4 = between(sdate, startTime, edate, etime);
                    } else {
                        elapsedTime4 = between(sdate, startTime, edate, endTime);
                    }
                } else {
                    elapsedTime4 = Duration.ZERO;
                }
            } else {
                // 대상일에 발생하여 대상일에 미복구 (여러 날에 걸친 장애)
                if (startTime.compareTo(stime) <= 0 && stime.compareTo(endTime) <= 0) {
                    elapsedTime1 = between(sdate, stime, sdate, endTime);
                } else if (stime.compareTo(startTime) < 0) {
                    elapsedTime1 = between(sdate, startTime, sdate, endTime);
                } else {
                    elapsedTime1 = Duration.ZERO;
                }

                if (etime.compareTo(startTime) <= 0) {
                    elapsedTime2 = Duration.ZERO;
                } else if (startTime.compareTo(etime) <= 0 && etime.compareTo(endTime) <= 0) {
                    elapsedTime2 = between(edate, startTime, edate, etime);
                } else {
                    elapsedTime2 = between(edate, startTime, edate, endTime);
                }

                long elapsedDate = ChronoUnit.DAYS.between(
                        LocalDate.parse(edate, DTF_YMD), LocalDate.parse(sdate, DTF_YMD));

                if (elapsedDate == 1) {
                    elapsedTime3 = Duration.ZERO;
                } else {
                    elapsedTime3 = between(sdate, startTime, sdate, endTime);
                }

                elapsedTime4 = Duration.ofSeconds(
                        elapsedTime1.getSeconds()
                                + elapsedTime2.getSeconds()
                                + elapsedTime3.getSeconds() * (elapsedDate - 1));
            }
        } else {
            // 07:00 ~ 02:00 형(자정 넘김 운영)
            if (sdate.compareTo(edate) == 0) {
                // 대상일에 발생하여 대상일에 복구
                if (vStartTime.compareTo(stime) <= 0 && stime.compareTo(endTime) <= 0) {
                    // 발생 00:00~02:00
                    if (vStartTime.compareTo(etime) <= 0 && etime.compareTo(startTime) <= 0) {
                        elapsedTime1 = between(sdate, stime, edate, etime);
                    } else if (endTime.compareTo(etime) <= 0 && etime.compareTo(startTime) <= 0) {
                        elapsedTime1 = between(sdate, stime, edate, endTime);
                    } else {
                        elapsedTime1 = between(sdate, stime, edate, endTime);
                        elapsedTime2 = between(sdate, startTime, edate, etime);
                    }
                } else if (endTime.compareTo(stime) <= 0 && stime.compareTo(startTime) <= 0) {
                    // 발생 02:00~07:00 (휴지시간)
                    if (vStartTime.compareTo(etime) <= 0 && etime.compareTo(startTime) <= 0) {
                        elapsedTime1 = Duration.ZERO;
                    } else if (endTime.compareTo(etime) <= 0 && etime.compareTo(startTime) <= 0) {
                        elapsedTime1 = Duration.ZERO;
                    } else {
                        elapsedTime1 = between(sdate, startTime, edate, etime);
                    }
                } else {
                    // 발생 07:00~24:00
                    if (vStartTime.compareTo(etime) <= 0 && etime.compareTo(startTime) <= 0) {
                        elapsedTime1 = Duration.ZERO;
                    } else if (endTime.compareTo(etime) <= 0 && etime.compareTo(startTime) <= 0) {
                        elapsedTime1 = Duration.ZERO;
                    } else {
                        elapsedTime1 = between(sdate, stime, edate, etime);
                    }
                }
                elapsedTime4 = Duration.ofSeconds(
                        elapsedTime1.getSeconds() + elapsedTime2.getSeconds() + elapsedTime3.getSeconds());
            } else {
                if (vStartTime.compareTo(stime) <= 0 && stime.compareTo(endTime) <= 0) {
                    elapsedTime1 = between(sdate, stime, sdate, endTime);
                } else if (endTime.compareTo(stime) <= 0 && stime.compareTo(startTime) <= 0) {
                    elapsedTime1 = Duration.ZERO;
                } else {
                    elapsedTime1 = between(sdate, stime, sdate, vEndTime);
                }

                if (vStartTime.compareTo(stime) <= 0 && stime.compareTo(endTime) <= 0) {
                    elapsedTime2 = between(edate, vStartTime, edate, etime);
                } else if (endTime.compareTo(stime) <= 0 && stime.compareTo(startTime) <= 0) {
                    elapsedTime2 = between(edate, vStartTime, edate, endTime);
                } else {
                    elapsedTime2 = Duration.ofSeconds(
                            between(edate, vStartTime, edate, endTime).getSeconds()
                                    + between(edate, startTime, edate, etime).getSeconds());
                }

                if (elapsedTime1.getSeconds() == 1) {
                    elapsedTime3 = Duration.ZERO;
                } else {
                    elapsedTime3 = Duration.ofSeconds(
                            between(edate, vStartTime, edate, endTime).getSeconds()
                                    + between(edate, startTime, edate, vEndTime).getSeconds());
                }

                elapsedTime4 = Duration.ofSeconds(
                        elapsedTime1.getSeconds()
                                + elapsedTime2.getSeconds()
                                + elapsedTime3.getSeconds() * (elapsedTime1.getSeconds() - 1));
            }
        }
        return elapsedTime4;
    }
}
