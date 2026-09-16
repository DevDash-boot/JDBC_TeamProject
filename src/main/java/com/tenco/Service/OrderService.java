package com.tenco.Service;

import com.tenco.dao.OrderDAO;
import com.tenco.dao.OrderItemDAO;
import com.tenco.dao.ProductDAO;
import com.tenco.dto.Order;
import com.tenco.dto.OrderItem;
import com.tenco.dto.Product;
import com.tenco.util.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class OrderService {
    private final OrderDAO orderDAO = new OrderDAO();
    private final OrderItemDAO orderItemDAO = new OrderItemDAO();
    private final ProductDAO productDAO = new ProductDAO();

    /**
     * 1. 신규 주문 처리 (트랜잭션)
     * - orders 저장 -> order_id 발급
     * - order_item 저장
     * - product 재고 차감
     */
    public boolean processOrder(Order orders, List<OrderItem> items) {
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
            for (OrderItem itemDTO : items) {
                itemDTO.setOrderId(orderId); // 받아은 order_id 세팅

                Product product =productDAO.selectProductById(conn, itemDTO.getProductId());
                if (product == null || product.getStock() < itemDTO.getQuantity()) {
                    System.out.println("상품 [ID:" +itemDTO.getProductId() +"]의 재고가 부족합니다.");
                    conn.rollback();
                    return false;
                }

                // 주문 상품 저장
                // addOrderItem이랑 겹침(아마)
                int itemResult = orderItemDAO.insertOrderItem(conn, itemDTO);
                if (itemResult == 0) {
                    conn.rollback();
                    return false;
                }
                int stockResult = productDAO.outStock(conn, itemDTO.getProductId(), itemDTO.getQuantity());
                if (stockResult == 0) {
                    conn.rollback();
                    return false;
                }
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
    public Order getOrderById(int orderId) {
        try (Connection conn = util.getConnection()) {
            return orderDAO.selectOrderById(conn, orderId);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 3. 특정 주문의 상품 목록 조회
    public List<OrderItem> getOrderItemsByOrderId(int orderId) {
        try (Connection conn = util.getConnection()) {
            return orderItemDAO.selectItemByOrderId(conn, orderId);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 4. 전체 주문 목록 조회

    public List<Order> getAllOrders() {
        try (Connection conn = util.getConnection()) {
            return orderDAO.selectAllOrders(conn);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // View에서 입력받은 productId로 DB에서 상품 정보(단가 포함)를 읽어오는 메서드
    public Product getProductById(int productId) {
        try (Connection conn = util.getConnection()) {
            return productDAO.selectProductById(conn, productId);
        } catch (SQLException e) {
            System.err.println("상품 조회 실패: " + e.getMessage());
            return null;
        }
    }

    public boolean cancelOrder(int orderId) {
        Connection conn = null;
        try {
            conn = util.getConnection();
            conn.setAutoCommit(false);
            // 1. 주문 상품 조회
            List<OrderItem> items = orderItemDAO.selectItemByOrderId(conn, orderId);
            if (items.isEmpty()) {
                conn.rollback();
                return false;
            }

            // 2. 재고 복원
            for (OrderItem item : items) {
                int result = productDAO.inStock(conn, item.getProductId(), item.getQuantity());
                if (result == 0) {
                    conn.rollback();
                    return false;
                }
            }

            // 3. order_item 삭제
            int itemResult = orderItemDAO.deleteByOrderId(conn, orderId);

            if (itemResult == 0) {
                conn.rollback();
                return false;
            }

            // 4. orders 삭제
            int orderResult = orderDAO.cancelOrder(conn, orderId);

            if (orderResult == 0) {
                conn.rollback();
                return false;
            }
            // 5. 전부 성공
            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}