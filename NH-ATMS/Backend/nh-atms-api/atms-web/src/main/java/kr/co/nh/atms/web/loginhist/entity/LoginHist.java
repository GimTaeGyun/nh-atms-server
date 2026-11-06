package kr.co.nh.atms.web.loginhist.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** T_LOGIN_HIST */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginHist {
    private Long loginSeq;
    private String userId;
    private LocalDateTime loginDt;
    private LocalDateTime logoutDt;
    private String loginIp;
    private String loginResult;   // S/F
    private String failReason;
    private String userAgent;
}
