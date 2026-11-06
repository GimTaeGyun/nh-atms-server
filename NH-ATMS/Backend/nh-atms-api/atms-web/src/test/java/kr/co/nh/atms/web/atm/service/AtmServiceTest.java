package kr.co.nh.atms.web.atm.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.atm.dto.AtmSearchDto;
import kr.co.nh.atms.web.atm.entity.Atm;
import kr.co.nh.atms.web.atm.mapper.AtmMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AtmService 단위 테스트")
class AtmServiceTest {

    @Mock private AtmMapper atmMapper;
    @InjectMocks private AtmService service;

    @Test
    @DisplayName("search: 페이징 결과를 PageResult로 감싸 반환한다")
    void search_returnsPageResult() {
        AtmSearchDto dto = new AtmSearchDto();
        Atm a = Atm.builder().termGroupId("G1").termId("T1").build();
        given(atmMapper.selectList(dto)).willReturn(List.of(a));
        given(atmMapper.countList(dto)).willReturn(1L);

        PageResult<Atm> result = service.search(dto);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalCount()).isEqualTo(1L);
        assertThat(result.getPage()).isEqualTo(1);
    }

    @Test
    @DisplayName("get: 미존재 시 ATM_001 BusinessException 발생")
    void get_notFound_throws() {
        given(atmMapper.selectOne("G1", "T1")).willReturn(null);

        assertThatThrownBy(() -> service.get("G1", "T1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("찾을 수 없습니다");
    }

    @Test
    @DisplayName("create: 중복 PK이면 ATM_002 발생, insert 호출 안됨")
    void create_duplicate_throws() {
        Atm a = Atm.builder().termGroupId("G1").termId("T1").build();
        given(atmMapper.selectOne("G1", "T1")).willReturn(a);

        assertThatThrownBy(() -> service.create(a))
                .isInstanceOf(BusinessException.class);

        verify(atmMapper, never()).insert(any());
    }

    @Test
    @DisplayName("create: 정상 등록 시 mapper.insert 1회 호출")
    void create_success() {
        Atm a = Atm.builder().termGroupId("G1").termId("T1").build();
        given(atmMapper.selectOne("G1", "T1")).willReturn(null);
        given(atmMapper.insert(a)).willReturn(1);

        service.create(a);

        verify(atmMapper).insert(a);
    }
}
