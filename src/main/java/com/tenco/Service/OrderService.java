package com.tenco.Service;

import com.tenco.dao.*;
import com.tenco.dto.OrderDTO;
import com.tenco.dto.OrderItemDTO;
import com.tenco.dto.ProductDTO;
import com.tenco.util.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class OrderService {
    private final OrderDAO orderDAO = new OrderDAO();
    private final OrderItemDAO orderItemDAO = new OrderItemDAOImpl();
    private final ProductDAO productDAO = new ProductDAOImpl();

    /**
     * 1. 신규 주문 처리 (트랜잭션)
     * - orders 저장 -> order_id 발급
     * - order_item 저장
     * - product 재고 차감
     */
    public boolean processOrder(OrderDTO orders, List<OrderItemDTO> items) {
        Connection conn = null;
        try {
            conn = util.getConnection();
            // 자동 커밋 해제 (트랜잭션 시작)
            conn.setAutoCommit(false);

            // 1. orders 테이블 저장 & 생성된 order_id 가져오기
            int orderId = orderDAO.insertOrder(conn, orders);
            if (orderId == 0) {
                conn.rollback();
                return false;
            }

            // 2. order_item 저장 및 상품 재고 차감
            for (OrderItemDTO itemDTO : items) {
                itemDTO.setOrderId(orderId); // 받아은 order_id 세팅

                ProductDTO product =productDAO.selectProductById(conn, itemDTO.getProductId());
                if (product == null || product.getStock() < itemDTO.getQuantity()) {
                    System.out.println("상품 [ID:" +itemDTO.getProductId() +"]의 재고가 부족합니다.");
                    conn.rollback();
                    return false;
                }

                // 주문 상품 저장
                int itemResult = orderItemDAO.insertOrderItem(conn, itemDTO);
                if (itemResult == 0) {
                    conn.rollback();
                    return false;
                }

                 // 재고 차감 (ProductDAO 차감 메서드)
                int stockResult = productDAO.outStock(conn, itemDTO.getProductId(), itemDTO.getQuantity());
                if (stockResult == 0) {
                    conn.rollback();
                    return false;
                }
            }
            conn.commit(); // 성공시 커밋
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // 예외 발생시 롤백
                } catch (SQLException rollbackEX) {
                    rollbackEX.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEX) {
                    closeEX.printStackTrace();
                }
            }
        }
    }

    // 2. 주문 단건 조회
    public OrderDTO getOrderById(int orderId) {
        try (Connection conn = util.getConnection()) {
            return orderDAO.selectOrderById(conn, orderId);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 3. 특정 주문의 상품 목록 조회

    public List<OrderItemDTO> getOrderItemsByOrderId(int orderId) {
        try (Connection conn = util.getConnection()) {
            return orderItemDAO.selectItemByOrderId(conn, orderId);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 4. 전체 주문 목록 조회

    public List<OrderDTO> getAllOrders() {
        try (Connection conn = util.getConnection()) {
            return orderDAO.selectAllOrders(conn);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // View에서 입력받은 productId로 DB에서 상품 정보(단가 포함)를 읽어오는 메서드
    public ProductDTO getProductById(int productId) {
        try (Connection conn = util.getConnection()) {
            return productDAO.selectProductById(conn, productId);
        } catch (SQLException e) {
            System.err.println("상품 조회 실패: " + e.getMessage());
            return null;
        }
    }

    // 완료된 주문의 상품 수량 변경 및 재고/총금액 반영
    public boolean updateOrderItemQuantity(int orderId, int productId, int oldQuantity, int newQuantity, int price) {
        // 수량 차이 계산 (양수: 추가 재고 차감 / 음수: 재고 환원)
        int quantityDIff = newQuantity - oldQuantity;
        int priceDiff = quantityDIff * price;

        // DB 트랜잭션 수행 (Connection commit/rollback)
        // 1. order_items 테이블의 수량(quantity) UPDATE
        // 2. orders 테이블의 total_price UPDATE (기존 total_price + priceDiff)
        // 3. products 테이블의 stock UPDATE (기존 stock - quantityDiff)
        return orderDAO.updateOrderItemAndStock(orderId, productId, newQuantity, priceDiff, quantityDIff);
    }

    // 주문 취소 처리 (재고 복구 + 주문 데이터 처리)
    public boolean cancelOrder(int orderId, List<OrderItemDTO> itemList) {
        // DB 트랜잭션 수행 (Connection commit/rollback)
        // 1. itemList를 순회하며 각 상품(productId)의 재고(stock)를 수량(quantity)만큼 증가(+)
        // 2. order_items 테이블에서 해당 order_id 관련 레코드 삭제 (또는 status='CANCELED' 업데이트)
        // 3. orders 테이블에서 해당 order_id 레코드 삭제 (또는 status='CANCELED' 업데이트)

        return orderDAO.cancelOrderTransaction(orderId, itemList);
    }

}
