package com.tenco.Service;

import com.tenco.dao.OrderDAO;
import com.tenco.dao.OrderItemDAO;
import com.tenco.dao.ProductDAO;
import com.tenco.dto.OrderItem;
import com.tenco.dto.Product;
import com.tenco.util.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class OrderItemService {
    private final OrderItemDAO orderItemDAO = new OrderItemDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final ProductDAO productDAO = new ProductDAO();

    // 특정 주문 상품 조회
    public List<OrderItem> selectOrderItem(int orderId) throws SQLException {
        if (orderId <= 0) {
            throw new SQLException("주문자 번호를 입력해주세요.");
        }
        return orderItemDAO.selectOrderItem(orderId);
    }

    // 주문 전체 조회
    public List<OrderItem> allOrderItem() {
        return orderItemDAO.allOrderItem();
    }

    // 주문 수정 - 트랜잭션 처리
    // 주문 수정 시 재고도 변경되어야 하며, order의 총 금액도 변경되어야 한다.
    public int updateOrderItem(int quantity, int orderItemId) throws SQLException {
        if (orderItemId <= 0) {
            throw new SQLException("주문상품 번호를 입력해주세요.");
        }
        if (quantity <= 0) {
            throw new SQLException("수량은 1개 이상이어야 합니다.");
        }
        try (Connection conn = util.getConnection()) {
            // 트랜잭션 시작
            conn.setAutoCommit(false);
            try {
                OrderItem orderItem = orderItemDAO.selectById(conn, orderItemId);
                if (orderItem == null) {
                    return 0;
                }
                int oldQuantity = orderItem.getQuantity();
                int difference = quantity - oldQuantity;
                // 수량 증가 → 재고 차감
                if (difference > 0) {
                    Product product = productDAO.selectProductById(conn, orderItem.getProductId());
                    if (product == null) {
                        throw new SQLException("상품이 존재하지 않습니다.");
                    }
                    if (product.getStock() < difference) {
                        throw new SQLException("상품 재고가 부족합니다.");
                    }
                    if (productDAO.outStock(conn, orderItem.getProductId(), difference) == 0) {
                        throw new SQLException("상품 재고 차감에 실패했습니다.");
                    }
                }
                // 수량 감소 → 재고 증가
                else if (difference < 0) {
                    if (productDAO.inStock(conn, orderItem.getProductId(), -difference) == 0) {
                        throw new SQLException("상품 재고 복구에 실패했습니다.");
                    }
                }
                // 주문상품 수량 변경
                if (orderItemDAO.updateOrderItem(conn, quantity, orderItemId) == 0) {
                    throw new SQLException("주문상품 수정에 실패했습니다.");
                }
                // 주문 총액 갱신
                int totalPrice = orderItemDAO.sumOrderItem(conn, orderItem.getOrderId());
                if (orderDAO.updateTotalPrice(conn, orderItem.getOrderId(), totalPrice) == 0) {
                    throw new SQLException("주문 처리에 실패했습니다.");
                }
                conn.commit();
                return 1;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
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

    // 주문 상품 삭제 - 트랜잭션 처리
    // 삭제 시 재고 복구, order의 주문이 여러개면 반영해서 변경, 1건이면 삭제되어야한다.
    public int deleteOrderItem(int orderItemId) throws SQLException {
        if (orderItemId <= 0) {
            throw new SQLException("주문상품 번호를 입력해주세요.");
        }
        try (Connection conn = util.getConnection()) {
            // 트랜잭션 시작
            conn.setAutoCommit(false);
            try {
                OrderItem orderItem = orderItemDAO.selectById(conn, orderItemId);
                if (orderItem == null) {
                    return 0;
                }
                // 재고 복구
                if (productDAO.inStock(conn, orderItem.getProductId(), orderItem.getQuantity()) == 0) {
                    throw new SQLException("상품 재고 복구에 실패했습니다.");
                }
                // 주문상품 삭제
                if (orderItemDAO.deleteOrderItem(conn, orderItemId) == 0) {
                    throw new SQLException("주문상품 삭제에 실패했습니다.");
                }
                // 남은 주문상품 확인
                int orderId = orderItem.getOrderId();
                if (orderItemDAO.countOrderItem(conn, orderId) == 0) {
                    // 남은 상품이 없으면 주문 삭제
                    if (orderDAO.cancelOrder(conn, orderId) == 0) {
                        throw new SQLException("주문 처리에 실패했습니다.");
                    }
                } else {
                    // 남은 상품이 있으면 주문 금액 수정
                    int totalPrice = orderItemDAO.sumOrderItem(conn, orderId);
                    if (orderDAO.updateTotalPrice(conn, orderId, totalPrice) == 0) {
                        throw new SQLException("주문 처리에 실패했습니다.");
                    }
                }
                conn.commit();
                return 1;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
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

    // 주문 합계 금액 조회
    public int sumOrderItem(int orderId) throws SQLException {
        if (orderId <= 0) {
            throw new SQLException("주문자 번호를 입력해주세요.");
        }
        return orderItemDAO.sumOrderItem(orderId);
    }
}
