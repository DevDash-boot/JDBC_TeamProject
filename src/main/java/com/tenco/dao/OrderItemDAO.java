package com.tenco.dao;

import com.tenco.dto.OrderItem;
import com.tenco.util.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    // 주문 상품 수량 수정(UPDATE)
    public int updateOrderItem(int quantity, int orderItemId) {
        int rows = 0;
        String sql = """
                UPDATE order_item SET quantity = ?
                WHERE order_item_id = ?;
                """;
        try (Connection conn = util.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, quantity);
                pstmt.setInt(2, orderItemId);
                rows = pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return rows;
    }

    // 주문 상품 삭제(DELETE)
    public int deleteOrderItem(int orderItemId) {
        int rows = 0;
        String sql = """
                DELETE FROM order_item
                WHERE order_item_id = ?;
                """;
        try (Connection conn = util.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, orderItemId);
                rows = pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return rows;
    }

    // 주문별 총 금액 계산(SELECT + SUM)
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
}
