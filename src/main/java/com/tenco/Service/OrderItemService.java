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

//    // 주문 상품 등록
//    // TODO -주문등록 필요없어서 삭제
//    public int addOrderItem(OrderItem orderItem) throws SQLException {
//        if (orderItem == null) {
//            throw new SQLException("주문한 상품 정보가 없습니다.");
//        } else if (orderItem.getOrderId() <= 0 || orderItem.getProductId() <= 0) {
//            throw new SQLException("주문자 번호와 상품번호를 입력해주세요.");
//        } else if (orderItem.getQuantity() <= 0) {
//            throw new SQLException("상품은 1개 이상입니다.");
//        }
//        return orderItemDAO.addOrderItem(orderItem);
//    }

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

    // 주문 수정
    // 트랜잭션 처리
    // 주문 수정 시 재고도 변경되어야 하며, order의 총 금액도 변경되어야 한다.
    public int updateOrderItem(int quantity, int orderItemId) throws SQLException {
        if (orderItemId <= 0) {
            throw new SQLException("주문상품 번호를 입력해주세요.");
        }
        if (quantity <= 0) {
            throw new SQLException("수량은 1개 이상이어야 합니다.");
        }
        Connection conn = null;

        try {
            conn = util.getConnection();
            conn.setAutoCommit(false);
            // 1. 기존 주문상품 조회
            OrderItem orderItem = orderItemDAO.selectById(conn, orderItemId);

            if (orderItem == null) {
                conn.rollback();
                return 0;
            }

            int oldQuantity = orderItem.getQuantity();
            int productId = orderItem.getProductId();
            int orderId = orderItem.getOrderId();

            // 2. 기존 수량과 변경 수량의 차이
            int difference = quantity - oldQuantity;

            // 3. 수량 증가 후 재고 차감
            if (difference > 0) {
                Product product = productDAO.selectProductById(conn, productId);
                if (product == null) {
                    throw new SQLException("상품이 존재하지 않습니다.");
                }
                if (product.getStock() < difference) {
                    throw new SQLException("상품 재고가 부족합니다.");
                }
                int stockResult = productDAO.outStock(conn, productId, difference);
                if (stockResult == 0) {
                    conn.rollback();
                    return 0;
                }
            }

            // 4. 수량 감소 후 재고 복구
            else if (difference < 0) {
                int stockResult = productDAO.inStock(conn, productId, -difference);
                if (stockResult == 0) {
                    conn.rollback();
                    return 0;
                }
            }

            // 5. order_item 수량 변경
            int result = orderItemDAO.updateOrderItem(conn, quantity, orderItemId);
            if (result == 0) {
                conn.rollback();
                return 0;
            }

            // 6. 변경된 order_item들을 기준으로 주문 총액 계산
            int totalPrice = orderItemDAO.sumOrderItem(conn, orderId);

            // 7. orders.total_price 변경
            int orderResult = orderDAO.updateTotalPrice(conn, orderId, totalPrice);
            if (orderResult == 0) {
                conn.rollback();
                return 0;
            }
            // 8. 전부 성공
            conn.commit();
            return result;
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 주문 상품 삭제
    // 트랜잭션 처리
    // 삭제 시 재고 복구, order의 주문이 여러개면 반영해서 변경, 1건이면 삭제되어야한다.
    public int deleteOrderItem(int orderItemId) throws SQLException {
        if (orderItemId <= 0) {
            throw new SQLException("주문상품 번호를 입력해주세요.");
        }
        Connection conn = null;
        try {
            conn = util.getConnection();
            conn.setAutoCommit(false);

            // 1. 삭제하기 전에 기존 주문상품 조회
            OrderItem orderItem = orderItemDAO.selectById(conn, orderItemId);
            if (orderItem == null) {
                conn.rollback();
                return 0;
            }

            int orderId = orderItem.getOrderId();
            int quantity = orderItem.getQuantity();
            int productId = orderItem.getProductId();

            // 2. 주문상품 수량만큼 상품 재고 복구
            int stockResult = productDAO.inStock(conn, productId, quantity);
            if (stockResult == 0) {
                conn.rollback();
                return 0;
            }

            // 3. 주문상품 삭제
            int result = orderItemDAO.deleteOrderItem(conn, orderItemId);

            if (result == 0) {
                conn.rollback();
                return 0;
            }
            // 4. 해당 주문에 남은 상품 개수 확인
            int itemCount = orderItemDAO.countOrderItem(conn, orderId);

            // 5. 남은 상품이 없으면 주문 자체 삭제
            if (itemCount == 0) {
                int orderResult = orderDAO.cancelOrder(conn, orderId);
                if (orderResult == 0) {
                    conn.rollback();
                    return 0;
                }
            } else {
                // 6. 상품이 남아 있으면 주문 총액 갱신
                int totalPrice = orderItemDAO.sumOrderItem(conn, orderId);
                int orderResult = orderDAO.updateTotalPrice(conn, orderId, totalPrice);
                if (orderResult == 0) {
                    conn.rollback();
                    return 0;
                }
            }

            // 7. 전부 성공
            conn.commit();
            return result;
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
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
