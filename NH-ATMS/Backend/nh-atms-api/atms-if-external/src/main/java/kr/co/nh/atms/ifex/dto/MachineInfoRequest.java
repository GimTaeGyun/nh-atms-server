package kr.co.nh.atms.ifex.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 기기정보 통보 (machineInfoNotification.do / IQ_04001140).
 */
@Data
public class MachineInfoRequest {

    @NotBlank(message = "termGroupId(영업점코드)는 필수입니다.")
    private String termGroupId;

    @NotBlank(message = "termId(단말번호)는 필수입니다.")
    private String termId;

    private String machineModel;
    private String vendorCd;
    private String installDate;
}
