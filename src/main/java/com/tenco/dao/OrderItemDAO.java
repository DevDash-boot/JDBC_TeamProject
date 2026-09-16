package com.tenco.dao;

import com.tenco.dto.OrderItemDTO;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface OrderItemDAO {
    // 1. 주문 상품 등록
    int insertOrderItem(Connection conn, OrderItemDTO dto) throws SQLException;

    // 2. 다건 주문 등록
    public int[] insertOrderItems(Connection conn, List<OrderItemDTO> dtoList) throws SQLException;

    // 3. 특정 주문의 상품 목록 조회
    List<OrderItemDTO> selectItemByOrderId(Connection conn, int orderId) throws SQLException;
}