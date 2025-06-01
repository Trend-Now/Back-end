package com.trend_now.backend.config;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedissonConfig {

    private final RedissonClient redissonClient;

    public void execute(String lockName, long waitMilliSecond, long releaseMilliSecond,
            Runnable runnable) {
        RLock lock = redissonClient.getLock(lockName);
        try {
            boolean isLocked = lock.tryLock(waitMilliSecond, releaseMilliSecond,
                    TimeUnit.MILLISECONDS);
            if (!isLocked) {
                throw new IllegalArgumentException("[" + lockName + "] lock 획득 실패");
            }
            runnable.run();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
