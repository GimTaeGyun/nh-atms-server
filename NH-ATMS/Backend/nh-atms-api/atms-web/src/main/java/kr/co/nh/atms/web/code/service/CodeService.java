package kr.co.nh.atms.web.code.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.code.dto.CodeGroupDto;
import kr.co.nh.atms.web.code.dto.CodeItemDto;
import kr.co.nh.atms.web.code.dto.CodeSearchDto;
import kr.co.nh.atms.web.code.entity.Code;
import kr.co.nh.atms.web.code.entity.CodeGroup;
import kr.co.nh.atms.web.code.mapper.CodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodeService {
    private final CodeMapper mapper;

    // ── T_CODE 조회 ──────────────────────────────────────────────────────────

    public PageResult<CodeItemDto> search(CodeSearchDto s) {
        List<CodeItemDto> dtos = mapper.selectCodeList(s).stream()
                .map(this::toItemDto).collect(Collectors.toList());
        return PageResult.of(dtos, mapper.countCodeList(s), s);
    }

    public CodeItemDto get(String langCd, String groupCd, String code) {
        Code c = mapper.selectCodeOne(langCd, groupCd, code);
        if (c == null) throw new BusinessException("COD_001", "공통코드를 찾을 수 없습니다.");
        return toItemDto(c);
    }

    public List<CodeItemDto> getByGroup(String langCd, String groupCd) {
        return mapper.selectCodeByGroup(langCd, groupCd).stream()
                .map(this::toItemDto).collect(Collectors.toList());
    }

    // ── T_CODE 쓰기 ──────────────────────────────────────────────────────────

    @Transactional
    public void create(Code c) {
        if (!StringUtils.hasText(c.getLangCd()) || !StringUtils.hasText(c.getGroupCd()) || !StringUtils.hasText(c.getCode()))
            throw new BusinessException("COD_003", "언어코드, 그룹코드, 코드는 필수입니다.");
        if (mapper.selectGroupOne(c.getLangCd(), c.getGroupCd()) == null)
            throw new BusinessException("COG_001", "존재하지 않는 코드 그룹입니다.");
        if (mapper.selectCodeOne(c.getLangCd(), c.getGroupCd(), c.getCode()) != null)
            throw new BusinessException("COD_002", "이미 존재하는 공통코드입니다.");
        mapper.insertCode(c);
    }

    @Transactional
    public void update(Code c) {
        if (mapper.updateCode(c) == 0) throw new BusinessException("COD_001", "공통코드를 찾을 수 없습니다.");
    }

    @Transactional
    public void delete(String langCd, String groupCd, String code) {
        if (mapper.deleteCode(langCd, groupCd, code) == 0)
            throw new BusinessException("COD_001", "공통코드를 찾을 수 없습니다.");
    }

    // ── T_CODE_GROUP 조회 ─────────────────────────────────────────────────────

    public List<CodeGroupDto> getAllGroups() {
        return mapper.selectGroupList().stream()
                .map(this::toGroupDto).collect(Collectors.toList());
    }

    public CodeGroupDto getGroup(String langCd, String groupCd) {
        CodeGroup g = mapper.selectGroupOne(langCd, groupCd);
        if (g == null) throw new BusinessException("COG_001", "코드 그룹을 찾을 수 없습니다.");
        return toGroupDto(g);
    }

    // ── T_CODE_GROUP 쓰기 ─────────────────────────────────────────────────────

    @Transactional
    public void createGroup(CodeGroup g) {
        if (!StringUtils.hasText(g.getLangCd()))
            throw new BusinessException("COG_003", "언어코드는 필수입니다.");
        if (!StringUtils.hasText(g.getGroupCd()))
            throw new BusinessException("COG_003", "그룹코드는 필수입니다.");
        if (!StringUtils.hasText(g.getGroupName()))
            throw new BusinessException("COG_004", "그룹명은 필수입니다.");
        if (mapper.selectGroupOne(g.getLangCd(), g.getGroupCd()) != null)
            throw new BusinessException("COG_002", "이미 존재하는 코드 그룹입니다.");
        mapper.insertGroup(g);
    }

    @Transactional
    public void updateGroup(CodeGroup g) {
        if (!StringUtils.hasText(g.getGroupName()))
            throw new BusinessException("COG_004", "그룹명은 필수입니다.");
        if (mapper.updateGroup(g) == 0)
            throw new BusinessException("COG_001", "코드 그룹을 찾을 수 없습니다.");
    }

    @Transactional
    public void deleteGroup(String langCd, String groupCd) {
        if (mapper.countCodesByGroup(langCd, groupCd) > 0)
            throw new BusinessException("COG_005", "해당 그룹에 속한 코드가 있어 삭제할 수 없습니다.");
        if (mapper.deleteGroup(langCd, groupCd) == 0)
            throw new BusinessException("COG_001", "코드 그룹을 찾을 수 없습니다.");
    }

    // ── Entity → DTO 변환 ────────────────────────────────────────────────────

    private CodeItemDto toItemDto(Code c) {
        CodeItemDto dto = new CodeItemDto();
        dto.setLangCd(c.getLangCd());
        dto.setGroupCd(c.getGroupCd());
        dto.setCode(c.getCode());
        dto.setName(c.getName());
        dto.setOrderNo(c.getOrderNo());
        return dto;
    }

    private CodeGroupDto toGroupDto(CodeGroup g) {
        CodeGroupDto dto = new CodeGroupDto();
        dto.setLangCd(g.getLangCd());
        dto.setGroupCd(g.getGroupCd());
        dto.setGroupName(g.getGroupName());
        return dto;
    }
}
