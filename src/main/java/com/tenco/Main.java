package com.tenco;

import com.tenco.View.OrderView;
import com.tenco.util.util;

public class Main {
    public static void main(String[] args) {
        // 애플리케이션 종료 시 커넥션 풀 자원을 해제하기 위한 셧다운 훅(Shutdown Hook) 등록
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[시스템] DB 커넥션 풀 자원을 정리합니다...");
            util.close();
        }));

        System.out.println("편의점 주문 관리 시스템을 시작합니다.");

        // View 객체 생성 및 메뉴 실행
        OrderView orderView = new OrderView();
        orderView.displayMenu();

        System.out.println("프로그램이 정상적으로 종료되었습니다.");
    }
}