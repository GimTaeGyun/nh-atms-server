package kr.co.nh.atms.web.notice.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.web.notice.entity.Notice;
import kr.co.nh.atms.web.notice.mapper.NoticeMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoticeService 단위 테스트")
class NoticeServiceTest {

    @Mock private NoticeMapper mapper;
    @InjectMocks private NoticeService service;

    @Test
    @DisplayName("create: nextSeqNo로 채번 후 insert 실행, useYn 미지정 시 'Y' 기본값")
    void create_assignsSeqAndDefaultsUseYn() {
        Notice n = Notice.builder().catCd("NTC").subject("제목").build();
        given(mapper.nextSeqNo("NTC")).willReturn(7L);

        Long seq = service.create(n);

        assertThat(seq).isEqualTo(7L);
        assertThat(n.getSeqNo()).isEqualTo(7L);
        assertThat(n.getUseYn()).isEqualTo("Y");
        verify(mapper).insert(n);
    }

    @Test
    @DisplayName("get: 조회 성공 시 hit++ 호출")
    void get_increasesHit() {
        Notice n = Notice.builder().catCd("NTC").seqNo(1L).subject("hi").hit(5L).build();
        given(mapper.selectOne("NTC", 1L)).willReturn(n);

        Notice result = service.get("NTC", 1L);

        assertThat(result).isSameAs(n);
        verify(mapper).increaseHit("NTC", 1L);
    }

    @Test
    @DisplayName("get: 미존재 시 NTC_001 발생")
    void get_notFound() {
        given(mapper.selectOne("NTC", 99L)).willReturn(null);

        assertThatThrownBy(() -> service.get("NTC", 99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("공지사항");
    }
}
