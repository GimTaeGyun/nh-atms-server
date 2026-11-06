package kr.co.nh.atms.web.code.mapper;

import kr.co.nh.atms.web.code.dto.CodeSearchDto;
import kr.co.nh.atms.web.code.entity.Code;
import kr.co.nh.atms.web.code.entity.CodeGroup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CodeMapper {

    // ── T_CODE ───────────────────────────────────────────────────────────────
    List<Code> selectCodeList(CodeSearchDto search);
    long countCodeList(CodeSearchDto search);
    Code selectCodeOne(@Param("langCd") String langCd,
                       @Param("groupCd") String groupCd,
                       @Param("code") String code);
    List<Code> selectCodeByGroup(@Param("langCd") String langCd, @Param("groupCd") String groupCd);
    int insertCode(Code c);
    int updateCode(Code c);
    int deleteCode(@Param("langCd") String langCd,
                   @Param("groupCd") String groupCd,
                   @Param("code") String code);

    // ── T_CODE_GROUP ──────────────────────────────────────────────────────────
    List<CodeGroup> selectGroupList();
    CodeGroup selectGroupOne(@Param("langCd") String langCd, @Param("groupCd") String groupCd);
    int insertGroup(CodeGroup g);
    int updateGroup(CodeGroup g);
    int deleteGroup(@Param("langCd") String langCd, @Param("groupCd") String groupCd);
    int countCodesByGroup(@Param("langCd") String langCd, @Param("groupCd") String groupCd);
}
