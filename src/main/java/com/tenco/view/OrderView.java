package com.tenco.view;

import com.tenco.Service.OrderService;
import com.tenco.dto.Order;
import com.tenco.dto.OrderItem;
import com.tenco.dto.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class OrderView {
    private Scanner scanner = new Scanner(System.in);
    private OrderService orderService = new OrderService();

    public void displayMenu() {
        while (true) {
            System.out.println("======= 주문 관리 시스템 ======");
            System.out.println("1. 주문 등록(결제)");
            System.out.println("2. 주문 조회");
            System.out.println("3. 주문 상세 목록 조회");
            System.out.println("4. 주문 상품 수량 변경");
            System.out.println("5. 주문 취소");
            System.out.println("0. 종료");
            System.out.println("=============================");
            System.out.println("선택 : ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    registerOrder();
                    break;
                case "2":
                    showAllOrders();
                    break;
                case "3":
                    showOrderDetail();
                    break;
                case "4":
                    updateOrderItemQuantity();
                    break;
                case "5":
                    cancelOrder(); // 추가
                    break;
                case "0":
                    System.out.println("주문 관리 시스템 종료");
                    return;
                default:
                    System.out.println("잘못된 입력");
            }
        }
    }

    // TODO- cancelOrder 작성
    private void cancelOrder() {
    }


    // 상품 주문 (주문 등록)
    private void registerOrder() {
        System.out.println("======= 주문 등록 =======");

        // 결제 수단 선택
        String paymentType;
        while (true) {
            System.out.print("결제 수단 선택 (1: CARD, 2: CASH) : ");
            String payInput = scanner.nextLine().trim();
            if ("1".equals(payInput)) {
                paymentType = "CARD";
                break;
            } else if ("2".equals(payInput)) {
                paymentType = "CASH";
                break;
            } else {
                System.out.println("올바른 결제 수단을 선택해 주세요 (1 또는 2).");
            }
        }

        List<OrderItem> items = new ArrayList<>();

        // 장바구니 담기
        while (true) {
            System.out.print("구매할 상품 ID 입력 (장바구니 담기 완료:0): ");
            int productId;
            try {
                productId = Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("상품 id를 입력해주세요");
                continue;
            }

            if (productId == 0) {
                break; // 장바구니 입력 종료 -> 결제 단계
            }

            // DB에서 상품 정보를 조회
            Product product = orderService.getProductById(productId);
            if (product == null) {
                System.out.println("존재하지 않는 상품 ID입니다. 다시 입력해 주세요.");
                continue;
            }

            if (product.getStock() <= 0) {
                System.out.println("해당 상품의 재고는 0입니다.");
                continue;
            }


            // 장바구니에 담긴 수량 계산
            final int currentProductId = productId;
            int cartQuantity = items.stream()
                    .filter(item -> item.getProductId() == currentProductId)
                    .mapToInt(OrderItem::getQuantity)
                    .sum();

            int availableStock = product.getStock() - cartQuantity;

            System.out.printf(" -> [선택 상품] %s | 단가: %,d원 | 남은 재고: %d개 (장바구니 담긴 수량: %d개)\n",
                    product.getProductName(), product.getPrice(), product.getStock(), cartQuantity);

            if (availableStock <= 0) {
                System.out.println("남은 재고를 모두 장바구니에 담았습니다.");
                continue;
            }

            // 수량 입력
            int quantity;
            boolean isCanceled = false;
            System.out.println("구매 수량 입력 : ");
            while (true) {
                try {
                    quantity = Integer.parseInt(scanner.nextLine().trim());

                    if (quantity == 0) {
                        System.out.println(" -> 상품 선택을 취소했습니다.");
                        isCanceled = true;
                        break; // 수량 입력 루프 탈출
                    }

                    if (quantity < 0) {
                        System.out.println("수량은 1개 이상이어야합니다.");
                        continue;
                    }

                    // 재고 체크
                    if (quantity > availableStock) {
                        System.out.printf("재고 부족 (추가 가능 최대 수량: %d개)\n", availableStock);
                        continue;
                    }

                    break;
                } catch (NumberFormatException e) {
                    System.out.println("숫자를 입력해주세요");
                }
            }
            if (isCanceled) {
                continue;
            }

            // 중복 상품 합산 처리
            Optional<OrderItem> existItem = items.stream()
                    .filter(item -> item.getProductId() == currentProductId)
                    .findFirst();

            if (existItem.isPresent()) {
                OrderItem item = existItem.get();
                item.setQuantity(item.getQuantity() + quantity);
                System.out.printf(" => [수량 추가] %s (총 %d개)\n", product.getProductName(), item.getQuantity());
            } else {
                OrderItem item = new OrderItem(productId, quantity, product.getPrice());
                items.add(item);
                System.out.printf(" => [장바구니 담기 완료] %s %d개\n", product.getProductName(), quantity);
            }
        }
        // 결제 처리 (장바구니 확인)
        if (items.isEmpty()) {
            System.out.println("주문할 상품이 선택되지 않았습니다.");
            return;
        }
        // 총 금액 계산
        int totalPrice = items.stream()
                .mapToInt(item -> item.getOrderPrice() * item.getQuantity())
                .sum();
        Order Order = new Order(paymentType, totalPrice);

        // 결제 및 재고 차감 처리 (트랜잭션)
        boolean isSuccess = orderService.processOrder(Order, items);

        if (isSuccess) {
            System.out.println("결제 성공  총금액 : " + totalPrice + "원");
        } else {
            System.out.println("결제 실패");
        }
    }

    // 전체 주문 조회 (주문 조회)
    private void showAllOrders() {
        System.out.println("====전체 주문 목록==========");
        List<Order> orderList = orderService.getAllOrders();

        if (orderList == null || orderList.isEmpty()) {
            System.out.println("등록된 주문 없습니다.");
            return;
        }

        System.out.println("주문번호 \t결제수단 \t총 금액 \t\t주문일시");
        System.out.println("-----------------------------------------------");
        for (Order order : orderList) {
            System.out.printf("%d \t\t %-6s \t %,d원 \t\t %s \n",
                    order.getOrderId(),
                    order.getPaymentType(),
                    order.getTotalPrice(),
                    order.getOrderDate() != null ? order.getOrderDate().toString().substring(0, 16) : "N/A");
        }
        System.out.println("----------------------------------------------------------");
    }

    // 주문 상세 목록 조회
    private void showOrderDetail() {
        System.out.println("주문 상세 조회");
        System.out.println("조회할 주문 ID : ");
        int orderId;
        try {
            orderId = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("숫자만 입력해 주세요");
            return;
        }

        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            System.out.println("존재하지 않는 주문 번호");
            return;
        }

        List<OrderItem> itemList = orderService.getOrderItemsByOrderId(orderId);

        System.out.println("=======주문 상세 정보======");
        System.out.println("주문 번호 : " + order.getOrderId());
        System.out.println("결제 수단 : " + order.getPaymentType());
        System.out.println("주문 일시 : " + order.getOrderDate());
        System.out.println("--------------------------------");
        System.out.println("상품ID\t상품명\t\t단가\t\t수량\t소계");
        System.out.println("------------------------------------");

        if (itemList != null && !itemList.isEmpty()) {
            for (OrderItem item : itemList) {
                Product product = orderService.getProductById(item.getProductId());
                String productName = (product != null) ? product.getProductName() : "알 수 없음";

                int subTotal = item.getOrderPrice() * item.getQuantity();
                System.out.printf("%d\t %-10s \t%,d원 \t %d개 \t%,d원 \n",
                        item.getProductId(),
                        productName,
                        item.getOrderPrice(),
                        item.getQuantity(),
                        subTotal);
            }
        } else {
            System.out.println("주문 상세 내역이 없습니다.");
        }

        System.out.println("--------------------------------");
        System.out.printf("총 결제 금액 : %,d원 \n", order.getTotalPrice());
        System.out.println("================================================");

    }

    // 이미 완료된 주문의 상품 수량 변경
    private void updateOrderItemQuantity() {
        System.out.println("===== 주문 상품 수량 변경 ====");
        System.out.println("수량을 변경할 주문 ID 입력 : ");
        int orderId;
        try {
            orderId = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("숫자를 입력해주세요");
            return;
        }

        Order order =orderService.getOrderById(orderId);
        if (order == null) {
            System.out.println("존재하지 않는 주문번호입니다.");
            return;
        }


    }
}
