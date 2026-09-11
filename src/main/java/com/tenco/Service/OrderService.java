package com.tenco.Service;

import com.tenco.dao.OrderDAO;
import com.tenco.dao.OrderItemDAO;
import com.tenco.dto.OrderDTO;
import com.tenco.dto.OrderItemDTO;
import com.tenco.util.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static com.tenco.util.util.getConnection;

public class OrderService {
    private final OrderDAO orderDAO = new OrderDAO();
    private final OrderItemDAO orderItemDAO = new OrderItemDAO();
//    private final ProductDAO productDAO = new ProductDAO();

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
            conn.setAutoCommit(false); // 트랜잭션

            // 1. orders 테이블 저장 & 생성된 order_id 가져오기
            int orderId = orderDAO.insertOrder(conn, orders);
            if (orderId == 0) {
                conn.rollback();
                return false;
            }

            // 2. order_item 저장 및 상품 재고 차감
            for (OrderItemDTO itemDTO : items) {
                itemDTO.setOrderId(orderId); // 받아은 order_id 세팅

                // 주문 상품 저장
                int itemResult = orderItemDAO.insertOrderItem(conn, itemDTO);
                if (itemResult == 0) {
                    conn.rollback();
                    return false;
                }

                // 재고 차감 (차감 수량은 음수로 전달)
                // ProductDAO 없어서 구현 불가
                // int stockResult = productDAO
                // if (stockResult == 0) {
                //    conn.rollback();
                //    return false;
                // }
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




}
