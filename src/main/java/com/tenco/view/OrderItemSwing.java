package com.tenco.view;

import com.tenco.Service.OrderItemService;
import com.tenco.dto.OrderItem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class OrderItemSwing extends JPanel {
    private final OrderItemService orderItemService = new OrderItemService();
    private final DefaultTableModel tableModel;
    private final JTable table;

    public OrderItemSwing() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 상단
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(Color.WHITE);

        JLabel title = new JLabel("주문한 상품");
        title.setFont(new Font("맑은 고딕", Font.BOLD, 24));

        JButton addButton = new JButton("주문 등록");
        JButton selectButton = new JButton("특정 주문 조회");
        JButton allButton = new JButton("전체 조회");
        JButton updateButton = new JButton("수량 수정");
        JButton deleteButton = new JButton("상품 삭제");
        JButton sumButton = new JButton("주문 총액");

        topPanel.add(title);
        topPanel.add(Box.createHorizontalStrut(15));
        topPanel.add(addButton);
        topPanel.add(selectButton);
        topPanel.add(allButton);
        topPanel.add(updateButton);
        topPanel.add(deleteButton);
        topPanel.add(sumButton);

        add(topPanel, BorderLayout.NORTH);

        // 테이블
        String[] columns = {"주문상품ID", "주문ID", "상품ID", "상품명", "수량", "가격"};

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 14));

        add(new JScrollPane(table), BorderLayout.CENTER);

        // 버튼 이벤트
        addButton.addActionListener(e -> addOrderItem());
        selectButton.addActionListener(e -> selectOrderItem());
        allButton.addActionListener(e -> allOrderItem());
        updateButton.addActionListener(e -> updateOrderItem());
        deleteButton.addActionListener(e -> deleteOrderItem());
        sumButton.addActionListener(e -> sumOrderItem());

        // 처음 화면에 전체 주문 상품 출력
        allOrderItem();
    }

    // 주문 등록
    private void addOrderItem() {
        try {
            int orderId = readInt("주문 번호를 입력하세요.");
            int productId = readInt("상품 번호를 입력하세요.");
            int quantity = readInt("수량을 입력하세요.");
            int orderPrice = readInt("가격을 입력하세요.");

            OrderItem orderItem = OrderItem.builder()
                    .orderId(orderId)
                    .productId(productId)
                    .quantity(quantity)
                    .orderPrice(orderPrice)
                    .build();

            orderItemService.addOrderItem(orderItem);

            JOptionPane.showMessageDialog(this, "주문 상품이 등록되었습니다.");
            allOrderItem();
        } catch (Exception e) {
            showError(e);
        }
    }

    // 특정 주문 조회
    private void selectOrderItem() {
        try {
            int orderId = readInt("조회할 주문 번호를 입력하세요.");
            List<OrderItem> orderItemList = orderItemService.selectOrderItem(orderId);

            tableModel.setRowCount(0);
            addRows(orderItemList);

            if (orderItemList.isEmpty()) {
                JOptionPane.showMessageDialog(this, "해당 주문의 상품이 없습니다.");
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    // 전체 주문 조회
    private void allOrderItem() {
        try {
            List<OrderItem> orderItemList = orderItemService.allOrderItem();

            tableModel.setRowCount(0);
            addRows(orderItemList);
        } catch (Exception e) {
            showError(e);
        }
    }

    // 주문 상품 수량 수정
    private void updateOrderItem() {
        try {
            int orderItemId = readInt("수정할 주문 상품 ID를 입력하세요.");
            int quantity = readInt("변경할 수량을 입력하세요.");

            int rows = orderItemService.updateOrderItem(quantity, orderItemId);

            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "상품 수량이 변경되었습니다.");
                allOrderItem();
            } else {
                JOptionPane.showMessageDialog(this, "수정할 상품이 없습니다.");
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    // 주문 상품 삭제
    private void deleteOrderItem() {
        try {
            int orderItemId = readInt("삭제할 주문 상품 ID를 입력하세요.");

            int result = JOptionPane.showConfirmDialog(
                    this,
                    "주문 상품 ID " + orderItemId + "를 삭제하시겠습니까?",
                    "삭제 확인",
                    JOptionPane.YES_NO_OPTION
            );

            if (result != JOptionPane.YES_OPTION) {
                return;
            }

            int rows = orderItemService.deleteOrderItem(orderItemId);

            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "상품이 삭제되었습니다.");
                allOrderItem();
            } else {
                JOptionPane.showMessageDialog(this, "삭제할 상품이 없습니다.");
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    // 주문별 총 금액
    private void sumOrderItem() {
        try {
            int orderId = readInt("총 금액을 조회할 주문 번호를 입력하세요.");
            int totalPrice = orderItemService.sumOrderItem(orderId);

            JOptionPane.showMessageDialog(
                    this,
                    "주문 ID " + orderId + "의 총 금액은 " + totalPrice + "원입니다.",
                    "주문 총액",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception e) {
            showError(e);
        }
    }

    // 테이블 데이터 추가
    private void addRows(List<OrderItem> orderItemList) {
        for (OrderItem o : orderItemList) {
            tableModel.addRow(new Object[]{
                    o.getOrderItemId(),
                    o.getOrderId(),
                    o.getProductId(),
                    o.getProductName(),
                    o.getQuantity(),
                    o.getOrderPrice()
            });
        }
    }

    // 숫자 입력
    private int readInt(String message) {
        while (true) {
            String input = JOptionPane.showInputDialog(this, message);

            if (input == null) {
                throw new RuntimeException("입력이 취소되었습니다.");
            }

            try {
                return Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(
                        this,
                        "숫자를 입력해주세요.",
                        "입력 오류",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        }
    }

    // 오류 출력
    private void showError(Exception e) {
        JOptionPane.showMessageDialog(
                this,
                "오류: " + e.getMessage(),
                "오류",
                JOptionPane.ERROR_MESSAGE
        );
    }
}