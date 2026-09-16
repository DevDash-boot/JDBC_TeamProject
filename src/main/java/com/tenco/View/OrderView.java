package com.tenco.View;

import com.tenco.Service.OrderService;
import com.tenco.dto.OrderDTO;
import com.tenco.dto.OrderItemDTO;
import com.tenco.dto.ProductDTO;

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

        List<OrderItemDTO> items = new ArrayList<>();

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
            ProductDTO product = orderService.getProductById(productId);
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
                    .mapToInt(OrderItemDTO::getQuantity)
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
            System.out.println("구매 수량 입력 : ");
            while (true) {
                try {
                    quantity = Integer.parseInt(scanner.nextLine().trim());

                    if (quantity == 0) {
                        System.out.println(" -> 상품 선택을 취소했습니다.");
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

            // 중복 상품 합산 처리
            Optional<OrderItemDTO> existItem = items.stream()
                    .filter(item -> item.getProductId() == currentProductId)
                    .findFirst();

            if (existItem.isPresent()) {
                OrderItemDTO item = existItem.get();
                item.setQuantity(item.getQuantity() + quantity);
                System.out.printf(" => [수량 추가] %s (총 %d개)\n", product.getProductName(), item.getQuantity());
            } else {
                OrderItemDTO item = new OrderItemDTO(productId, quantity, product.getPrice());
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
        OrderDTO orderDTO = new OrderDTO(paymentType, totalPrice);

        // 결제 및 재고 차감 처리 (트랜잭션)
        boolean isSuccess = orderService.processOrder(orderDTO, items);

        if (isSuccess) {
            System.out.println("결제 성공  총금액 : " + totalPrice + "원");
        } else {
            System.out.println("결제 실패");
        }
    }

    // 전체 주문 조회 (주문 조회)
    private void showAllOrders() {
        System.out.println("====전체 주문 목록==========");
        List<OrderDTO> orderList = orderService.getAllOrders();

        if (orderList == null || orderList.isEmpty()) {
            System.out.println("등록된 주문 없습니다.");
            return;
        }

        System.out.println("주문번호 \t결제수단 \t총 금액 \t 주문 상태 \t\t주문일시");
        System.out.println("-----------------------------------------------");
        for (OrderDTO order : orderList) {
            System.out.printf("%d \t\t %-6s \t %,d원 \t %s \t\t %s \n",
                    order.getOrderId(),
                    order.getPaymentType(),
                    order.getTotalPrice(),
                    order.getStatus() != null ? order.getStatus() : "??",
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

        OrderDTO order = orderService.getOrderById(orderId);
        if (order == null) {
            System.out.println("존재하지 않는 주문 번호");
            return;
        }

        boolean isCancelled = "CANCELLED".equalsIgnoreCase(order.getStatus());

        List<OrderItemDTO> itemList = orderService.getOrderItemsByOrderId(orderId);

        System.out.println("=======주문 상세 정보======");
        System.out.println("주문 번호 : " + order.getOrderId());
        System.out.println("결제 수단 : " + order.getPaymentType());
        System.out.println("주문 상태 : " + (isCancelled ? "취소됨" : order.getStatus()));
        System.out.println("주문 일시 : " + order.getOrderDate());

        // 취소된 주문일 경우 안내 문구 표시
        if (isCancelled) {
            System.out.println("===========================");
            System.out.println("해당 주문은 취소된 주문의 상세 내역입니다.");
        }
        System.out.println("--------------------------------");
        System.out.println("상품ID\t상품명\t\t단가\t\t수량\t소계");
        System.out.println("------------------------------------");

        if (itemList != null && !itemList.isEmpty()) {
            for (OrderItemDTO item : itemList) {
                ProductDTO product = orderService.getProductById(item.getProductId());
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

        OrderDTO order = orderService.getOrderById(orderId);
        if (order == null) {
            System.out.println("존재하지 않는 주문번호입니다.");
            return;
        }

        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            System.out.println("취소된 주문의 상품 수량은 변경할 수 없습니다.");
            return;
        }

        List<OrderItemDTO> itemList = orderService.getOrderItemsByOrderId(orderId);
        if (itemList == null || itemList.isEmpty()) {
            System.out.println("해당 주문에 상품 내역이 없습니다.");
            return;
        }

        // 주문 상세 항목 출력
        System.out.println("[주문번호 : " + orderId + "] 상품 목록");
        for (OrderItemDTO item : itemList) {
            ProductDTO product = orderService.getProductById(item.getProductId());
            String productName = (product != null) ? product.getProductName() : "알수 없음";
            System.out.printf("상품 ID: %d | 상품명 : %s | 현재 수량 : %d개 | 단가 : %,d원\n",
                    item.getProductId(), productName, item.getQuantity(), item.getOrderPrice());
        }

        // 변경할 상품 입력
        System.out.println("수량을 변경할 상품 ID 입력 (취소 : 0) :");
        int productId;
        try {
            productId = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("숫자만 입력해주세요");
            return;
        }

        if (productId == 0) {
            System.out.println("수량 변경을 취소하겠습니다.");
            return;
        }

        // 대상 항목 찾기
        Optional<OrderItemDTO> targetItemOpt = itemList.stream()
                .filter(item -> item.getProductId() == productId)
                .findFirst();

        if (targetItemOpt.isEmpty()) {
            System.out.println("해당 주문에 입력하신 상품이 없습니다");
            return;
        }

        OrderItemDTO targetItem = targetItemOpt.get();
        ProductDTO product = orderService.getProductById(productId);

        if (product == null) {
            System.out.println("해당 상품의 정보를 찾을 수 없습니다.");
            return;
        }

        // 변경 가능 재고 계산
        // (현재 재고 + 해당 주문에 이미 묶여있던 수량)
        int maxAvailableStock = product.getStock() + targetItem.getQuantity();

        System.out.printf(" \n[선택 상품] %s (현재 수량 : %d개 / 최대 변경 가능 수량 %d개)\n",
                product.getProductName(), targetItem.getQuantity(), maxAvailableStock);

        int newQuantity;
        while (true) {
            System.out.println("변경하실 수량 입력 (취소 : 0) :");
            try {
                newQuantity = Integer.parseInt(scanner.nextLine().trim());
                if (newQuantity == 0) {
                    System.out.println("변경을 취소하겠습니다.");
                    return;
                }
                if (newQuantity < 0) {
                    System.out.println("수량은 1개 이상이어야합니다.");
                    return;
                }
                if (newQuantity > maxAvailableStock) {
                    System.out.printf("재고가 부족합니다 .(최대 가능 수량 : %d개)\n", maxAvailableStock);
                    continue;
                }
                break;
            } catch (NumberFormatException e) {
                System.out.println("숫자만 입력해주세요.");
            }
        }
        // 기존 주문의 수량과 변경할 수량이 똑같은 경우
        if (newQuantity == targetItem.getQuantity()) {
            System.out.println("기존 수량과 일치합니다.");
            return;
        }

        // 트랜잭션(주문 수량에 맞춰서 상품의 재고 업데이트해야됨)
        boolean isSuccess = orderService.updateOrderItemQuantity(orderId, productId, targetItem.getQuantity(), newQuantity, targetItem.getOrderPrice());

        if (isSuccess) {
            System.out.println("주문 상품 수량 변경 선공");
        } else {
            System.out.println("수량 변경 처리 중 오류 발생");
        }

    }

    // 주문한 상품 삭제
    private void cancelOrder() {
        System.out.println("==== 주문 취소 ====");
        System.out.println("취소할 주문 ID 입력");
        int orderId;
        try {
            orderId = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("숫자만 입력해주세요");
            return;
        }

        // 주문 확인
        OrderDTO order = orderService.getOrderById(orderId);
        if (order == null) {
            System.out.println("검색하신 주문 번호가 없습니다.");
            return;
        }

        // 이미 취소된 주문인지 확인
        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) {
            System.out.println("이미 취소된 주문입니다.");
            return;
        }

        // 주문 상세 항목(상품 & 수량) 조회
        List<OrderItemDTO> itemList = orderService.getOrderItemsByOrderId(orderId);
        if (itemList == null || itemList.isEmpty()) {
            System.out.println("취소할 주문내역이 없습니다.");
            return;
        }

        // 취소할 주문 정보 표시
        System.out.println("== 취소할 주문 정보 ==");
        System.out.printf(" 주문 번호 : %d\n", order.getOrderId());
        System.out.printf(" 결제 수단 : %s\n", order.getPaymentType());
        System.out.printf(" 총 결제 금액 : %,d\n", order.getTotalPrice());
        System.out.println(" 주문 내역 : ");
        for (OrderItemDTO item : itemList) {
            ProductDTO product = orderService.getProductById(item.getProductId());
            String productName = (product != null) ? product.getProductName() : "알수없음";
            System.out.printf(" * %s (%d개)\n", productName, item.getQuantity());
        }

        // 주문 취소 확인
        System.out.println("주문 취소하시겠습니까? (Y/N)");
        String confirm = scanner.nextLine().trim();

        if (!"Y".equalsIgnoreCase(confirm)) {
            System.out.println("주문 취소 중단하셨습니다.");
            return;
        }

        // 트랜잭션(주문 취소에 맞춰서 재고 복구)
        boolean isSuccess = orderService.cancelOrder(orderId, itemList);

        if (isSuccess) {
            System.out.println("주문 취소 성공");
        } else {
            System.out.println("주문 취소 처리 중 오류 발생");
        }

    }


}
