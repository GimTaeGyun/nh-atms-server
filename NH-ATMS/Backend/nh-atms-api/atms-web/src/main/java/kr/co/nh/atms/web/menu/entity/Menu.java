package kr.co.nh.atms.web.menu.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** T_MENU (메뉴 트리) — PK: menuNo */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Menu {
    private Long menuNo;
    private Long parentNo;
    private String menuName;
    private String menuUrl;
    private Integer menuLevel;
    private Integer menuOrder;
    private String menuIcon;
    private String menuType;
    private String visibleYn;
    private String useYn;
    private String description;
    private LocalDateTime regDt;
    private LocalDateTime updDt;
}
