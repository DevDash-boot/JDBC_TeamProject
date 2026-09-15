package com.tenco;


import com.tenco.view.*;

import java.util.Scanner;

public class Main2 {
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("===== 무인편의점 재고 관리 시스템 =====");
        boolean isAdmin = true;

        while(true){
            printMenu();
            int choice = readInt("선택 : ");

            try {
                switch (choice){
                    case 1:
                        if (!isAdmin){
                            System.out.println("관리자 로그인 필요");
                        }
                        if (isAdmin) {
                            System.out.println("상품관리 메뉴로 이동합니다.\n");
                            ProductView productView = new ProductView();
                            productView.start();
                        }
                        break;
                    case 2:
                        System.out.println("주문/결제 메뉴로 이동합니다.\n");
                        OrderView orderView = new OrderView();
                        orderView.displayMenu();
                        break;
                    case 3:
                        System.out.println("주문한 상품 메뉴로 이동합니다.\n");
                        OrderItemView orderItemView = new OrderItemView();
                        orderItemView.start();
                        break;
                    case 4:
                        if (!isAdmin){
                            System.out.println("관리자 로그인 필요");
                        }
                        if (isAdmin) {
                            System.out.println("발주/입고 메뉴로 이동합니다.\n");
                            new PurchaseView().purchaseView();
                        }
                        break;
                    case 5:
                        System.out.println("관리자 메뉴로 이동합니다.\n");
                        AdminView adminView = new AdminView();
                        adminView.AdminStart();
                        break;
                    case 6:
                        System.out.println("매장 정보로 이동합니다.\n");
                        StoreInfoView storeInfoView = new StoreInfoView();
                        storeInfoView.start();
                        break;
                    case 0:
                        System.out.println("프로그램을 종료합니다.");
                        scanner.close();
                        return;
                    default:
                        System.out.println("0~6 사이의 숫자를 입력하세요.");
                }
            } catch (Exception e) {
                System.out.println("오류: " + e.getMessage());
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n[메뉴]");
        System.out.println("1.  상품관리(관리자)");
        System.out.println("2.  주문 / 결제");
        System.out.println("3.  주문한 상품");
        System.out.println("4.  발주 / 입고(관리자)");
        System.out.println("5.  관리자 정보(로그인)");
        System.out.println("6.  매장 정보");
        System.out.println("0. 종료");
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.print("숫자를 입력해주세요 : ");
            }
        }
    }
}