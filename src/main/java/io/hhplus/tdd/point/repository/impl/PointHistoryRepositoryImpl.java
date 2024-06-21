package io.hhplus.tdd.point.repository.impl;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.point.entity.PointHistory;
import io.hhplus.tdd.point.enums.TransactionType;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PointHistoryRepositoryImpl implements PointHistoryRepository {

    private final PointHistoryTable pointHistoryTable;

    @Override
    public List<PointHistory> selectAllByUserId(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }

    @Override
    public void insert(long userId, long amount, TransactionType type, long updateMillis) {
        pointHistoryTable.insert(userId, amount, type, updateMillis);
    }

    @Override
    public long selectAvailableUserPointByUserId(long id) {
        return pointHistoryTable.selectAllByUserId(id)
                .stream()
                .map((ph) -> ph.type().equals(TransactionType.CHARGE) ? ph.amount() :  ph.amount() * -1)
                .reduce(0L, Long::sum);
    }
}
