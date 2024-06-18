package io.hhplus.tdd.point;

import io.hhplus.tdd.point.repository.PointHistoryRepository;
import io.hhplus.tdd.point.repository.UserPointRepository;
import org.junit.jupiter.api.DisplayName;
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
public class PointServiceTest {

    @MockBean
    private UserPointRepository userPointRepository;

    @MockBean
    private PointHistoryRepository pointHistoryRepository;

    @Autowired
    private PointService pointService;

    @Test
    @DisplayName("사용자 충전 내역 없을 시 기본값 리턴")
    void select_userPoint_from_empty_UserPointTable_then_return_emptyUserPoint() {
        // given
        long id = 1;
        UserPoint expectedUserPoint = UserPoint.empty(id);
        given(userPointRepository.selectById(anyLong())).willReturn(expectedUserPoint);

        // when - then
        UserPoint realUserPoint = pointService.point(id);
        assertEquals(expectedUserPoint, realUserPoint);
    }

    @Test
    @DisplayName("사용자 충전 내역 없을 시 빈 내역 리스트 리턴")
    void select_pointHistoryByUser_from_empty_pointHistoryTable_then_return_empty_list() {
        // given
        long id = 1;
        List<PointHistory> expectedPointHistory = new ArrayList<>();
        given(pointHistoryRepository.selectAllByUserId(anyLong())).willReturn(expectedPointHistory);

        // when - then
        List<PointHistory> realPointHistory = pointService.history(id);
        assertTrue(realPointHistory.isEmpty());
    }

    @Test
    @DisplayName("id 1번인 사용자 5000 포인트 충전")
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

    @Test
    @DisplayName("id 1번인 사용자 5000 포인트 2번 충전 후 내역 2 건 모두 조회")
    void user_1_charge_5000_points_twice_and_select_pointHistory() {
        // given
        long id = 1;
        long amount = 5000;
        List<PointHistory> expectedPointHistory = List.of(
                new PointHistory(1, 1, amount, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(1, 1, amount, TransactionType.CHARGE, System.currentTimeMillis()));
        given(pointHistoryRepository.selectAllByUserId(anyLong())).willReturn(expectedPointHistory);

        // when - then
        pointService.charge(id, amount);
        pointService.charge(id, amount);

        List<PointHistory> realPointHistory = pointService.history(id);
        assertFalse(realPointHistory.isEmpty());
        assertEquals(2, realPointHistory.size());
        assertEquals(expectedPointHistory.get(0).amount(), realPointHistory.get(0).amount());
        assertEquals(expectedPointHistory.get(1).amount(), realPointHistory.get(1).amount());
    }

    @Test
    @DisplayName("id 1번인 사용자 5000 포인트 2번 충전 후 포인트 조회")
    void user_1_charge_5000_points_twice_and_select_points() {
        // given
        long id = 1;
        long amount = 5000;
        long times = 2;
        UserPoint expectedUserPoint = new UserPoint(id, amount * times, System.currentTimeMillis());
        given(userPointRepository.selectById(anyLong())).willReturn(expectedUserPoint);

        // when - then
        pointService.charge(id, amount);
        pointService.charge(id, amount);
        UserPoint realUserPoint = pointService.point(id);
        assertEquals(expectedUserPoint.id(), realUserPoint.id());
        assertEquals(expectedUserPoint.point(), realUserPoint.point());
    }
}
