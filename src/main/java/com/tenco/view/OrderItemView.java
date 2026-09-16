package com.tenco.view;

import com.tenco.Service.*;
import com.tenco.dto.OrderItem;
import com.tenco.util.util;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class OrderItemView {
    private final OrderItemService orderItemService = new OrderItemService();

    private final Scanner scanner = new Scanner(System.in);

    // 처리순서
    // 1. 메뉴 출력
    // 2. 번호 입력
    // 3. 번호에 맞는 메서드 호출
    // 4. 호출 중 SQLException 이 나면 에러 메세지를 출력하고 다시 1번으로 돌아간다.
    // 5. 0번을 입력하면 프로그램 종료 또는 return 루프 탈출
    public void start(){
        System.out.println("===== 무인편의점 상품 관리 시스템 =====");
        while(true){
            printMenu();
            int choice = readInt("선택: ");

            try {
                switch (choice){
                    // TODO -주문등록 삭제
//                    case 1: addOrderItem(); break;
                    case 1: selectOrderItem(); break;
                    case 2: allOrderItem(); break;
                    case 3: updateOrderItem(); break;
                    case 4: deleteOrderItem(); break;
                    case 5: sumOrderItem(); break;
                    case 0:
                        System.out.println("프로그램을 종료합니다.");
                        util.close();
                        scanner.close();
                        return;
                    default:
                        System.out.println("0~5 사이의 숫자를 입력하세요.");
                }
            } catch (Exception e) {
                System.out.println("오류: " + e.getMessage());
            }
        }
    }
    // TODO - 주문등록 필요없어서 삭제
//    private void addOrderItem() throws SQLException {
//        System.out.print("주문 번호 : ");
//        int orderId = scanner.nextInt();
//        System.out.print("상품 번호 : ");
//        int productId = scanner.nextInt();
//        System.out.print("수량 : ");
//        int quantity = scanner.nextInt();
//        System.out.print("가격 : ");
//        int orderPrice = scanner.nextInt();
//
//        OrderItem orderItem = OrderItem.builder()
//                .orderId(orderId)
//                .productId(productId)
//                .quantity(quantity)
//                .orderPrice(orderPrice)
//                .build();
//        orderItemService.addOrderItem(orderItem);
//    }

    private void selectOrderItem() throws SQLException {
        System.out.print("특정 주문 검색 : ");
        int orderId = scanner.nextInt();
        List<OrderItem> orderItemList = orderItemService.selectOrderItem(orderId);
        System.out.println("ID|주문자ID|상품ID|상품명|수량|가격");
        System.out.println("───────────────────────────────────");
        for(OrderItem o : orderItemList){
            System.out.printf("%d  |  %d  |  %d  |  %s  |  %d  |  %d\n",
                    o.getOrderItemId(), o.getOrderId(), o.getProductId(),
                    o.getProductName(), o.getQuantity(), o.getOrderPrice());
        }
    }

    private void allOrderItem(){
        System.out.println("[전체 보기]");
        List<OrderItem> orderItemList = orderItemService.allOrderItem();
        System.out.println("ID|주문자ID|상품ID|상품명|수량|가격");
        System.out.println("───────────────────────────────────");
        for(OrderItem o : orderItemList){
            System.out.printf("%d  |  %d  |  %d  |  %s  |  %d  |  %d\n",
                    o.getOrderItemId(), o.getOrderId(), o.getProductId(),
                    o.getProductName(), o.getQuantity(), o.getOrderPrice());
        }
    }

    private void updateOrderItem() throws SQLException {
        System.out.print("수정할 주문ID 선택 : ");
        int orderItemId = scanner.nextInt();
        System.out.print("상품 수량 변경 : ");
        int quantity = scanner.nextInt();
        int rows = orderItemService.updateOrderItem(quantity, orderItemId);
        if (rows > 0){
            System.out.println("상품 수량이 변경되었습니다.");
        } else{
            System.out.println("수정할 상품이 없습니다.");
        }
    }

    private void deleteOrderItem() throws SQLException {
        System.out.print("주문 삭제 : ");
        int orderItemId = scanner.nextInt();
        int rows = orderItemService.deleteOrderItem(orderItemId);
        if (rows > 0){
            System.out.println("상품 수량이 삭제되었습니다.");
        } else{
            System.out.println("삭제할 상품이 없습니다.");
        }
    }

    private void sumOrderItem() throws SQLException {
        System.out.print("주문자ID : ");
        int orderId = scanner.nextInt();
        int totalPrice = orderItemService.sumOrderItem(orderId);
        System.out.println("주문자 ID " + orderId +  "의 총 금액은 " + totalPrice + "원 입니다.");
    }

    private void printMenu() {
        System.out.println("\n[메뉴]");
        // TODO - 주문등록 삭제
        //        System.out.println("1. 주문 등록");
        System.out.println("1. 특정 상품 조회");
        System.out.println("2. 주문 전체 조회");
        System.out.println("3. 주문한 상품 수량 수정");
        System.out.println("4. 주문한 상품 삭제");
        System.out.println("5. 주문별 총 금액 계산");
        System.out.println("0. 종료");
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
}
