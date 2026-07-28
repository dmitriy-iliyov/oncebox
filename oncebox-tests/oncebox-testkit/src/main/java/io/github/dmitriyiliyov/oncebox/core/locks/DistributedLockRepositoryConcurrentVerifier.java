package io.github.dmitriyiliyov.oncebox.core.locks;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class DistributedLockRepositoryConcurrentVerifier {

    private final JdbcTemplate jdbcTemplate;
    private final DistributedLockRepository repository;
    private final DistributedLockRepositoryVerifier.IdExtractor idExtractor;
    private final DistributedLockRepositoryVerifier.IdPreparer idPreparer;
    private final SetupJobScript setupJobScript;

    public DistributedLockRepositoryConcurrentVerifier(
            JdbcTemplate jdbcTemplate,
            DistributedLockRepository repository,
            DistributedLockRepositoryVerifier.IdExtractor idExtractor,
            DistributedLockRepositoryVerifier.IdPreparer idPreparer,
            SetupJobScript setupJobScript
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.repository = repository;
        this.idExtractor = idExtractor;
        this.idPreparer = idPreparer;
        this.setupJobScript = setupJobScript;
    }

    public void concurrent_multipleTryLock_onlyOneSucceeds() throws Exception {
        String jobName = "test-job-" + UUID.randomUUID();
        setupJobScript.setup(jdbcTemplate, idPreparer, jobName, 1000L, 5000L);

        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        List<UUID> successfulWorkers = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    UUID workerId = UUID.randomUUID();
                    startLatch.await();
                    boolean locked = repository.tryLock(jobName, workerId);
                    if (locked) {
                        successCount.incrementAndGet();
                        synchronized (successfulWorkers) {
                            successfulWorkers.add(workerId);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(successfulWorkers).hasSize(1);
        assertThat(getLockedBy(jobName)).isEqualTo(successfulWorkers.get(0));
    }

    public void concurrent_lockAndUnlock_noRaceCondition() throws Exception {
        String jobName = "test-job-" + UUID.randomUUID();
        setupJobScript.setup(jdbcTemplate, idPreparer, jobName, 1000L, 5000L);

        int iterations = 20;
        CountDownLatch finishLatch = new CountDownLatch(iterations);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(5);

        for (int i = 0; i < iterations; i++) {
            executor.submit(() -> {
                try {
                    UUID workerId = UUID.randomUUID();
                    if (repository.tryLock(jobName, workerId)) {
                        successCount.incrementAndGet();
                        Thread.sleep(50);
                        repository.unlock(jobName, workerId);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        finishLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).isGreaterThan(0);
    }

    public void concurrent_lockExpiredAndRetry_onlyOneGetsLock() throws Exception {
        String jobName = "test-job-" + UUID.randomUUID();
        setupJobScript.setup(jdbcTemplate, idPreparer, jobName, 100L, 200L);

        UUID worker1 = UUID.randomUUID();
        repository.tryLock(jobName, worker1);

        Thread.sleep(300);

        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        List<UUID> successfulWorkers = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    UUID workerId = UUID.randomUUID();
                    startLatch.await();
                    boolean locked = repository.tryLock(jobName, workerId);
                    if (locked) {
                        successCount.incrementAndGet();
                        synchronized (successfulWorkers) {
                            successfulWorkers.add(workerId);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(successfulWorkers).hasSize(1);
        assertThat(getLockedBy(jobName)).isNotEqualTo(worker1);
    }

    public void concurrent_highContention_correctBehavior() throws Exception {
        String jobName = "test-job-" + UUID.randomUUID();
        setupJobScript.setup(jdbcTemplate, idPreparer, jobName, 5000L, 1000L);

        int threadCount = 50;
        int iterations = 100;
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger totalAttempts = new AtomicInteger(0);
        AtomicInteger successfulLocks = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        UUID workerId = UUID.randomUUID();
                        totalAttempts.incrementAndGet();
                        if (repository.tryLock(jobName, workerId)) {
                            successfulLocks.incrementAndGet();
                            Thread.sleep(10);
                            repository.unlock(jobName, workerId);
                        }
                        Thread.sleep(5);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        finishLatch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(totalAttempts.get()).isEqualTo(threadCount * iterations);
        assertThat(successfulLocks.get()).isGreaterThan(0);
        assertThat(successfulLocks.get()).isLessThan(totalAttempts.get());
    }

    public void concurrent_unlockByDifferentWorkers_onlyOwnerUnlocks() throws Exception {
        String jobName = "test-job-" + UUID.randomUUID();
        setupJobScript.setup(jdbcTemplate, idPreparer, jobName, 1000L, 5000L);

        UUID owner = UUID.randomUUID();
        repository.tryLock(jobName, owner);

        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    UUID workerId = UUID.randomUUID();
                    startLatch.await();
                    repository.unlock(jobName, workerId);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(getLockedBy(jobName)).isEqualTo(owner);

        repository.unlock(jobName, owner);
        Thread.sleep(1500);

        boolean canLock = repository.tryLock(jobName, UUID.randomUUID());
        assertThat(canLock).isTrue();
    }

    public void concurrent_multipleJobsLocking_independentLocks() throws Exception {
        int jobCount = 5;
        List<String> jobNames = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            String jobName = "test-job-" + UUID.randomUUID();
            setupJobScript.setup(jdbcTemplate, idPreparer, jobName, 1000L, 5000L);
            jobNames.add(jobName);
        }

        int threadCount = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            int jobIndex = i % jobCount;
            executor.submit(() -> {
                try {
                    UUID workerId = UUID.randomUUID();
                    startLatch.await();
                    if (repository.tryLock(jobNames.get(jobIndex), workerId)) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(jobCount);

        for (String jobName : jobNames) {
            UUID lockedBy = getLockedBy(jobName);
            assertThat(lockedBy).isNotNull();
        }
    }

    public void cleanUp() {
        jdbcTemplate.update("DELETE FROM outbox_jobs");
    }

    private UUID getLockedBy(String jobName) {
        return jdbcTemplate.query(
                "SELECT locked_by FROM outbox_jobs WHERE job_name = ?",
                ps -> ps.setString(1, jobName),
                (rs, n) -> idExtractor.extract(rs.getObject("locked_by"))
        ).stream().findFirst().orElse(null);
    }
}