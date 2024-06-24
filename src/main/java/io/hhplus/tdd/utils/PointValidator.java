package io.hhplus.tdd.utils;

import lombok.Getter;
import org.springframework.stereotype.Component;

@Component
public class PointValidator {

    @Getter
    public static class Balance {
        private long id;
        private long balance;
        private boolean available;
    }
}
