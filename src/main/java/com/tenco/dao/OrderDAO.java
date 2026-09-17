package com.tenco.dao;

import com.tenco.dto.Order;
import com.tenco.dto.OrderItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    // 1. 주문 생성 (생성된 PK 반환)
    public int insertOrder(Connection conn, Order dto) throws SQLException {
        String sql = """
                INSERT INTO orders (payment_type, total_price) VALUES (?,?)
                """;
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
    public Order selectOrderById(Connection conn, int orderId) throws SQLException {
        String sql = """
                SELECT order_id, payment_type, total_price, order_date FROM orders WHERE order_id = ?
                """;
        Order dto = null;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    dto = new Order(
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
    public List<Order> selectAllOrders(Connection conn) throws SQLException {
        String sql = """
                SELECT order_id, payment_type, total_price, order_date FROM orders ORDER BY order_id DESC
                """;
        List<Order> list = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(new Order(
                        rs.getInt("order_id"),
                        rs.getString("payment_type"),
                        rs.getInt("total_price"),
                        rs.getTimestamp("order_date")
                ));
            }
        }
        return list;
    }

    public int cancelOrder(Connection conn, int orderId) throws SQLException {
        String sql = """
            DELETE FROM orders
            WHERE order_id = ?
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            return pstmt.executeUpdate();
        }
    }

    // orderItem 에서 수량 변경 시 반영되는 부분을 위한 DAO
    public int updateTotalPrice(Connection conn, int orderId, int totalPrice) throws SQLException {
        String sql = """
            UPDATE orders
            SET total_price = ?
            WHERE order_id = ?
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, totalPrice);
            pstmt.setInt(2, orderId);

            return pstmt.executeUpdate();
        }
    }

    // TODO - 추가
    // 주문 상품 수량 변경 및 재고, 총 금액 반영 (트랜잭션) (Connection 외부 주입 방식)
    public void updateOrderItemAndStock(Connection conn,int orderItemId, int orderId, int productId, int newQuantity, int priceDiff, int quantityDiff) throws SQLException {


        // order_items 테이블 수량 UPDATE
        String updateItemSql = """
                UPDATE order_item SET quantity = ? WHERE order_item_id = ?
                """;
        // order 테이블의 total_price UPDATE
        String updateOrderSql = """
                UPDATE orders SET total_price = total_price + ? WHERE order_id = ?
                """;
        // product 테이블의 stock UPDATE (추가 구매시 차감, 구매 수량 감소시 원복)
        String updateStockSql = """
                UPDATE product SET stock = stock - ? WHERE product_id = ? AND (stock >= ? OR ? <= 0)
                """;

        try (PreparedStatement itemStmt = conn.prepareStatement(updateItemSql);
             PreparedStatement orderStmt = conn.prepareStatement(updateOrderSql);
             PreparedStatement stockStmt = conn.prepareStatement(updateStockSql)) {

            // order_item 변경
            itemStmt.setInt(1, newQuantity);
            itemStmt.setInt(2, orderItemId); // PK 조건 지정
            if (itemStmt.executeUpdate() == 0) {
                throw new SQLException("주문 항목 수정 실패 (주문번호/상품 ID 불일치)");
            }

            // orders 변경
            orderStmt.setInt(1, priceDiff);
            orderStmt.setInt(2, orderId);
            if (orderStmt.executeUpdate() == 0) {
                throw new SQLException("주문 총 금액 수정 실패 (주문번호 불일치)");
            }

            // product 변경
            stockStmt.setInt(1, quantityDiff);
            stockStmt.setInt(2, productId);
            stockStmt.setInt(3, quantityDiff); // stock >= quantityDiff 검사용
            stockStmt.setInt(4, quantityDiff); // 수량이 감소(quantityDiff 음수)하는 경우
            if (stockStmt.executeUpdate() == 0) {
                throw new SQLException("상품 재고 수정 실패 (상품 ID 불일치)");
            }
        }
    }
    // 주문 취소 (Batch Processing 적용)
    public void cancelOrderTransaction(Connection conn, int orderId, List<OrderItem> itemList) throws SQLException {
        String updateStockSql = """
                UPDATE product SET stock = stock + ? WHERE product_id = ?
                """;
        // TODO - status  제거
        //String updateOrderSql = "UPDATE orders SET status = 'CANCELLED' WHERE order_id = ?";

        // 1) 재고 원복 (Batch 처리)
        try (PreparedStatement stockStmt = conn.prepareStatement(updateStockSql)) {
            for (OrderItem item : itemList) {
                stockStmt.setInt(1, item.getQuantity());
                stockStmt.setInt(2, item.getProductId());
                stockStmt.addBatch();
            }
            int[] results = stockStmt.executeBatch();
            for (int count : results) {
                if (count == 0) {
                    throw new SQLException("상품 재고 원복 중 일부 항목 실패");
                }
            }
        }
        // TODO - status  제거
//        // 2) 주문 상태 변경
//        try (PreparedStatement orderStmt = conn.prepareStatement(updateOrderSql)) {
//            orderStmt.setInt(1, orderId);
//            if (orderStmt.executeUpdate() == 0) {
//                throw new SQLException("주문 상태 변경 실패 (주문 ID: " + orderId + ")");
//            }
//        }
    }

}
