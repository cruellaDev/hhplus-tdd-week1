package io.hhplus.tdd.point;

import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.anyLong;
import static org.mockito.BDDMockito.given;

@WebMvcTest(PointService.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PointServiceTest {

    @MockBean
    private UserPointRepository userPointRepository;

    @MockBean
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private PointService pointService;

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
        UserPoint realUserPoint = pointService.point(id);

        // then
        assertEquals(expectedUserPoint, realUserPoint);
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
        List<PointHistory> realPointHistory = pointService.history(id);

        // then
        assertTrue(realPointHistory.isEmpty());
    }

    /**
     * id 1번인 사용자 5000 포인트 충전
     */
    @Test
    void user_1_charge_5000_points() {
        // given
        long id = 1;
        long amount = 5000;

        UserPoint expectedUserPoint = new UserPoint(id, amount, System.currentTimeMillis());
        given(userPointRepository.insertOrUpdate(anyLong(), anyLong())).willReturn(expectedUserPoint);

        // when - then
        UserPoint realUserPoint = pointService.charge(id, amount);
        assertEquals(expectedUserPoint.id(), realUserPoint.id());
        assertEquals(expectedUserPoint.point(), realUserPoint.point());
    }

    /**
     * id 1번인 사용자 5000 포인트 2번 충전 후 내역 조회
     */
    @Test
    void user_1_charge_5000_points_twice_and_select_pointHistory() {
        // given
        long id = 1;
        long amount = 5000;
        long times = 2;

        UserPoint expectedUserPoint = new UserPoint(id, amount * times, System.currentTimeMillis());
        List<PointHistory> expectedPointHistory = List.of(
                new PointHistory(1, 1, amount, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(1, 1, amount, TransactionType.CHARGE, System.currentTimeMillis()));
        given(userPointRepository.insertOrUpdate(anyLong(), anyLong())).willReturn(expectedUserPoint);
        given(pointHistoryRepository.selectAllByUserId(anyLong())).willReturn(expectedPointHistory);

        // when
        pointService.charge(id, amount);
        pointService.charge(id, amount);

        // then
        List<PointHistory> realPointHistory = pointService.history(id);
        assertFalse(realPointHistory.isEmpty());
        assertEquals(2, realPointHistory.size());
        assertEquals(expectedPointHistory.get(0).amount(), realPointHistory.get(0).amount());
        assertEquals(expectedPointHistory.get(1).amount(), realPointHistory.get(1).amount());
    }

    /**
     * id 1번인 사용자 5000 포인트 2번 충전
     */
    @Test
    void user_1_charge_5000_points_twice() {
        // given
        long id = 1;
        long amount = 5000;
        long times = 2;

        UserPoint expectedUserPoint = new UserPoint(id, amount * times, System.currentTimeMillis());
        given(userPointRepository.insertOrUpdate(anyLong(), anyLong())).willReturn(expectedUserPoint);
        given(userPointRepository.selectById(anyLong())).willReturn(expectedUserPoint);

        // when
        pointService.charge(id, amount);
        pointService.charge(id, amount);
        UserPoint realUserPoint = pointService.point(id);

        // then
        assertEquals(expectedUserPoint.id(), realUserPoint.id());
        assertEquals(expectedUserPoint.point(), realUserPoint.point());
    }

    /**
     * id 1번인 사용자 1000 포인트 충전하고 5000 포인트 사용 시도 시 잔액 부족 오류
     * 남는 잔액은 1000 그대로 남아야 함
     */
    @Test
    void user_1_charge_1000_points_and_use_5000_points_then_throw_exception() {
        // given
        long id = 1;
        long chargeAmount = 1000;
        long spentAmount = 5000;

        UserPoint expectedUserPoint = new UserPoint(id, chargeAmount, System.currentTimeMillis());
        given(userPointRepository.insertOrUpdate(anyLong(), anyLong())).willReturn(expectedUserPoint);
        given(userPointRepository.selectById(anyLong())).willReturn(expectedUserPoint);

        // when
        pointService.charge(id, chargeAmount);
        assertThrows(RuntimeException.class, () -> pointService.use(id, spentAmount));

        UserPoint realUserPoint = pointService.point(id);

        // then
        assertEquals(expectedUserPoint.id(), realUserPoint.id());
        assertEquals(expectedUserPoint.point(), realUserPoint.point());
    }

    /**
     * id 1번인 사용자 5000 포인트 충전하고 1000 포인트 사용 시 잔액 포인트가 포인트 내역 합계와 일치
     */
    @Test
    void user_1_charge_1000_points_and_use_5000_points_and_select_pointHistory() {
        // given
        long id = 1;
        long chargeAmount = 5000;
        long spentAmount = 1000;

        UserPoint expectedUserPoint = new UserPoint(id, chargeAmount - spentAmount, System.currentTimeMillis());
        given(userPointRepository.insertOrUpdate(anyLong(), anyLong())).willReturn(expectedUserPoint);
        given(userPointRepository.selectById(anyLong())).willReturn(expectedUserPoint);
        given(pointHistoryRepository.selectAvailableUserPointByUserId(anyLong())).willReturn(chargeAmount - spentAmount);

        // when
        pointService.charge(id, chargeAmount);
        pointService.use(id, spentAmount);

        long availablePoints = pointHistoryRepository.selectAvailableUserPointByUserId(id);
        UserPoint realUserPoint = pointService.point(id);

        // then
        assertEquals(4000, availablePoints);
        assertEquals(4000, realUserPoint.point());
    }

}
