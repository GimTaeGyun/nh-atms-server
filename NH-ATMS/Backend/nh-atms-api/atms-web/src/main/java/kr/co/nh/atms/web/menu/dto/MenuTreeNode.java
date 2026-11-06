package kr.co.nh.atms.web.menu.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** 메뉴 트리 노드 (프론트 사이드 메뉴 렌더링용) */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MenuTreeNode {
    private Long menuNo;
    private Long parentNo;
    private String menuName;
    private String menuUrl;
    private Integer menuLevel;
    private Integer menuOrder;
    private String menuIcon;
    private String menuType;

    @Builder.Default
    private List<MenuTreeNode> children = new ArrayList<>();
}
