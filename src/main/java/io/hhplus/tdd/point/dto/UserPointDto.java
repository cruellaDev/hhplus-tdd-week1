package io.hhplus.tdd.point.dto;

import io.hhplus.tdd.point.entity.UserPoint;
import lombok.Data;

@Data
public class UserPointDto {
    private long id;
    private long point;
    private long updateMillis;

    private UserPointDto() {};

    public UserPointDto(long id, long point, long updateMillis) {
        this.id = id;
        this.point = point;
        this.updateMillis = updateMillis;
    }

    public static UserPointDto from(final UserPoint userPoint) {
        return new UserPointDto(
            userPoint.id(),
            userPoint.point(),
            userPoint.updateMillis()
        );
    }
}
