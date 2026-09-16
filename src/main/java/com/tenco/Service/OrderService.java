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
    private final ProductDAO productDAO = new ProductDAO();

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

                ProductDTO product = productDAO.selectProductById(conn, itemDTO.getProductId());
                if (product == null || product.getStock() < itemDTO.getQuantity()) {
                    System.out.println("상품 [ID:" + itemDTO.getProductId() + "]의 재고가 부족합니다.");
                    conn.rollback();
                    return false;
                }

                // 재고 차감 (ProductDAO 구매시 차감 메서드)
                int stockResult = productDAO.buyOutStock(conn, itemDTO.getProductId(), itemDTO.getQuantity());
                if (stockResult == 0) {
                    System.err.println("상품 [ID: " + itemDTO.getProductId() + "]의 재고가 부족하거나 존재하지 않습니다.");
                    conn.rollback();
                    return false;
                }
                
                // 주문 상품 일괄 저장 (Batch Insert)
                int[] itemResult = orderItemDAO.insertOrderItems(conn, items);
                for (int count : itemResult) {
                    if (count == 0) {
                        conn.rollback();
                        return false;
                    }
                }
            }
            conn.commit(); // 성공시 커밋
            return true;

        } catch (SQLException e) {
            rollbackQuietly(conn);
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(conn);
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

    // 완료된 주문의 상품 수량 변경 및 재고/총금액 반영 (트랜잭션)
    public boolean updateOrderItemQuantity(int orderId, int productId, int oldQuantity, int newQuantity, int price) {
        // 수량 차이 계산 (양수: 추가 재고 차감 / 음수: 재고 환원)
        int quantityDIff = newQuantity - oldQuantity;
        int priceDiff = quantityDIff * price;

        Connection conn = null;
        try {
            conn = util.getConnection();
            conn.setAutoCommit(false);

            // DAO 를 통한 개별 처리
            orderDAO.updateOrderItemAndStock(conn, orderId, productId, newQuantity, priceDiff, quantityDIff);

            conn.commit();
            return true;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(conn);
        }
    }

    // 주문 취소 처리 (재고 복구 + 주문 상태 CANCELLED) (트랜잭션)
    public boolean cancelOrder(int orderId, List<OrderItemDTO> itemList) {
        Connection conn = null;
        try {
            conn = util.getConnection();
            conn.setAutoCommit(false);

            // 각 상품 재고 복구
            orderDAO.cancelOrderTransaction(conn, orderId, itemList);

            conn.commit();
            return true;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            e.printStackTrace();
            return false;
        } finally {
            closeQuietly(conn);
        }
    }

    // --- 중복 Methods ---
    // 트랜잭션 롤백 중복 부분
    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // 트랜잭션 자원해제 중복 부분
    private void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

