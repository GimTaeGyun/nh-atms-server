package kr.co.nh.atms.batch.job;

import kr.co.nh.atms.batch.config.Batday99Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * batday99 : 파일 정리 Job — AS-IS atmb batday99 이식.
 * Job 빈 이름 = T_BATCH_JOB.PROC_NAME = "batday99" (크론: 00 50 23 * * ?)
 *
 * 1) 저널 이미지 루트(jnl-img-root) 하위의 yyyyMMdd 일자 디렉토리 중
 *    보관개월(jnl-img-months, 음수) 이전 디렉토리를 통째로 삭제
 * 2) 설정 목록(targets)의 각 경로에서 보관일수(days, 음수) 초과(수정일 기준)
 *    파일을 재귀 삭제 (AS-IS 와 동일하게 빈 디렉토리는 남김)
 *
 * AS-IS 와의 차이: AS-IS 는 config.json + 수동실행 파라미터(list)를 함께 지원했으나
 * TO-BE 수동실행 API 는 파라미터를 받지 않으므로 설정(application.yml) 기반으로 통일.
 */
@Slf4j
@Configuration
public class Batday99JobConfig {

    public static final String JOB_NAME = "batday99";

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Bean(name = JOB_NAME)
    public Job batday99Job(JobRepository jobRepository,
                           @Qualifier("fileCleanupStep") Step fileCleanupStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(fileCleanupStep)
                .build();
    }

    @Bean
    public Step fileCleanupStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager,
                                @Qualifier("fileCleanupTasklet") Tasklet fileCleanupTasklet) {
        return new StepBuilder("batday99.fileCleanupStep", jobRepository)
                .tasklet(fileCleanupTasklet, transactionManager)
                .build();
    }

    @Bean
    public Tasklet fileCleanupTasklet(Batday99Properties props) {
        return (contribution, chunkContext) -> {
            long deleted = 0;

            // 1) 저널 이미지 일자 디렉토리 정리
            deleted += cleanJournalImages(props);

            // 2) 설정 경로별 보관일수 초과 파일 정리
            for (Batday99Properties.Target target : props.getTargets()) {
                if (target.getPath() == null || target.getPath().isBlank()) continue;
                Path path = Paths.get(target.getPath());
                if (!Files.exists(path)) {
                    log.warn("[batday99] [config-path][days({}):{}][not found]", target.getDays(), path);
                    continue;
                }
                log.info("[batday99] [config-path][days({}):{}]", target.getDays(), path);
                deleted += deleteOldFiles(path, target.getDays());
            }

            ExecutionContext jobCtx = contribution.getStepExecution()
                    .getJobExecution().getExecutionContext();
            jobCtx.putLong("procCnt", deleted);

            log.info("[batday99] 파일 정리 완료 삭제파일수={}", deleted);
            return RepeatStatus.FINISHED;
        };
    }

    /** 저널 이미지 루트 하위 yyyyMMdd 디렉토리 중 보관개월 이전 디렉토리 삭제 (AS-IS 동일) */
    private long cleanJournalImages(Batday99Properties props) throws IOException {
        if (props.getJnlImgRoot() == null || props.getJnlImgRoot().isBlank()) {
            return 0;
        }
        Path root = Paths.get(props.getJnlImgRoot());
        if (!Files.exists(root)) {
            log.warn("[batday99] [jnl-img-root:{}][not found]", root);
            return 0;
        }
        ZoneId zone = ZoneId.systemDefault();
        Instant cutoff = LocalDate.now().plusMonths(props.getJnlImgMonths()).atStartOfDay(zone).toInstant();
        long deleted = 0;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(root)) {
            for (Path dir : ds) {
                if (!Files.isDirectory(dir)) continue;
                Instant dirDate;
                try {
                    dirDate = LocalDate.parse(dir.getFileName().toString(), YYYYMMDD)
                            .atStartOfDay(zone).toInstant();
                } catch (Exception ex) {
                    continue; // 일자 형식이 아닌 디렉토리는 제외
                }
                if (!dirDate.isBefore(cutoff)) continue;
                deleted += deleteRecursively(dir);
                if (!Files.exists(dir)) {
                    log.info("[batday99] [jnl-img-path:{}] 삭제", root.relativize(dir));
                }
            }
        }
        return deleted;
    }

    /** 디렉토리 전체(파일+하위디렉토리) 삭제 — 삭제한 파일 수 반환 */
    private long deleteRecursively(Path dir) {
        AtomicLong count = new AtomicLong();
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            boolean isFile = Files.isRegularFile(p);
                            Files.delete(p);
                            if (isFile) count.incrementAndGet();
                        } catch (Exception ex) {
                            log.warn("[batday99] [path:{}][exception][{}]", p, ex.getMessage());
                        }
                    });
        } catch (Exception ex) {
            log.warn("[batday99] [folder:{}][exception][{}]", dir, ex.getMessage());
        }
        return count.get();
    }

    /** 보관일수(days, 음수) 초과 파일 삭제 (하위 디렉토리 포함, 빈 폴더는 유지 — AS-IS 동일) */
    private long deleteOldFiles(Path path, int days) {
        ZoneId zone = ZoneId.systemDefault();
        Instant cutoff = LocalDate.now().plusDays(days).atStartOfDay(zone).toInstant();
        AtomicLong count = new AtomicLong();

        // path 가 단일 파일인 경우
        if (Files.isRegularFile(path)) {
            try {
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                if (attrs.lastModifiedTime().toInstant().isBefore(cutoff)) {
                    Files.delete(path);
                    count.incrementAndGet();
                    log.info("[batday99] [file:{}] 삭제", path);
                }
            } catch (Exception ex) {
                log.warn("[batday99] [file:{}][exception][{}]", path, ex.getMessage());
            }
            return count.get();
        }

        if (!Files.isDirectory(path)) return 0;

        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            if (!Files.isRegularFile(p)) return;
                            BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                            if (attrs.lastModifiedTime().toInstant().isBefore(cutoff)) {
                                Files.delete(p);
                                count.incrementAndGet();
                                log.info("[batday99] [file:/{}] 삭제", path.relativize(p));
                            }
                        } catch (Exception ex) {
                            log.warn("[batday99] [file:{}][exception][{}]", p, ex.getMessage());
                        }
                    });
        } catch (Exception ex) {
            log.warn("[batday99] [folder:{}][exception][{}]", path, ex.getMessage());
        }
        return count.get();
    }
}
