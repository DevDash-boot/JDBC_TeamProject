package com.tenco.dao;

import com.tenco.dto.Order;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    // 1. 주문 생성 (생성된 PK 반환)
    public int insertOrder(Connection conn, Order dto) throws SQLException {
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
    public Order selectOrderById(Connection conn, int orderId) throws SQLException {
        String sql = "SELECT order_id, payment_type, total_price, order_date FROM orders WHERE order_id = ?";
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
        String sql = "SELECT order_id, payment_type, total_price, order_date FROM orders ORDER BY order_id DESC";
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
}
