package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.dto.PointHistoryDto;
import io.hhplus.tdd.point.dto.UserPointDto;
import io.hhplus.tdd.point.enums.TransactionType;
import io.hhplus.tdd.point.entity.UserPoint;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import io.hhplus.tdd.utils.LockByKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final LockByKey lockByKey;

    public UserPointDto point(long id) {
        return UserPointDto.from(userPointRepository.selectById(id));
    }

    public List<PointHistoryDto> history(long id) {
        return pointHistoryRepository.selectAllByUserId(id).stream().map(PointHistoryDto::from).collect(Collectors.toList());
    }

    public UserPointDto charge(long id, long amount)  {
        if (amount <= 0) {
            throw new RuntimeException("충전 금액은 0보다 커야 합니다.");
        }
        long existPoints = this.selectExistPointsByUserId(id);
        long totalAmount = existPoints + amount;
        try {
            return lockByKey.manageLock(id, () -> {
                UserPoint userPoint = userPointRepository.insertOrUpdate(id, totalAmount);
                pointHistoryRepository.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
                return UserPointDto.from(userPoint);
            });
        } catch (InterruptedException e) {
            throw new RuntimeException("관리자에게 문의하십시오.");
        }
    }

    public UserPointDto use(long id, long amount) {
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
                return UserPointDto.from(userPoint);
            });
        } catch (InterruptedException e) {
            throw new RuntimeException("관리자에게 문의하십시오.");
        }
    }

    private long selectExistPointsByUserId(long id) {
        return Optional.ofNullable(userPointRepository.selectById(id)).map(UserPoint::point).orElse(0L);
    }
}
