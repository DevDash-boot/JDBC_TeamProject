package com.tenco.Service;

import com.tenco.dao.AdminDAO;
import com.tenco.dto.Admin;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.SQLException;
import java.util.List;

public class AuthService {

    private final AdminDAO adminDAO = new AdminDAO();


    // 1. 관리자 로그인 인증
    public Admin authenticationAdmin(String loginId, String password) throws SQLException {
        if (loginId == null || loginId.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            throw new SQLException("관리자 id와 비밀번호를 입력하세요");
        }

        Admin admin = adminDAO.findByLogin(loginId);
        // 해시 처리 추가 수정
        if (admin == null || !BCrypt.checkpw(password,admin.getPassword())) {
            return null;
        }
        admin.setPassword(null);
        return admin;
    }

    // 2. 신규 관리자 등록
    public void registrationAdmin(Admin admin) throws SQLException {
        if (admin.getLoginId() == null || admin.getLoginId().trim().isEmpty() ||
                admin.getPassword() == null || admin.getPassword().trim().isEmpty() ||
                admin.getName() == null || admin.getName().trim().isEmpty()) {
            throw new SQLException("항목을 입력해주세요");
        }

        Admin newAdmin = adminDAO.findByLogin(admin.getLoginId());
        if (newAdmin != null) {
            throw new SQLException("이미 등록되있는 아이디 입니다.");
        }

        // 비밀번호 암호화 하기
        String hashPassword = BCrypt.hashpw(admin.getPassword(), BCrypt.gensalt(10));
        admin.setPassword(hashPassword);

        int result = adminDAO.insertAdmin(admin);
        if (result == 0) {
            throw new SQLException("관리자 등록에 실패했습니다.");
        }
    }

    // 3. 관리자 전체 조회
    public List<Admin> getAllAdmin() {
        List<Admin> adminList = adminDAO.findAll();
        for (Admin admin : adminList) {
            admin.setPassword(null);
        }
        return adminList;
    }


    // 솔트 :  해시 처리 하기
}

