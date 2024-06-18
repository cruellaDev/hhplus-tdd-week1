package io.hhplus.tdd.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.BDDMockito.anyLong;
import static org.mockito.BDDMockito.given;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

@WebMvcTest(PointController.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PointController pointController;

    @MockBean
    private PointService pointService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pointController).build();
    }

    private void checkGetMethodApiIsOkStatusAndPrintResponse(String urlTemplate, Object... uriVariables) throws Exception {
        mockMvc.perform(get(urlTemplate, uriVariables))
                .andExpect(status().isOk())
                .andDo(print());
    }

    /**
     * 사용자 포인트 조회 호출 ok 확인
     * @throws Exception
     */
    @Test
    void call_getUserPointApi_when_status_is_ok_then_success() throws Exception {
        checkGetMethodApiIsOkStatusAndPrintResponse("/point/{id}", 0);
    }

    /**
     * 사용자 포인트 이용 내역 조회 호출 status ok 확인
     * @throws Exception
     */
    @Test
    void call_getUserPointHistoryApi_when_status_is_ok_then_success() throws Exception {
        checkGetMethodApiIsOkStatusAndPrintResponse("/point/{id}/histories", 0);
    }

    /**
     * 사용자 충전 내역 없을 시 사용자 포인트 기본값 리턴
     * @throws Exception
     */
    @Test
    void call_getUserPointApi_when_no_one_charge_then_return_empty_userPoint() throws Exception{
        // given
        long id = 1;
        UserPoint expectedUserPoint = UserPoint.empty(id);
        given(pointService.point(anyLong())).willReturn(expectedUserPoint);

        // when - then
        MvcResult mvcResult = mockMvc.perform(get("/point/{id}", id))
                .andExpect(status().isOk())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        UserPoint realUserPoint = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserPoint.class);

        assertEquals(expectedUserPoint.id(), realUserPoint.id());
        assertEquals(expectedUserPoint.point(), realUserPoint.point());
    }

    /**
     * 사용자 충전 내역 없을 시 빈 내역 리스트 리턴
     * @throws Exception
     */
    @Test
    void call_getPointHistories_when_no_one_charge_then_return_empty_list() throws Exception {
        // given
        long id = 1;
        given(pointService.history(anyLong())).willReturn(List.of());

        // when - then
        MvcResult mvcResult = mockMvc.perform(get("/point/{id}/histories", id))
                .andExpect(status().isOk())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        List<PointHistory> pointHistoryList = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), List.class);

        assertTrue(pointHistoryList.isEmpty());
    }
}
