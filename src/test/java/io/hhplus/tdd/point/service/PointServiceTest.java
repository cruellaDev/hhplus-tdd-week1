package io.hhplus.tdd.point.service;

import io.hhplus.tdd.point.dto.PointHistoryDto;
import io.hhplus.tdd.point.dto.UserPointDto;
import io.hhplus.tdd.point.entity.PointHistory;
import io.hhplus.tdd.point.enums.TransactionType;
import io.hhplus.tdd.point.entity.UserPoint;
import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import io.hhplus.tdd.utils.LockByKey;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    private UserPointRepository userPointRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @Mock
    private LockByKey lockByKey;

    /**
     * 사용자 충전 내역 없을 시 기본값 리턴
     */
    @Test
    void select_userPoint_from_empty_UserPointTable_then_return_emptyUserPoint() {
        // given
        long id = 1;
        UserPoint expectedUserPoint = UserPoint.empty(id);
        given(userPointRepository.selectById(anyLong())).willReturn(expectedUserPoint);

        // when
        UserPointDto realUserPoint = pointService.point(id);

        // then
        assertEquals(expectedUserPoint.id(), realUserPoint.getId());
        assertEquals(expectedUserPoint.point(), realUserPoint.getPoint());
    }

    /**
     * 사용자 충전 내역 없을 시 빈 내역 리스트 리턴
     */
    @Test
    void select_pointHistoryByUser_from_empty_pointHistoryTable_then_return_empty_list() {
        // given
        long id = 1;
        List<PointHistory> expectedPointHistory = new ArrayList<>();
        given(pointHistoryRepository.selectAllByUserId(anyLong())).willReturn(expectedPointHistory);

        // when
        List<PointHistoryDto> realPointHistory = pointService.history(id);

        // then
        assertTrue(realPointHistory.isEmpty());
    }

    /**
     * id 1번인 사용자 5000 포인트 충전
     */
    @Test
    void user_1_charge_5000_points() throws InterruptedException {
        // given
        long id = 1;
        long amount = 5000;

        UserPointDto expectedUserPoint = UserPointDto.from(new UserPoint(id, amount, System.currentTimeMillis()));
        given(lockByKey.manageLock(anyLong(), any())).willAnswer(invocation -> expectedUserPoint);

        // when - then
        UserPointDto realUserPoint = pointService.charge(id, amount);
        assertEquals(expectedUserPoint.getId(), realUserPoint.getId());
        assertEquals(expectedUserPoint.getPoint(), realUserPoint.getPoint());
    }

    /**
     * id 1번인 사용자 5000 포인트 2번 충전 후 내역 조회
     */
    @Test
    void user_1_charge_5000_points_twice_and_select_pointHistory() throws InterruptedException {
        // given
        long id = 1;
        long amount = 5000;
        long times = 2;

        UserPointDto expectedUserPoint = UserPointDto.from(new UserPoint(id, amount * times, System.currentTimeMillis()));
        List<PointHistory> expectedPointHistory = List.of(
                new PointHistory(1, 1, amount, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(1, 1, amount, TransactionType.CHARGE, System.currentTimeMillis()));
        given(pointHistoryRepository.selectAllByUserId(anyLong())).willReturn(expectedPointHistory);
        given(lockByKey.manageLock(anyLong(), any())).willAnswer(invocation -> expectedUserPoint);

        // when
        pointService.charge(id, amount);
        pointService.charge(id, amount);

        // then
        List<PointHistoryDto> realPointHistory = pointService.history(id);
        assertFalse(realPointHistory.isEmpty());
        assertEquals(2, realPointHistory.size());
        assertEquals(expectedPointHistory.get(0).amount(), realPointHistory.get(0).getAmount());
        assertEquals(expectedPointHistory.get(1).amount(), realPointHistory.get(1).getAmount());
    }

    /**
     * id 1번인 사용자 5000 포인트 2번 충전
     */
    @Test
    void user_1_charge_5000_points_twice() throws InterruptedException {
        // given
        long id = 1;
        long amount = 5000;
        long times = 2;

        UserPoint expectedUserPoint = new UserPoint(id, amount * times, System.currentTimeMillis());
        given(userPointRepository.selectById(anyLong())).willReturn(expectedUserPoint);
        given(lockByKey.manageLock(anyLong(), any())).willAnswer(invocation -> UserPointDto.from(expectedUserPoint));

        // when
        pointService.charge(id, amount);
        pointService.charge(id, amount);
        UserPointDto realUserPoint = pointService.point(id);

        // then
        assertEquals(expectedUserPoint.id(), realUserPoint.getId());
        assertEquals(expectedUserPoint.point(), realUserPoint.getPoint());
    }

    /**
     * id 1번인 사용자 1000 포인트 충전하고 5000 포인트 사용 시도 시 잔액 부족 오류
     * 남는 잔액은 1000 그대로 남아야 함
     */
    @Test
    void user_1_charge_1000_points_and_use_5000_points_then_throw_exception() throws InterruptedException {
        // given
        long id = 1;
        long chargeAmount = 1000;
        long spentAmount = 5000;

        UserPoint expectedUserPoint = new UserPoint(id, chargeAmount, System.currentTimeMillis());
        given(userPointRepository.selectById(anyLong())).willReturn(expectedUserPoint);
        given(lockByKey.manageLock(anyLong(), any())).willAnswer(invocation -> UserPointDto.from(expectedUserPoint));

        // when
        pointService.charge(id, chargeAmount);
        assertThrows(RuntimeException.class, () -> pointService.use(id, spentAmount));

        UserPointDto realUserPoint = pointService.point(id);

        // then
        assertEquals(expectedUserPoint.id(), realUserPoint.getId());
        assertEquals(expectedUserPoint.point(), realUserPoint.getPoint());
    }

    /**
     * id 1번인 사용자 5000 포인트 충전하고 1000 포인트 사용 시 잔액 포인트가 포인트 내역 합계와 일치
     */
    @Test
    void user_1_charge_1000_points_and_use_5000_points_and_select_pointHistory() throws InterruptedException {
        // given
        long id = 1;
        long chargeAmount = 5000;
        long spentAmount = 1000;

        UserPoint expectedUserPoint = new UserPoint(id, chargeAmount - spentAmount, System.currentTimeMillis());
        given(userPointRepository.selectById(anyLong())).willReturn(expectedUserPoint);
        given(pointHistoryRepository.selectAvailableUserPointByUserId(anyLong())).willReturn(chargeAmount - spentAmount);
        given(lockByKey.manageLock(anyLong(), any())).willAnswer(invocation -> UserPointDto.from(expectedUserPoint));

        // when
        pointService.charge(id, chargeAmount);
        pointService.use(id, spentAmount);

        long availablePoints = pointHistoryRepository.selectAvailableUserPointByUserId(id);
        UserPointDto realUserPoint = pointService.point(id);

        // then
        assertEquals(4000, availablePoints);
        assertEquals(4000, realUserPoint.getPoint());
    }

    /**
     * 충전 금액이 0 이하일 시 예외를 던진다.
     */
    @Test
    void charge_points_less_then_zero_throw_exception() {
        // given
        long id = 1;
        long amount = -1000;

        // when - then
        assertThrows(RuntimeException.class, () -> pointService.charge(id, amount));
    }

    /**
     * 사용 금액이 0 이하일 시 예외를 던진다.
     */
    @Test
    void use_points_less_then_zero_throw_exception() {
        // given
        long id = 1;
        long amount = -1000;

        // when - then
        assertThrows(RuntimeException.class, () -> pointService.use(id, amount));
    }

}
