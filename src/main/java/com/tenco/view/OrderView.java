package com.tenco.view;

import com.tenco.Service.OrderItemService;
import com.tenco.Service.OrderService;
import com.tenco.dto.Order;
import com.tenco.dto.OrderItem;
import com.tenco.dto.Product;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class OrderView {
    private final OrderService orderService = new OrderService();
    private final Scanner scanner = new Scanner(System.in);
    private final OrderItemService orderItemService = new OrderItemService();

    public void displayMenu() {
        while (true) {
            System.out.println("\n===== 주문 / 결제 =====");
            System.out.println("1. 주문 등록(결제)");
            System.out.println("2. 주문 조회");
            System.out.println("3. 주문 상세 목록 조회");
            System.out.println("4. 주문 상품 수량 변경");
            System.out.println("5. 주문 취소");
            System.out.println("0. 종료");

            int choice = readInt("선택 : ");

            try {
                switch (choice) {
                    case 1:
                        registerOrder();
                        break;
                    case 2:
                        showAllOrders();
                        break;
                    case 3:
                        showOrderDetail();
                        break;
                    case 4:
                        updateOrderItemQuantity();
                        break;
                    case 5:
                        cancelOrder();
                        break;
                    case 0:
                        System.out.println("주문 메뉴를 종료합니다.");
                        return;
                    default:
                        System.out.println("0~5 사이의 숫자를 입력해주세요.");
                }
            } catch (Exception e) {
                System.out.println("오류 : " + e.getMessage());
            }
        }
    }

    private void registerOrder() {
        System.out.println("\n===== 주문 등록 =====");
        System.out.println("1. CARD");
        System.out.println("2. CASH");

        int paymentChoice = readInt("결제 수단 선택 : ");
        String paymentType;

        if (paymentChoice == 1) {
            paymentType = "CARD";
        } else if (paymentChoice == 2) {
            paymentType = "CASH";
        } else {
            System.out.println("잘못된 결제 수단입니다.");
            return;
        }

        List<OrderItem> items = new ArrayList<>();

        while (true) {
            int productId = readInt(
                    "\n구매할 상품 ID를 입력하세요. " +
                            "(장바구니 담기 완료 : 0) : "
            );

            if (productId == 0) {
                break;
            }

            Product product = orderService.getProductById(productId);

            if (product == null) {
                System.out.println("존재하지 않는 상품 ID입니다.");
                continue;
            }

            if (product.getStock() <= 0) {
                System.out.println("해당 상품의 재고가 없습니다.");
                continue;
            }

            int cartQuantity = items.stream()
                    .filter(item -> item.getProductId() == productId)
                    .mapToInt(OrderItem::getQuantity)
                    .sum();

            int availableStock = product.getStock() - cartQuantity;

            if (availableStock <= 0) {
                System.out.println("해당 상품의 재고를 모두 장바구니에 담았습니다.");
                continue;
            }

            System.out.println("\n상품명 : " + product.getProductName());
            System.out.println("단가 : " + String.format("%,d", product.getPrice()) + "원");
            System.out.println("현재 재고 : " + product.getStock() + "개");
            System.out.println("장바구니 수량 : " + cartQuantity + "개");
            System.out.println("추가 가능 수량 : " + availableStock + "개");

            int quantity = readInt("구매 수량 : ");

            if (quantity == 0) {
                System.out.println("상품 선택을 취소했습니다.");
                continue;
            }

            if (quantity < 0) {
                System.out.println("수량은 0보다 커야 합니다.");
                continue;
            }

            if (quantity > availableStock) {
                System.out.println(
                        "재고가 부족합니다. 추가 가능 수량 : " +
                                availableStock + "개"
                );
                continue;
            }

            OrderItem existingItem = items.stream()
                    .filter(item -> item.getProductId() == productId)
                    .findFirst()
                    .orElse(null);

            if (existingItem != null) {
                existingItem.setQuantity(
                        existingItem.getQuantity() + quantity
                );

                System.out.println(
                        product.getProductName() +
                                " 수량이 추가되었습니다. 총 수량 : " +
                                existingItem.getQuantity() + "개"
                );
            } else {
                OrderItem item = new OrderItem(
                        productId,
                        quantity,
                        product.getPrice()
                );

                items.add(item);

                System.out.println(
                        product.getProductName() +
                                " " + quantity + "개가 장바구니에 추가되었습니다."
                );
            }
        }

        if (items.isEmpty()) {
            System.out.println("주문할 상품이 선택되지 않았습니다.");
            return;
        }

        int totalPrice = items.stream()
                .mapToInt(item ->
                        item.getOrderPrice() * item.getQuantity())
                .sum();

        System.out.println("\n===== 주문 확인 =====");

        for (OrderItem item : items) {
            Product product = orderService.getProductById(item.getProductId());

            String productName = product != null
                    ? product.getProductName()
                    : "알 수 없음";

            int subTotal =
                    item.getOrderPrice() * item.getQuantity();

            System.out.println(
                    productName +
                            " / " +
                            item.getQuantity() +
                            "개 / " +
                            String.format("%,d", subTotal) +
                            "원"
            );
        }

        System.out.println("-------------------------");
        System.out.println(
                "총 결제 금액 : " +
                        String.format("%,d", totalPrice) +
                        "원"
        );
        System.out.println("결제 수단 : " + paymentType);

        String answer = readLine("결제하시겠습니까? (Y/N) : ");

        if (!answer.equalsIgnoreCase("Y")) {
            System.out.println("결제가 취소되었습니다.");
            return;
        }

        Order order = new Order(paymentType, totalPrice);

        boolean success = orderService.processOrder(order, items);

        if (success) {
            System.out.println("\n결제가 완료되었습니다.");
            System.out.println(
                    "총 결제 금액 : " +
                            String.format("%,d", totalPrice) +
                            "원"
            );
        } else {
            System.out.println("결제에 실패했습니다.");
        }
    }

    private void showAllOrders() {
        System.out.println("\n===== 주문 목록 =====");

        List<Order> orderList = orderService.getAllOrders();

        if (orderList == null || orderList.isEmpty()) {
            System.out.println("등록된 주문이 없습니다.");
            return;
        }

        for (Order order : orderList) {
            String orderDate = order.getOrderDate() != null
                    ? order.getOrderDate().toString()
                    : "N/A";

            System.out.println(
                    "주문번호 : " + order.getOrderId() +
                            " / 결제수단 : " + order.getPaymentType() +
                            " / 총 금액 : " +
                            String.format("%,d", order.getTotalPrice()) +
                            "원 / 주문일시 : " + orderDate
            );
        }
    }

    private void showOrderDetail() {
        int orderId = readInt("조회할 주문 ID : ");

        Order order = orderService.getOrderById(orderId);

        if (order == null) {
            System.out.println("존재하지 않는 주문번호입니다.");
            return;
        }

        List<OrderItem> itemList =
                orderService.getOrderItemsByOrderId(orderId);

        System.out.println("\n===== 주문 상세 =====");
        System.out.println("주문번호 : " + order.getOrderId());
        System.out.println("결제수단 : " + order.getPaymentType());
        System.out.println("주문일시 : " + order.getOrderDate());

        System.out.println("\n[주문 상품]");

        if (itemList == null || itemList.isEmpty()) {
            System.out.println("주문 상품이 없습니다.");
            return;
        }

        for (OrderItem item : itemList) {
            Product product =
                    orderService.getProductById(item.getProductId());

            String productName = product != null
                    ? product.getProductName()
                    : "알 수 없음";

            int subTotal =
                    item.getOrderPrice() * item.getQuantity();

            System.out.println(
                    "주문상품 ID : " + item.getOrderItemId() +
                            " / 상품 ID : " + item.getProductId() +
                            " / 상품명 : " + productName +
                            " / 단가 : " +
                            String.format("%,d", item.getOrderPrice()) +
                            "원 / 수량 : " + item.getQuantity() +
                            "개 / 소계 : " +
                            String.format("%,d", subTotal) +
                            "원"
            );
        }

        System.out.println(
                "\n총 결제 금액 : " +
                        String.format("%,d", order.getTotalPrice()) +
                        "원"
        );
    }

    private void updateOrderItemQuantity() throws SQLException {
        int orderId = readInt("주문 ID : ");

        Order order = orderService.getOrderById(orderId);

        if (order == null) {
            System.out.println("존재하지 않는 주문입니다.");
            return;
        }

        List<OrderItem> itemList =
                orderService.getOrderItemsByOrderId(orderId);

        if (itemList == null || itemList.isEmpty()) {
            System.out.println("주문 상품이 없습니다.");
            return;
        }

        System.out.println("\n===== 주문 상품 =====");

        for (OrderItem item : itemList) {
            Product product =
                    orderService.getProductById(item.getProductId());

            String productName = product != null
                    ? product.getProductName()
                    : "알 수 없음";

            System.out.println(
                    "주문상품 ID : " + item.getOrderItemId() +
                            " / 상품 ID : " + item.getProductId() +
                            " / 상품명 : " + productName +
                            " / 현재 수량 : " + item.getQuantity()
            );
        }

        int orderItemId =
                readInt("수량을 변경할 주문상품 ID : ");

        OrderItem targetItem = itemList.stream()
                .filter(item -> item.getOrderItemId() == orderItemId)
                .findFirst()
                .orElse(null);

        if (targetItem == null) {
            System.out.println("해당 주문상품이 존재하지 않습니다.");
            return;
        }

        int quantity = readInt("변경할 수량 : ");

        if (quantity <= 0) {
            System.out.println("수량은 1개 이상이어야 합니다.");
            return;
        }

        System.out.println(
                "기존 수량 : " + targetItem.getQuantity() +
                        "개 → 변경 수량 : " + quantity + "개"
        );

        String answer =
                readLine("수량을 변경하시겠습니까? (Y/N) : ");

        if (!answer.equalsIgnoreCase("Y")) {
            System.out.println("수량 변경을 취소했습니다.");
            return;
        }

        int result = orderItemService.updateOrderItem(
                quantity,
                orderItemId
        );

        if (result > 0) {
            System.out.println("주문상품 수량이 변경되었습니다.");
        } else {
            System.out.println("주문상품 수량 변경에 실패했습니다.");
        }
    }

    private void cancelOrder() {
        int orderId = readInt("취소할 주문 ID : ");

        Order order = orderService.getOrderById(orderId);

        if (order == null) {
            System.out.println("존재하지 않는 주문번호입니다.");
            return;
        }

        List<OrderItem> itemList =
                orderService.getOrderItemsByOrderId(orderId);

        System.out.println("\n===== 주문 취소 =====");
        System.out.println("주문번호 : " + order.getOrderId());
        System.out.println("결제수단 : " + order.getPaymentType());
        System.out.println(
                "총 금액 : " +
                        String.format("%,d", order.getTotalPrice()) +
                        "원"
        );

        if (itemList != null && !itemList.isEmpty()) {
            System.out.println("\n[주문 상품]");

            for (OrderItem item : itemList) {
                Product product =
                        orderService.getProductById(item.getProductId());

                String productName = product != null
                        ? product.getProductName()
                        : "알 수 없음";

                System.out.println(
                        productName +
                                " / " +
                                item.getQuantity() +
                                "개"
                );
            }
        }

        String answer =
                readLine("\n정말 주문을 취소하시겠습니까? (Y/N) : ");

        if (!answer.equalsIgnoreCase("Y")) {
            System.out.println("주문 취소를 취소했습니다.");
            return;
        }

        boolean success = orderService.cancelOrder(orderId);

        if (success) {
            System.out.println("주문이 취소되었습니다.");
        } else {
            System.out.println("주문 취소에 실패했습니다.");
        }
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);

            try {
                return Integer.parseInt(
                        scanner.nextLine().trim()
                );
            } catch (NumberFormatException e) {
                System.out.println("숫자를 입력해주세요.");
            }
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
}
