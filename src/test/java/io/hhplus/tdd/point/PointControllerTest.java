package io.hhplus.tdd.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

@WebMvcTest(PointController.class)
@AutoConfigureMockMvc
public class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PointController pointController;

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
    @DisplayName("사용자 포인트 조회 호출 status ok 확인")
    public void call_getUserPointApi_when_status_is_ok_then_success() throws Exception {
        checkGetMethodApiIsOkStatusAndPrintResponse("/point/{id}", 0);
    }

    /**
     * 사용자 포인트 이용 내역 조회 호출 status ok 확인
     * @throws Exception
     */
    @Test
    @DisplayName("사용자 포인트 이용 내역 조회 호출 status ok 확인")
    public void call_getUserPointHistoryApi_when_status_is_ok_then_success() throws Exception {
        checkGetMethodApiIsOkStatusAndPrintResponse("/point/{id}/histories", 0);
    }

    @Test
    @DisplayName("사용자 포인트 이용 내역 조회 호출 status ok 확인")
    public void call_getUserPointApi_when_no_one_charge_then_return_empty_userPoint() throws Exception{
        MvcResult mvcResult = mockMvc.perform(get("/point/{id}", 0))
                .andExpect(status().isOk())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        UserPoint userPoint = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserPoint.class);

        assertEquals(userPoint.id(), 0);
        assertEquals(userPoint.point(), 0);
        assertEquals(userPoint.updateMillis(), 0);
    }
}
