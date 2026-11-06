package kr.co.nh.atms.web.branch.mapper;

import kr.co.nh.atms.web.branch.dto.BranchSearchDto;
import kr.co.nh.atms.web.branch.entity.Branch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BranchMapper {
    List<Branch> selectList(BranchSearchDto search);
    long countList(BranchSearchDto search);
    Branch selectOne(@Param("termGroupId") String termGroupId,
                     @Param("branchCd") String branchCd,
                     @Param("branchName") String branchName);
    int insert(Branch branch);
    int update(Branch branch);
    int delete(@Param("termGroupId") String termGroupId,
               @Param("branchCd") String branchCd,
               @Param("branchName") String branchName);
}
