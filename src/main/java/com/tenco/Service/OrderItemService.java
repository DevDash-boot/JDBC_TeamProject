package com.tenco.Service;

import com.tenco.dao.OrderItemDAO;
import com.tenco.dto.OrderItem;

import java.sql.SQLException;
import java.util.List;

public class OrderItemService {
    private final OrderItemDAO orderItemDAO = new OrderItemDAO();

    // 주문 상품 등록
    public int addOrderItem(OrderItem orderItem) throws SQLException {
        if (orderItem == null) {
            throw new SQLException("주문한 상품 정보가 없습니다.");
        } else if (orderItem.getOrderId() <= 0 || orderItem.getProductId() <= 0) {
            throw new SQLException("주문자 번호와 상품번호를 입력해주세요.");
        } else if (orderItem.getQuantity() <= 0) {
            throw new SQLException("상품은 1개 이상입니다.");
        }
        return orderItemDAO.addOrderItem(orderItem);
    }

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

    // 주문 상품 수량 수정
    public int updateOrderItem(int quantity, int orderItemId) throws SQLException {
        if (orderItemId <= 0) {
            throw new SQLException("주문번호를 입력해주세요.");
        } else if (quantity <= 0) {
            throw new SQLException("수량은 1개 이상이어야 합니다.");
        }
        return orderItemDAO.updateOrderItem(quantity, orderItemId);
    }

    // 주문 상품 삭제
    public int deleteOrderItem(int orderItemId) throws SQLException {
        if (orderItemId <= 0) {
            throw new SQLException("주문번호를 입력해주세요.");
        }
        return orderItemDAO.deleteOrderItem(orderItemId);
    }

    // 주문 합계 금액 조회
    public int sumOrderItem(int orderId) throws SQLException {
        if (orderId <= 0) {
            throw new SQLException("주문자 번호를 입력해주세요.");
        }
        return orderItemDAO.sumOrderItem(orderId);
    }
}
