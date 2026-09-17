package com.tenco.dao;

import com.tenco.dto.Admin;
import com.tenco.util.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AdminDAO {
    // 관리자 테이블 로그인 조회
    public Admin findByLogin(String loginId) {

        String sql = """
                SELECT * 
                FROM admin
                WHERE login_id = ?
                """;
        try (Connection conn = util.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, loginId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Admin.builder()
                            .adminId(rs.getInt("admin_id"))
                            .loginId(rs.getString("login_id"))
                            .password(rs.getString("password"))
                            .name(rs.getString("name"))
                            .build();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);

        }
        return null;
    }

    // 2. 신규 관리자 등록
    public int insertAdmin(Admin admin) {
        String sql = """
                INSERT INTO admin (login_id, password, name)
                VALUES (?, ?, ?)
                """;
        try (Connection conn = util.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, admin.getLoginId());
            pstmt.setString(2, admin.getPassword());
            pstmt.setString(3, admin.getName());

            return pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }

    // 3. 관리자 전체 조회
    public List<Admin> findAll() {
        List<Admin> adminList = new ArrayList<>();

        String sql = """
                SELECT * FROM admin order by admin_id
                """;

        try (Connection conn = util.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Admin admin = Admin.builder()
                        .adminId(rs.getInt("admin_id"))
                        .loginId(rs.getString("login_id"))
                        .password(rs.getString("password"))
                        .name(rs.getString("name"))
                        .build();

                adminList.add(admin);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);

        }
        return adminList;
    }
}