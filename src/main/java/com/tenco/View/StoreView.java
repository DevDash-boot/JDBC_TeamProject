
// 나중에 활용할 코드
//package com.tenco.View;
//
//import com.tenco.Service.OrderService;
//import com.tenco.dto.OrderItemDTO;
//import com.tenco.dto.OrderDTO;
//import com.tenco.dto.ProductDTO;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Scanner;
//
//public class StoreView {
//    private final Scanner scanner = new Scanner(System.in);
//    private final OrderService orderService = new OrderService();
//
//    public void handleOrderView() {
//        System.out.println("\n========================================");
//        System.out.println("            [ 상품 결제 진행 ]          ");
//        System.out.println("========================================");
//
//        System.out.print("▶ 결제 수단 선택 (CARD / CASH) : ");
//        String paymentType = scanner.nextLine().trim().toUpperCase();
//
//        List<OrderItemDTO> orderItemList = new ArrayList<>();
//        int totalPrice = 0;
//
//        while (true) {
//            System.out.println("\n----------------------------------------");
//            System.out.print("▶ 상품 ID 입력 (결제 진행은 0 입력) : ");
//            int productId = Integer.parseInt(scanner.nextLine().trim());
//
//            // 0 입력 시 상품 장바구니 담기 종료
//            if (productId == 0) break;
//
//            // =========================================================
//            // [getProductById 활용 1] DB에서 상품 조회 및 존재 여부 확인
//            // =========================================================
//            ProductDTO product = orderService.getProductById(productId);
//
//            if (product == null) {
//                System.out.println("❌ 입력하신 상품 ID(" + productId + ")는 존재하지 않는 상품입니다.");
//                continue;
//            }
//
//            // 조회된 상품 정보 출력
//            System.out.println("   [선택 상품] " + product.getProductName()
//                    + " | 단가: " + product.getPrice() + "원"
//                    + " | 남은 재고: " + product.getStock() + "개");
//
//            System.out.print("▶ 구매 수량 입력 : ");
//            int quantity = Integer.parseInt(scanner.nextLine().trim());
//
//            // =========================================================
//            // [getProductById 활용 2] 조회한 상품 정보로 재고 수량 사전 검증
//            // =========================================================
//            if (quantity <= 0) {
//                System.out.println("❌ 수량은 1개 이상 입력해야 합니다.");
//                continue;
//            }
//
//            if (product.getStock() < quantity) {
//                System.out.println("❌ 재고가 부족합니다. (현재 재고: " + product.getStock() + "개)");
//                continue;
//            }
//
//            // =========================================================
//            // [getProductById 활용 3] product.getPrice()에서 단가를 가져와 세팅
//            // =========================================================
//            int orderPrice = product.getPrice(); // DB에서 가져온 실제 단가
//            OrderItemDTO item = new OrderItemDTO(0, 0, productId, quantity, orderPrice);
//            orderItemList.add(item);
//
//            // 단가 * 수량을 합산하여 현재까지의 결제 예정 금액 누적
//            totalPrice += (orderPrice * quantity);
//
//            System.out.println(">> [" + product.getProductName() + " " + quantity + "개] 장바구니에 담겼습니다.");
//            System.out.println("   현재 총 결제 예정 금액: " + totalPrice + "원");
//        }
//
//        // 장바구니 검증
//        if (orderItemList.isEmpty()) {
//            System.out.println("❌ 주문할 상품이 선택되지 않아 결제를 취소합니다.");
//            return;
//        }
//
//        // 최종 주문 DTO 생성 (자동 합산된 totalPrice 전달)
//        OrderDTO orderDTO = new OrderDTO(0, paymentType, totalPrice, null);
//
//        // Service 트랜잭션 호출
//        boolean isSuccess = orderService.processOrder(orderDTO, orderItemList);
//
//        System.out.println("\n========================================");
//        if (isSuccess) {
//            System.out.println("✅ 결제가 완료되었습니다!");
//            System.out.println("   - 총 결제 금액: " + totalPrice + "원");
//            System.out.println("   - 결제 수단: " + paymentType);
//        } else {
//            System.out.println("❌ 결제 처리에 실패했습니다.");
//        }
//        System.out.println("========================================");
//    }
//}