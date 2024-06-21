package io.hhplus.tdd.point.dto;

import io.hhplus.tdd.point.entity.PointHistory;
import io.hhplus.tdd.point.enums.TransactionType;
import lombok.Data;

@Data
public class PointHistoryDto {
    private long id;
    private long userId;
    private long amount;
    private TransactionType type;
    private long updateMillis;

    private PointHistoryDto() {};

    public PointHistoryDto(long id, long userId, long amount, TransactionType type, long updateMillis) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.updateMillis = updateMillis;
    }

    public static PointHistoryDto from(final PointHistory pointHistory) {
        return new PointHistoryDto(
                pointHistory.id(),
                pointHistory.userId(),
                pointHistory.amount(),
                pointHistory.type(),
                pointHistory.updateMillis()
        );
    }
}
