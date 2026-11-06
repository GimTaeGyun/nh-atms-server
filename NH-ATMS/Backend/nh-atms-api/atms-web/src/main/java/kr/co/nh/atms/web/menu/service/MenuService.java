package kr.co.nh.atms.web.menu.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.web.menu.dto.MenuTreeNode;
import kr.co.nh.atms.web.menu.entity.Menu;
import kr.co.nh.atms.web.menu.mapper.MenuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuMapper menuMapper;

    /** 로그인 사용자의 권한 기반 메뉴 트리 */
    public List<MenuTreeNode> getMyMenuTree(String userId) {
        List<Menu> flat = menuMapper.selectAccessibleMenusByUserId(userId);
        return buildTree(flat);
    }

    /** 전체 메뉴 트리 (관리자) */
    public List<MenuTreeNode> getAllMenuTree() {
        return buildTree(menuMapper.selectAllActiveMenus());
    }

    public Menu getOne(Long menuNo) {
        return menuMapper.selectOne(menuNo);
    }

    @Transactional
    public Menu create(Menu menu) {
        if (menu.getMenuLevel() == null) {
            menu.setMenuLevel(menu.getParentNo() == null ? 1 : 2);
        }
        menuMapper.insert(menu);
        // 상위 메뉴의 권한코드를 신규 메뉴에 복사 → 사이드바 자동 노출
        if (menu.getParentNo() != null) {
            menuMapper.copyAuthMenuFromParent(menu.getParentNo(), menu.getMenuNo());
        }
        return menu;
    }

    @Transactional
    public void update(Menu menu) {
        menuMapper.update(menu);
    }

    @Transactional
    public void delete(Long menuNo) {
        int childCount = menuMapper.countChildren(menuNo);
        if (childCount > 0) {
            throw new BusinessException("MNU_001", "하위 메뉴가 존재하여 삭제할 수 없습니다.");
        }
        menuMapper.softDelete(menuNo);
    }

    /** 플랫 목록 → 부모/자식 트리 */
    private List<MenuTreeNode> buildTree(List<Menu> flat) {
        if (flat == null || flat.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, MenuTreeNode> map = new HashMap<>();
        for (Menu m : flat) {
            map.put(m.getMenuNo(), toNode(m));
        }

        List<MenuTreeNode> roots = new ArrayList<>();
        for (Menu m : flat) {
            MenuTreeNode node = map.get(m.getMenuNo());
            if (m.getParentNo() == null) {
                roots.add(node);
            } else {
                MenuTreeNode parent = map.get(m.getParentNo());
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    // 부모가 권한에 없는 경우(상위 메뉴는 보이지 않지만 자식만 접근 가능) — 루트로 승격
                    roots.add(node);
                }
            }
        }

        Comparator<MenuTreeNode> byOrder = Comparator
                .comparing(MenuTreeNode::getMenuOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MenuTreeNode::getMenuNo);
        sortRecursive(roots, byOrder);
        return roots;
    }

    private void sortRecursive(List<MenuTreeNode> nodes, Comparator<MenuTreeNode> cmp) {
        nodes.sort(cmp);
        for (MenuTreeNode n : nodes) {
            if (n.getChildren() != null && !n.getChildren().isEmpty()) {
                sortRecursive(n.getChildren(), cmp);
            }
        }
    }

    private MenuTreeNode toNode(Menu m) {
        return MenuTreeNode.builder()
                .menuNo(m.getMenuNo())
                .parentNo(m.getParentNo())
                .menuName(m.getMenuName())
                .menuUrl(m.getMenuUrl())
                .menuLevel(m.getMenuLevel())
                .menuOrder(m.getMenuOrder())
                .menuIcon(m.getMenuIcon())
                .menuType(m.getMenuType())
                .children(new ArrayList<>())
                .build();
    }
}
