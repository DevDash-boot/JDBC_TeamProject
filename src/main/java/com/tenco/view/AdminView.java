package com.tenco.view;

import com.tenco.Service.AuthService;
import com.tenco.dto.Admin;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class AdminView {

    private final AuthService service = new AuthService();
    private final Scanner scanner = new Scanner(System.in);

    private Integer currentAdminId = null;
    private String currentAdminName = null;


    public boolean AdminStart() {
        while (true) {
            printMenu();
            int choice = readInt("선택: ");

            switch (choice) {
                case 1:
                    loginMenu();
                    if (isAdminLoggedIn()) {
                        return true;
                    }
                    break;
                case 2:
                    if (requireAdmin("신규 관리자 등록")) {
                        registerMenu();
                    }
                    break;
                case 3:
                    if (requireAdmin("관리자 전체 목록 조회")) {
                        showAllAdmins();
                    }
                    break;
                case 4:
                    logout();
                    break;
                case 0:
                    System.out.println("메인 메뉴로 돌아갑니다");
                    return false;
                default:
                    System.out.println("잘못된 입력입니다. 다시 입력해주세요.");
            }
        }
    }

    // 메뉴 화면
    private void printMenu() {
        System.out.println("\n=== 관리자 관리 시스템 ===");
        if (isAdminLoggedIn()) {
            System.out.println("로그인: " + currentAdminName + "관리자");
        } else {
            System.out.println("로그아웃 상태");
        }
        System.out.println("---------------------");
        System.out.println(" 1. 로그인");
        System.out.println(" 2. 관리자 등록");
        System.out.println(" 3. 전체 목록 조회");
        System.out.println(" 4. 로그아웃");
        System.out.println(" 0. 종료");
        System.out.println("---------------------");
    }

    // 관리자 권한 확인하기
    private boolean requireAdmin(String menuName) {
        if (!isAdminLoggedIn()) {
            System.out.println("관리자만 " + menuName + " 기능을 이용할 수 있습니다");
            return false;
        }
        return true;
    }

    // 1. 관리자 로그인
    private Admin loginMenu() {
        if (isAdminLoggedIn()) {
            System.out.println("현재 로그인 상태입니다");
            return null;
        }
        System.out.println("\n 관리자 로그인");
        System.out.print("ID: ");
        String loginId = scanner.nextLine().trim();
        System.out.print("비밀번호: ");
        String password = scanner.nextLine();

        try {
            Admin admin = service.authenticationAdmin(loginId, password);
            if (admin == null) {
                System.out.println("로그인 실패! ID 또는 비밀번호를 잘못 입력했습니다");
                return null;
            } else {
                currentAdminId = admin.getAdminId();
                currentAdminName = admin.getName();
                System.out.println(currentAdminName + " 관리자님이 로그인 했습니다!");
                return admin;
            }
        } catch (SQLException e) {
            System.out.println("오류! " + e.getMessage());
        }
        return null;
    }

    // 2. 신규 관리자 등록
    private void registerMenu() {
        System.out.println("\n=== 신규 관리자 등록 ===");
        System.out.print("ID: ");
        String loginId = scanner.nextLine().trim();
        System.out.print("비밀번호: ");
        String password = scanner.nextLine();
        System.out.print("이름: ");
        String name = scanner.nextLine().trim();

        Admin admin = Admin.builder()
                .loginId(loginId)
                .password(password)
                .name(name)
                .build();

        try {
            service.registrationAdmin(admin);
            System.out.println("신규 관리자가 성공적으로 등록되었습니다!");
        } catch (SQLException e) {
            System.out.println("등록실패! " + e.getMessage());
        }
    }

    // 3. 관리자 전체 목록 조회
    private void showAllAdmins() {
        List<Admin> adminList = service.getAllAdmin();
        System.out.println("=== 관리자 목록 ===");
        if (adminList == null || adminList.isEmpty()) {
            System.out.println("등록된 관리자가 없습니다");
            return;
        }
        System.out.println("-------------------------------");
        System.out.printf("%-8s | %-15s | %-10s\n", "관리자ID", "로그인 아이디", "이름");
        System.out.println("-------------------------------");
        for (Admin admin : adminList) {
            System.out.printf("%-8d | %-15s | %-10s\n",
                    admin.getAdminId(),
                    admin.getLoginId(),
                    admin.getName());
        }
        System.out.println();
    }

    // 4. 로그아웃
    private void logout() {
        if (!isAdminLoggedIn()) {
            System.out.println("현재 로그인 상태가 아닙니다");
            return;
        }
        System.out.println(currentAdminName + " 관리자님이 로그아웃되었습니다");
        currentAdminId = null;
        currentAdminName = null;
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("숫자를 입력해주세요.");
            }
        }
    }

    private boolean isAdminLoggedIn() {
        return currentAdminId != null;
    }

}