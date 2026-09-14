package com.tenco.View;

import com.tenco.Service.OrderService;
import com.tenco.dto.OrderDTO;
import com.tenco.dto.OrderItemDTO;
import com.tenco.util.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class OrderView {
    private Scanner scanner = new Scanner(System.in);
    private OrderService orderService = new OrderService();

    /**
     * [결제 메뉴 화면 처리 흐름]
     * 1. 결제 수단 입력 (CARD, CASH 등)
     * 2. 구매 상품 및 수량 입력 반복 (종료 조건: product_id = 0)
     * 3. 입력된 데이터를 기반으로 총 결제 금액 계산
     * 4. Service 레이어로 주문 요청 전송 (트랜잭션 처리)
     * 5. 처리 결과에 따른 사용자 응답 출력
     */
    private void handlerOrderView() {
        System.out.println("[상품 결제]");
        System.out.println("결제수단 (CARD/CASH)");
        String paymentType = scanner.nextLine();

        List<OrderItemDTO> items = new ArrayList<>();

        while (true) {
            System.out.println("상품 ID 입력 (종료:0)");
            int productId = Integer.parseInt(scanner.nextLine().trim());

            if (productId == 0) break;

            // Product 구현이 아직 안되어있음.
//             [STEP 1] DB에서 상품 정보를 조회하여 존재 여부 및 단가(price) 가져오기
//             ProductDTO product = storeService.getProductById(productId);
//
//            if (product == null) {
//                System.out.println("❌ 존재하지 않는 상품 ID입니다. 다시 입력해 주세요.");
//                continue;
//            }
//
//            System.out.println("   [선택 상품] " + product.getProductName() + " | 단가: " + product.getPrice() + "원");

            // Product 구현이 아직 안되어있음.
//            // [STEP 2] DB에서 가져온 product.getPrice()를 단가(orderPrice)로 자동 세팅
//            OrderItemDTO item = new OrderItemDTO(0, 0, productId, quantity, product.getPrice());
//            orderItemList.add(item);
//
//            System.out.println(">> [담기 완료] " + product.getProductName() + " " + quantity + "개 추가됨");
        }

        if (items.isEmpty()) {
            System.out.println("❌ 주문할 상품이 선택되지 않아 결제를 취소합니다.");
            return;
        }

        // [STEP 3] 담긴 상품들의 (단가 * 수량)을 합산하여 총 결제 금액 자동 계산
        int totalPrice = items.stream()
                .mapToInt(item -> item.getOrderPrice() * item.getQuantity())
                .sum();

        OrderDTO orderDTO = new OrderDTO(0, paymentType, totalPrice, null);

        // [STEP 4] 결제 및 재고 차감 처리 진행
        boolean isSuccess = orderService.processOrder(orderDTO, items);

        if (isSuccess) {
            System.out.println("결제가 성공적으로 안료되었습니다. 총 금액 : " + totalPrice + "원 입니다.");
        } else {
            System.out.println("결제 처리가 실패하였습니다.");
        }
    }

    // View에서 입력받은 productId로 DB에서 상품 정보(단가 포함)를 읽어오는 메서드
    // 아직 Product 구현 안해서 사용할 수 없음.
//    public ProductDTO getProductById(int productId) {
//        try (Connection conn = util.getConnection()) {
//            return productDAO.selectProductById(conn, productId);
//        } catch (SQLException e) {
//            System.err.println("상품 조회 실패: " + e.getMessage());
//            return null;
//        }
//    }
}
