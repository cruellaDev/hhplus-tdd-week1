package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.point.entity.PointHistory;
import io.hhplus.tdd.point.enums.TransactionType;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointHistoryRepository {
    List<PointHistory> selectAllByUserId(long id);
    void insert(long userId, long amount, TransactionType type, long updateMillis);
    long selectAvailableUserPointByUserId(long id);
}
