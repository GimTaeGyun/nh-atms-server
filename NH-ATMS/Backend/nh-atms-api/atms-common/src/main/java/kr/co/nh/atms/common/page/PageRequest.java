package kr.co.nh.atms.common.page;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 공통 페이지 요청 DTO
 * - page: 1-base
 * - size: 페이지 크기 (기본 20)
 * - sort: 정렬 컬럼 (예: "ENTER_DATE DESC")
 *
 * Oracle 페이징 시 offset = (page-1) * size, limit = size
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {
    private int page = 1;
    private int size = 20;
    private String sort;

    public int getOffset() {
        return Math.max(0, (page - 1) * size);
    }

    public int getLimit() {
        return Math.max(1, size);
    }
}
