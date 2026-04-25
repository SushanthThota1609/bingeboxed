package com.bingeboxed.recommendations.dto;

import java.math.BigDecimal;

public class ReasonResponse {

    private String reason;
    private BigDecimal score;

    public ReasonResponse() {}

    public ReasonResponse(String reason, BigDecimal score) {
        this.reason = reason;
        this.score = score;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
}
