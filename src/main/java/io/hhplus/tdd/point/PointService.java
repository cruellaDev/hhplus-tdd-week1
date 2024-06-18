package io.hhplus.tdd.point;

import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointRepository userPointRepository;

    private final PointHistoryRepository pointHistoryRepository;

    public UserPoint point(long id) {
        return userPointRepository.selectById(id);
    }

    public List<PointHistory> history(long id) {
        return pointHistoryRepository.selectAllByUserId(id);
    }

    public UserPoint charge(long id, long amount) {
        long existsPoints = Optional.ofNullable(userPointRepository.selectById(id)).map(UserPoint::point).orElse(0L);
        long totalAmount = existsPoints + amount;
        UserPoint userPoint = userPointRepository.insertOrUpdate(id, totalAmount);
        pointHistoryRepository.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

        return userPoint;
    }
}
