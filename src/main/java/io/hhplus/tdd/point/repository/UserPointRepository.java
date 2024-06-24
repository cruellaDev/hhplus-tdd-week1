package io.hhplus.tdd.point.repository;

import io.hhplus.tdd.point.entity.UserPoint;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPointRepository {
    UserPoint selectById(long id);
    UserPoint insertOrUpdate(long id, long amount);
}
