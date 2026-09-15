package com.tenco.dao;

import com.tenco.dto.OrderItemDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OrderItemDAOImpl implements OrderItemDAO {

    // 1. 주문 상품 등록
    @Override
    public int insertOrderItem(Connection conn, OrderItemDTO dto) throws SQLException {
        String sql = "INSERT INTO order_item (order_id, product_id, quantity, order_price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, dto.getOrderId());
            pstmt.setInt(2, dto.getProductId());
            pstmt.setInt(3, dto.getQuantity());
            pstmt.setInt(4, dto.getOrderPrice());
            return pstmt.executeUpdate();
        }
    }

    // 2. 다건 주문 상품 등록 (Batch Processing)
    @Override
    public int[] insertOrderItems(Connection conn, List<OrderItemDTO> dtoList) throws SQLException {
        String sql = "INSERT INTO order_item (order_id, product_id, quantity, order_price) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (OrderItemDTO dto : dtoList) {
                pstmt.setInt(1, dto.getOrderId());
                pstmt.setInt(2, dto.getProductId());
                pstmt.setInt(3, dto.getQuantity());
                pstmt.setInt(4, dto.getOrderPrice());
                pstmt.addBatch();
            }
            return pstmt.executeBatch();
        }
    }

    // 3. 특정 주문의 상품 목록 조회
    @Override
    public List<OrderItemDTO> selectItemByOrderId(Connection conn, int orderId) throws SQLException {
        String sql = "SELECT order_item_id, order_id, product_id, quantity, order_price FROM order_item WHERE order_id = ?";
        List<OrderItemDTO> list = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1,orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new OrderItemDTO(
                            rs.getInt("order_item_id"),
                            rs.getInt("order_id"),
                            rs.getInt("product_id"),
                            rs.getInt("quantity"),
                            rs.getInt("order_price")
                    ));
                }
            }
        }
        return list;
    }
}
