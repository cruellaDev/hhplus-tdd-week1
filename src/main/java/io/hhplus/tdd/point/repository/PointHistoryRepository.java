package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointHistoryRepository {
    List<PointHistory> selectAllByUserId(long id);
    PointHistory insert(long userId, long amount, TransactionType type, long updateMillis);
}
