package io.hhplus.tdd.point;

import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import io.hhplus.tdd.utils.LockByKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final LockByKey lockByKey;

    public UserPoint point(long id) {
        return userPointRepository.selectById(id);
    }

    public List<PointHistory> history(long id) {
        return pointHistoryRepository.selectAllByUserId(id);
    }

    public UserPoint charge(long id, long amount)  {
        if (amount <= 0) {
            throw new RuntimeException("충전 금액은 0보다 커야 합니다.");
        }
        long existPoints = this.selectExistPointsByUserId(id);
        long totalAmount = existPoints + amount;
        try {
            return lockByKey.manageLock(id, () -> {
                UserPoint userPoint = userPointRepository.insertOrUpdate(id, totalAmount);
                pointHistoryRepository.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
                return userPoint;
            });
        } catch (InterruptedException e) {
            throw new RuntimeException("관리자에게 문의하십시오.");
        }
    }

    public UserPoint use(long id, long amount) {
        if (amount <= 0) {
            throw new RuntimeException("사용 금액은 0보다 커야 합니다.");
        }
        long existPoints = this.selectExistPointsByUserId(id);
        if (amount > existPoints) {
            throw new RuntimeException("잔액이 부족합니다.");
        }

        long totalAmount = existPoints - amount;
        try {
            return lockByKey.manageLock(id, () -> {
                UserPoint userPoint = userPointRepository.insertOrUpdate(id, totalAmount);
                pointHistoryRepository.insert(id, amount, TransactionType.USE, System.currentTimeMillis());
                return userPoint;
            });
        } catch (InterruptedException e) {
            throw new RuntimeException("관리자에게 문의하십시오.");
        }
    }

    private long selectExistPointsByUserId(long id) {
        return Optional.ofNullable(userPointRepository.selectById(id)).map(UserPoint::point).orElse(0L);
    }
}
