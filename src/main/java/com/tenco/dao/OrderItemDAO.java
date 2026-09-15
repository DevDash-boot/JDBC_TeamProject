package com.tenco.dao;

import com.tenco.dto.OrderItem;
import com.tenco.util.util;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderItemDAO {


    // 주문 상품 등록(INSERT)
    public int addOrderItem(OrderItem orderItem) throws SQLException {
        int rows = 0;
        String sql = """
                INSERT INTO order_item(order_id, product_id, quantity, order_price)
                VALUES (?, ?, ?, ?);
                """;
        try (Connection conn = util.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, orderItem.getOrderId());
                pstmt.setInt(2, orderItem.getProductId());
                pstmt.setInt(3, orderItem.getQuantity());
                pstmt.setInt(4, orderItem.getOrderPrice());
                rows = pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return rows;
    }

    // 특정 주문 상품 조회(SELECT + WHERE)
    public List<OrderItem> selectOrderItem(int orderId) {
        List<OrderItem> orderItemList = new ArrayList<>();
        String sql = """
                SELECT
                    oi.order_item_id,
                    oi.order_id,
                    oi.product_id,
                    p.product_name,
                    oi.quantity,
                    oi.order_price
                FROM order_item oi
                JOIN product p
                ON oi.product_id = p.product_id
                WHERE oi.order_id = ?;
                """;
        try (Connection conn = util.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, orderId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    selectDB(rs, orderItemList);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return orderItemList;
    }

    // 주문 전체 조회(SELECT)
    public List<OrderItem> allOrderItem() {
        List<OrderItem> orderItemList = new ArrayList<>();
        String sql = """
            SELECT
                oi.order_item_id,
                oi.order_id,
                oi.product_id,
                p.product_name,
                oi.quantity,
                oi.order_price
            FROM order_item oi
            JOIN product p
            ON oi.product_id = p.product_id
            ORDER BY oi.order_item_id;
            """;
        try (Connection conn = util.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = pstmt.executeQuery()) {
                    selectDB(rs, orderItemList);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return orderItemList;
    }

    public int updateOrderItem(Connection conn, int quantity, int orderItemId) throws SQLException {
        String sql = """
            UPDATE order_item
            SET quantity = ?
            WHERE order_item_id = ?
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, quantity);
            pstmt.setInt(2, orderItemId);
            return pstmt.executeUpdate();
        }
    }

    public int deleteOrderItem(Connection conn, int orderItemId) throws SQLException {
        String sql = """
            DELETE FROM order_item
            WHERE order_item_id = ?
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderItemId);
            return pstmt.executeUpdate();
        }
    }

    // 주문별 금액 계산(SELECT + SUM)
    public int sumOrderItem(int orderId) {
        int totalPrice = 0;
        String sql = """
                SELECT SUM(order_price * quantity) AS total_price
                FROM order_item
                WHERE order_id = ?;
                """;
        try (Connection conn = util.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, orderId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        totalPrice = rs.getInt("total_price");
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return totalPrice;
    }

    private static void selectDB(ResultSet rs, List<OrderItem> orderItemList) throws SQLException {
        while (rs.next()) {
            orderItemList.add(OrderItem.builder()
                    .orderItemId(rs.getInt("order_item_id"))
                    .orderId(rs.getInt("order_id"))
                    .productId(rs.getInt("product_id"))
                    .productName(rs.getString("product_name"))
                    .quantity(rs.getInt("quantity"))
                    .orderPrice(rs.getInt("order_price"))
                    .build());
        }
    }

    // 다른 코드와 트랜잭션 용
    ///////////////////////////////
    public int insertOrderItem(Connection conn, OrderItem orderItem) throws SQLException {
        String sql = """
            INSERT INTO order_item(order_id, product_id, quantity, order_price)
            VALUES (?, ?, ?, ?)
            """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, orderItem.getOrderId());
            pstmt.setInt(2, orderItem.getProductId());
            pstmt.setInt(3, orderItem.getQuantity());
            pstmt.setInt(4, orderItem.getOrderPrice());
            int rows = pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) orderItem.setOrderItemId(rs.getInt(1));
            }
            return rows;
        }
    }

    public List<OrderItem> selectItemByOrderId(Connection conn, int orderId) throws SQLException {
        List<OrderItem> list = new ArrayList<>();
        String sql = """
            SELECT oi.order_item_id, oi.order_id, oi.product_id, p.product_name, oi.quantity, oi.order_price
            FROM order_item oi JOIN product p ON oi.product_id = p.product_id
            WHERE oi.order_id = ?
            """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                selectDB(rs, list); // 기존 private 메서드 재사용
            }
        }
        return list;
    }

    public OrderItem selectById(Connection conn, int orderItemId) throws SQLException {
        String sql = """
            SELECT order_item_id, order_id, product_id, quantity, order_price
            FROM order_item
            WHERE order_item_id = ?
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderItemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return OrderItem.builder()
                            .orderItemId(rs.getInt("order_item_id"))
                            .orderId(rs.getInt("order_id"))
                            .productId(rs.getInt("product_id"))
                            .quantity(rs.getInt("quantity"))
                            .orderPrice(rs.getInt("order_price"))
                            .build();
                }
            }
        }
        return null;
    }

    // 삭제 시 재고 관련 변경 부분
    public int deleteByOrderId(Connection conn, int orderId) throws SQLException {
        String sql = """
            DELETE FROM order_item
            WHERE order_id = ?
            """;
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            return pstmt.executeUpdate();
        }
    }

    // 주문 수정 시 변경되는 합계금액 부분
    public int sumOrderItem(Connection conn, int orderId) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(order_price * quantity), 0) AS total_price
            FROM order_item
            WHERE order_id = ?
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total_price");
                }
            }
        }
        return 0;
    }

    // 남은 상품 확인하는 곳
    public int countOrderItem(Connection conn, int orderId) throws SQLException {
        String sql = """
        SELECT COUNT(*)
        FROM order_item
        WHERE order_id = ?
        """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }
}