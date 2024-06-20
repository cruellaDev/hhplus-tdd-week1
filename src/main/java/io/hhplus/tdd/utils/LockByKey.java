package io.hhplus.tdd.utils;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * <pre> 다른 동작들에 대한 방해없이 동시 처리를 위해 특정 key에 대하여 lock을 걺 </pre>
 * @link <a href="https://www.baeldung.com/java-acquire-lock-by-key">참고사이트</a>
 */
@Component
public class LockByKey {

    // lock 대기 중인 쓰레드 수를 트래킹
    private static class LockWrapper {
        // Lock은 쓰레드 동기화를 위해 사용되는 객체로 획득될 때까지 쓰레드를 blocking 해준다.
        private final Lock lock = new ReentrantLock();
        private final AtomicInteger numberOfThreadsInQueue = new AtomicInteger(1);

        private LockWrapper addThreadInQueue() {
            numberOfThreadsInQueue.incrementAndGet();
            return this;
        }

        private int removeThreadFromQueue() {
            return numberOfThreadsInQueue.decrementAndGet();
        }

    }

    // ConcurrentHashMap을 사용하면 멀티쓰레드 환경에서도 데이터의 통일성을 보장된다고 함.
    private static ConcurrentHashMap<Long, LockWrapper> locks = new ConcurrentHashMap<>();

    // 요청된 동작이 이미 다른 thread에서 사용/처리 중일 시 같은 key로 요청이 들어올 경우 해당 요청 block
    public void lock(long key) {
        // 쓰레드가 key에 대하여 lock이 필요할 시 LockWrapper 가 해당 key를 위해 존재해야하는지 알아봐야 함.
        // 엾을 시 새 인스턴스, 있을 시 쓰레드 수 + 1
        // 특정 key 요청이 들어왔는데, 만약 Lock 이 있다면 새 value로 교체됨.
        LockWrapper lockWrapper = locks.compute(key, (k, v) -> v == null ? new LockWrapper() : v.addThreadInQueue());
        lockWrapper.lock.lock();
    }

    public void unlock(long key) {
        LockWrapper lockWrapper = locks.get(key);
        lockWrapper.lock.unlock();
        // 쓰레드 수 0이 되면 key 제거
        if (lockWrapper.removeThreadFromQueue() == 0) {
            // NB : We pass in the specific value to remove to handle the case where another thread would queue right before the removal
            locks.remove(key, lockWrapper);
        }
    }

    public boolean acquired(long key, long seconds) throws InterruptedException {
        LockWrapper lockWrapper = locks.get(key);
        return lockWrapper.lock.tryLock(seconds, TimeUnit.SECONDS);
    }

    public <T> T manageLock(long key, Supplier<T> block) throws InterruptedException {
        lock(key);
        if(!acquired(key, 10)) {
            throw new RuntimeException("Timed out");
        }
        try {
            return block.get();
        } finally {
            unlock(key);
        }
    }

}
