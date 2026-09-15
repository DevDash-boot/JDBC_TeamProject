package com.tenco.dao;

import com.tenco.dto.OrderDTO;
import com.tenco.dto.OrderItemDTO;
import com.tenco.util.util;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    // 1. 주문 생성 (생성된 PK 반환)
    public int insertOrder(Connection conn, OrderDTO dto) throws SQLException {
        String sql = "INSERT INTO orders (payment_type, total_price) VALUES (?,?)";
        int generatedId = 0;

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, dto.getPaymentType());
            pstmt.setInt(2, dto.getTotalPrice());
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    generatedId = rs.getInt(1);
                }
            }
        }
        return generatedId;
    }

    // 2. 단건 조회
    public OrderDTO selectOrderById(Connection conn, int orderId) throws SQLException {
        String sql = "SELECT order_id, payment_type, total_price, order_date FROM orders WHERE order_id = ?";
        OrderDTO dto = null;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    dto = new OrderDTO(
                            rs.getInt("order_id"),
                            rs.getString("payment_type"),
                            rs.getInt("total_price"),
                            rs.getTimestamp("order_date")
                    );
                }
            }
        }
        return dto;
    }

    // 3. 전체 목록 조회
    // 3. 전체 목록 조회
    public List<OrderDTO> selectAllOrders(Connection conn) throws SQLException {
        String sql = "SELECT order_id, payment_type, total_price, order_date FROM orders ORDER BY order_id DESC";
        List<OrderDTO> list = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(new OrderDTO(
                        rs.getInt("order_id"),
                        rs.getString("payment_type"),
                        rs.getInt("total_price"),
                        rs.getTimestamp("order_date")
                ));
            }
        }
        return list;
    }

    // 주문 상품 수량 변경 및 재고, 총 금액 반영 (트랜잭션)
    public boolean updateOrderItemAndStock(int orderId, int productId, int newQuantity, int priceDiff, int quantityDiff) {
        Connection conn = null;
        PreparedStatement updateItemStmt = null;
        PreparedStatement updateOrderStmt = null;
        PreparedStatement updateStockStmt = null;

        // order_items 테이블 수량 UPDATE
        String updatedItemSql = "UPDATE order_item SET quantity = ? WHERE order_id = ? AND product_id = ?";
        // order 테이블의 total_price UPDATE
        String updateOrderSql = "UPDATE orders SET total_price = total_price + ? WHERE order_id = ?";
        // product 테이블의 stock UPDATE (추가 구매시 차감, 구매 수량 감소시 원복)
        String updateStockSql = "UPDATE product SET stock = stock - ? WHERE product_id = ?";

        try {
            conn = util.getConnection();
            conn.setAutoCommit(false); // 자동 커밋 비활성화

            // order_items 테이블 수량 수정
            updateItemStmt = conn.prepareStatement(updatedItemSql);
            updateItemStmt.setInt(1, newQuantity);
            updateItemStmt.setInt(2, orderId);
            updateItemStmt.setInt(3, productId);

            int itemResult = updateItemStmt.executeUpdate();
            if (itemResult == 0) {
                throw new SQLException("주문 항목 수정 실패 (주문번호 | 상품 ID 불일치)");
            }

            // orders 테이블 총 금액 수정
            updateOrderStmt = conn.prepareStatement(updateOrderSql);
            updateOrderStmt.setInt(1, priceDiff);
            updateOrderStmt.setInt(2, orderId);

            int orderResult = updateOrderStmt.executeUpdate();
            if (orderResult == 0) {
                throw new SQLException("주문 총 금액 수정 실패 (주문번호 불일치)");
            }

            // products 테이블 재고수정
            updateStockStmt = conn.prepareStatement(updateStockSql);
            updateStockStmt.setInt(1, quantityDiff); // quantityDiff 양수면 stock 감소 음수면 stock 증가
            updateStockStmt.setInt(2, productId);

            int stockResult = updateStockStmt.executeUpdate();
            if (stockResult == 0) {
                throw new SQLException("상품 재고 수정 실패 (상품 ID 불일치)");
            }

            // 전부 성공하면 커밋
            conn.commit();
            System.out.println("주문 수량 및 재고 변경 성공");
            return true;

        } catch (SQLException e) {
            System.out.println("주문 수량 변경 중 예외 발생 rollback 실행");
            if (conn != null) {
                try {
                    conn.rollback(); // 오류 시 전체 롤백
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            // 자원 해제 및 자동 커밋 원상복구
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            closeResources(updateItemStmt, updateOrderStmt, updateStockStmt, conn);
        }

    }

    // 주문 취소 (트랜잭션)
    public boolean cancelOrderTransaction(int orderId,List<OrderItemDTO> itemList) {
        Connection conn = null;
        PreparedStatement updateStockStmt = null;
        PreparedStatement updateOrderStmt = null;
        PreparedStatement deleteItemStmt = null;

        // 재고 원복
        String updateStockSql = "UPDATE product SET stock = stock + ? WHERE product_id = ?";
        // 주문 상태를 'CANCELED' 로 UPDATE (주문 내역에 주문 취소 기록 남기기)
        String updateOrderSql = "UPDATE orders SET status = 'CANCELED' WHERE order_id = ?";

        try {
            conn = util.getConnection();
            conn.setAutoCommit(false ); // 자동 커밋 비활성화 (트랜잭션 시작)

            // 재고 원복
            updateStockStmt = conn.prepareStatement(updateStockSql);
            for (OrderItemDTO item : itemList) {
                updateStockStmt.setInt(1, item.getQuantity());
                updateStockStmt.setInt(2, item.getProductId());

                // 매 반복마다 DB로 쿼리 실행 요청
                int affectedRows = updateStockStmt.executeUpdate();

                // 재고 수정이 이루어지지 않은 경우
                if (affectedRows == 0) {
                    throw new SQLException("상품 ID " + item.getProductId()  + "의 재고 원복 실패");
                }

            }

            // 주문 상태 'CANCELED' 로 업데이트
            updateOrderStmt = conn.prepareStatement(updateOrderSql);
            updateOrderStmt.setInt(1, orderId);
            int orderResult = updateOrderStmt.executeUpdate();

            if (orderResult == 0) {
                throw new SQLException("주문 상태 변경 실패 (주문 ID :"+ orderId + ")");
            }

            // 모두 성공시 커밋
            conn.commit();
            System.out.println("주문 취소 성공");
            return true;

        } catch (SQLException e) {
            System.out.println("오류 발생으로 인한 rollback");
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    e.printStackTrace();
                }
            }
            return false;
        } finally {
            // 자원 해제 및 자동 커밋 원상복구
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            closeResources(updateStockStmt, updateOrderStmt ,conn);
        }
    }

    // DB 자동 해제 메서드
    private void closeResources (AutoCloseable... resources) {
        for (AutoCloseable res : resources) {
            if (res != null) {
                try {
                    res.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
