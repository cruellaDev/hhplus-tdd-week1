package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.dto.UserPointDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class PointConcurrencyTest {

    @Autowired
    private PointService pointService;

    /**
     * 1000원 충전 후 100씩 100번 한꺼번에 포인트 사용 시 순차적으로 처리하여 결과 0원
     * @throws InterruptedException
     * @link <a href="https://thalals.tistory.com/370">참고URL</a>
     */
    @Test
    void with_charging_10000_points_use_points_concurrently_for_10_times() throws InterruptedException {
        // given
        long id = 1;
        long chargeAmount = 10000;
        long expectedAmount = 0;

        // when - then
        UserPointDto chargedPoint = pointService.charge(id, chargeAmount);
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 1; i <= threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.use(id,1000);
                } catch (RuntimeException e) {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        UserPointDto userPoint = pointService.point(id);
        assertEquals(chargeAmount, chargedPoint.getPoint());
        assertEquals(expectedAmount, userPoint.getPoint());
    }

    /**
     * 1000원 충전 후 100씩 100번 한꺼번에 포인트 사용 시 순차적으로 처리하여 결과 0원
     * @throws InterruptedException
     * @link <a href="https://thalals.tistory.com/370">참고URL</a>
     */
    @Test
    void with_charging_10000_points_use_3000_points_concurrently_for_10_times() throws InterruptedException {
        // given
        long id = 1;
        long chargeAmount = 10000;
        long expectedAmount = 0;

        // when - then
        UserPointDto chargedPoint = pointService.charge(id, chargeAmount);
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 1; i <= threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.use(id,3000);
                } catch (RuntimeException e) {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        assertEquals(latch.getCount(), 7);
    }

}
