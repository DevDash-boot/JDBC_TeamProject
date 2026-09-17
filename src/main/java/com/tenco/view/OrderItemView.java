package com.tenco.view;

import com.tenco.Service.OrderItemService;
import com.tenco.dto.OrderItem;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

public class OrderItemView {
    private final Scanner scanner = new Scanner(System.in);
    private final OrderItemService orderItemService = new OrderItemService();

    public void start() {
        while (true) {
            System.out.println("\n===== 주문한 상품 관리 =====");
            System.out.println("1. 특정 주문 상품 조회");
            System.out.println("2. 전체 주문 상품 조회");
            System.out.println("3. 주문 상품 수량 수정");
            System.out.println("4. 주문 상품 삭제");
            System.out.println("5. 주문 금액 조회");
            System.out.println("0. 이전 메뉴");

            int choice = readInt("선택: ");

            try {
                switch (choice) {
                    case 1:
                        selectOrderItem();
                        break;
                    case 2:
                        allOrderItem();
                        break;
                    case 3:
                        updateOrderItem();
                        break;
                    case 4:
                        deleteOrderItem();
                        break;
                    case 5:
                        sumOrderItem();
                        break;
                    case 0:
                        return;
                    default:
                        System.out.println("0~5 사이의 숫자를 입력해주세요.");
                }
            } catch (SQLException e) {
                System.out.println("오류 : " + e.getMessage());
            } catch (Exception e) {
                System.out.println("오류 : " + e.getMessage());
            }
        }
    }

    // 특정 주문 상품 조회
    private void selectOrderItem() throws SQLException {
        int orderId = readInt("조회할 주문 번호 : ");
        List<OrderItem> list = orderItemService.selectOrderItem(orderId);
        if (list.isEmpty()) {
            System.out.println("해당 주문에 상품이 없습니다.");
            return;
        }
        System.out.println("\n===== 주문 상품 =====");
        for (OrderItem item : list) {
            printOrderItem(item);
        }
    }

    // 전체 주문 상품 조회
    private void allOrderItem() {
        List<OrderItem> list = orderItemService.allOrderItem();
        if (list.isEmpty()) {
            System.out.println("주문한 상품이 없습니다.");
            return;
        }
        System.out.println("\n===== 전체 주문 상품 =====");
        for (OrderItem item : list) {
            printOrderItem(item);
        }
    }

    // 주문 상품 수량 수정
    private void updateOrderItem() throws SQLException {
        int orderItemId = readInt("수정할 주문상품 번호 : ");
        int quantity = readInt("변경할 수량 : ");
        int result = orderItemService.updateOrderItem(quantity, orderItemId);
        if (result > 0) {
            System.out.println("주문 상품 수량이 수정되었습니다.");
        } else {
            System.out.println("주문 상품을 찾을 수 없습니다.");
        }
    }

    // 주문 상품 삭제
    private void deleteOrderItem() throws SQLException {
        int orderItemId = readInt("삭제할 주문상품 번호 : ");
        int result = orderItemService.deleteOrderItem(orderItemId);
        if (result > 0) {
            System.out.println("주문 상품이 삭제되었습니다.");
        } else {
            System.out.println("주문 상품을 찾을 수 없습니다.");
        }
    }

    // 주문 금액 조회
    private void sumOrderItem() throws SQLException {
        int orderId = readInt("조회할 주문 번호 : ");
        int totalPrice = orderItemService.sumOrderItem(orderId);
        System.out.println("주문 총 금액 : " + totalPrice + "원");
    }

    private void printOrderItem(OrderItem item) {
        System.out.println(
                "주문상품번호 : " + item.getOrderItemId()
                        + " | 주문번호 : " + item.getOrderId()
                        + " | 상품번호 : " + item.getProductId()
                        + " | 상품명 : " + item.getProductName()
                        + " | 수량 : " + item.getQuantity()
                        + " | 상품가격 : " + item.getOrderPrice() + "원"
        );
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
