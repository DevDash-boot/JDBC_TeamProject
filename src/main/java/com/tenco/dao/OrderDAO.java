package com.tenco.dao;

import com.tenco.dto.OrderDTO;

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
}
