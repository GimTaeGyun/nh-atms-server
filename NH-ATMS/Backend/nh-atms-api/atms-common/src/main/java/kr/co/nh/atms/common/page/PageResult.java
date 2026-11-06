package kr.co.nh.atms.common.page;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 공통 페이지 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private List<T> content;
    private long totalCount;
    private int page;
    private int size;
    private int totalPages;

    public static <T> PageResult<T> of(List<T> content, long totalCount, PageRequest req) {
        int totalPages = (int) Math.ceil((double) totalCount / Math.max(1, req.getSize()));
        return PageResult.<T>builder()
                .content(content)
                .totalCount(totalCount)
                .page(req.getPage())
                .size(req.getSize())
                .totalPages(totalPages)
                .build();
    }
}
