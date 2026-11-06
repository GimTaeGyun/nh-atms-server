package kr.co.nh.atms.web.user.service;

import kr.co.nh.atms.common.exception.BusinessException;
import kr.co.nh.atms.common.page.PageResult;
import kr.co.nh.atms.web.user.dto.UserSearchDto;
import kr.co.nh.atms.web.user.entity.User;
import kr.co.nh.atms.web.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<User> search(UserSearchDto s) {
        return PageResult.of(mapper.selectList(s), mapper.countList(s), s);
    }
    public User get(String userId) {
        User u = mapper.selectOne(userId);
        if (u == null) throw new BusinessException("USR_001", "사용자를 찾을 수 없습니다.");
        u.setPassword(null); // 응답에 비밀번호 미포함
        return u;
    }
    @Transactional
    public void create(User u) {
        if (mapper.selectOne(u.getUserId()) != null)
            throw new BusinessException("USR_002", "이미 존재하는 사용자 ID 입니다.");
        u.setPassword(passwordEncoder.encode(u.getPassword()));
        if (u.getUseYn() == null) u.setUseYn("Y");
        mapper.insert(u);
    }
    @Transactional
    public void update(User u) {
        if (mapper.update(u) == 0) throw new BusinessException("USR_001", "사용자를 찾을 수 없습니다.");
    }
    @Transactional
    public void delete(String userId) {
        if (mapper.delete(userId) == 0) throw new BusinessException("USR_001", "사용자를 찾을 수 없습니다.");
    }
    @Transactional
    public void changePassword(String userId, String newPassword) {
        if (mapper.updatePassword(userId, passwordEncoder.encode(newPassword)) == 0)
            throw new BusinessException("USR_001", "사용자를 찾을 수 없습니다.");
    }
    @Transactional
    public void unlock(String userId) {
        if (mapper.unlock(userId) == 0)
            throw new BusinessException("USR_001", "사용자를 찾을 수 없습니다.");
    }
    @Transactional
    public void updateUseYn(String userId, String useYn) {
        if (mapper.updateUseYn(userId, useYn) == 0)
            throw new BusinessException("USR_001", "사용자를 찾을 수 없습니다.");
    }
}
