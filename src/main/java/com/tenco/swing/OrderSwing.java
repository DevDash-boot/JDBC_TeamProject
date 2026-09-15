package com.tenco.swing;

import com.tenco.Service.OrderService;
import com.tenco.dto.Order;
import com.tenco.dto.OrderItem;
import com.tenco.dto.Product;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class OrderSwing extends JPanel {
    private final OrderService orderService = new OrderService();

    private final DefaultTableModel orderTableModel = new DefaultTableModel(
            new Object[]{"주문번호", "결제수단", "총 금액", "주문일시"}, 0
    );
    private final JTable orderTable = new JTable(orderTableModel);

    private final DefaultTableModel detailTableModel = new DefaultTableModel(
            new Object[]{"상품 ID", "상품명", "단가", "수량", "소계"}, 0
    );
    private final JTable detailTable = new JTable(detailTableModel);

    public OrderSwing() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton registerButton = new JButton("주문 등록(결제)");
        JButton allOrderButton = new JButton("주문 조회");
        JButton detailButton = new JButton("주문 상세 조회");
        JButton updateButton = new JButton("주문 상품 수량 변경");
        JButton cancelButton = new JButton("주문 취소");

        buttonPanel.add(registerButton);
        buttonPanel.add(allOrderButton);
        buttonPanel.add(detailButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.NORTH);

        JPanel tablePanel = new JPanel(new GridLayout(2, 1, 10, 10));

        JScrollPane orderScrollPane = new JScrollPane(orderTable);
        JScrollPane detailScrollPane = new JScrollPane(detailTable);

        orderScrollPane.setBorder(
                BorderFactory.createTitledBorder("주문 목록")
        );
        detailScrollPane.setBorder(
                BorderFactory.createTitledBorder("주문 상세 목록")
        );

        tablePanel.add(orderScrollPane);
        tablePanel.add(detailScrollPane);

        add(tablePanel, BorderLayout.CENTER);

        registerButton.addActionListener(e -> registerOrder());
        allOrderButton.addActionListener(e -> showAllOrders());
        detailButton.addActionListener(e -> showOrderDetail());
        updateButton.addActionListener(e -> updateOrderItemQuantity());
        cancelButton.addActionListener(e -> cancelOrder());

        showAllOrders();
    }

    private void registerOrder() {
        String[] paymentTypes = {"CARD", "CASH"};

        String paymentType = (String) JOptionPane.showInputDialog(
                this,
                "결제 수단을 선택하세요.",
                "주문 등록",
                JOptionPane.QUESTION_MESSAGE,
                null,
                paymentTypes,
                paymentTypes[0]
        );

        if (paymentType == null) {
            return;
        }

        List<OrderItem> items = new ArrayList<>();

        while (true) {
            String productIdInput = JOptionPane.showInputDialog(
                    this,
                    "구매할 상품 ID를 입력하세요.\n장바구니 담기를 완료하려면 0을 입력하세요.",
                    "상품 선택"
            );

            if (productIdInput == null) {
                return;
            }

            int productId;
            try {
                productId = Integer.parseInt(productIdInput.trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "상품 ID는 숫자로 입력해주세요.");
                continue;
            }

            if (productId == 0) {
                break;
            }

            Product product = orderService.getProductById(productId);

            if (product == null) {
                JOptionPane.showMessageDialog(this, "존재하지 않는 상품 ID입니다.");
                continue;
            }

            if (product.getStock() <= 0) {
                JOptionPane.showMessageDialog(this, "해당 상품의 재고가 없습니다.");
                continue;
            }

            int cartQuantity = items.stream()
                    .filter(item -> item.getProductId() == productId)
                    .mapToInt(OrderItem::getQuantity)
                    .sum();

            int availableStock = product.getStock() - cartQuantity;

            if (availableStock <= 0) {
                JOptionPane.showMessageDialog(this, "해당 상품의 재고를 모두 장바구니에 담았습니다.");
                continue;
            }

            String quantityInput = JOptionPane.showInputDialog(
                    this,
                    "상품명 : " + product.getProductName() +
                            "\n단가 : " + String.format("%,d", product.getPrice()) + "원" +
                            "\n현재 재고 : " + product.getStock() + "개" +
                            "\n장바구니 수량 : " + cartQuantity + "개" +
                            "\n추가 가능 수량 : " + availableStock + "개" +
                            "\n\n구매 수량을 입력하세요."
            );

            if (quantityInput == null) {
                continue;
            }

            int quantity;
            try {
                quantity = Integer.parseInt(quantityInput.trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "수량은 숫자로 입력해주세요.");
                continue;
            }

            if (quantity <= 0) {
                JOptionPane.showMessageDialog(this, "수량은 1개 이상이어야 합니다.");
                continue;
            }

            if (quantity > availableStock) {
                JOptionPane.showMessageDialog(
                        this,
                        "재고가 부족합니다.\n추가 가능 수량 : " + availableStock + "개"
                );
                continue;
            }

            OrderItem existingItem = items.stream()
                    .filter(item -> item.getProductId() == productId)
                    .findFirst()
                    .orElse(null);

            if (existingItem != null) {
                existingItem.setQuantity(existingItem.getQuantity() + quantity);
                JOptionPane.showMessageDialog(
                        this,
                        product.getProductName() + " 수량이 추가되었습니다.\n총 수량 : " +
                                existingItem.getQuantity() + "개"
                );
            } else {
                OrderItem item = new OrderItem(
                        productId,
                        quantity,
                        product.getPrice()
                );
                items.add(item);
                JOptionPane.showMessageDialog(
                        this,
                        product.getProductName() + " " + quantity + "개가 장바구니에 추가되었습니다."
                );
            }
        }

        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "주문할 상품이 선택되지 않았습니다.");
            return;
        }

        int totalPrice = items.stream()
                .mapToInt(item -> item.getOrderPrice() * item.getQuantity())
                .sum();

        Order order = new Order(paymentType, totalPrice);

        int result = JOptionPane.showConfirmDialog(
                this,
                "총 결제 금액 : " + String.format("%,d", totalPrice) + "원\n\n결제하시겠습니까?",
                "결제 확인",
                JOptionPane.YES_NO_OPTION
        );

        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        boolean isSuccess = orderService.processOrder(order, items);

        if (isSuccess) {
            JOptionPane.showMessageDialog(
                    this,
                    "결제가 완료되었습니다.\n총 결제 금액 : " +
                            String.format("%,d", totalPrice) + "원"
            );
            showAllOrders();
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "결제에 실패했습니다.",
                    "결제 실패",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void showAllOrders() {
        try {
            List<Order> orderList = orderService.getAllOrders();
            orderTableModel.setRowCount(0);

            if (orderList == null || orderList.isEmpty()) {
                JOptionPane.showMessageDialog(this, "등록된 주문이 없습니다.");
                return;
            }

            for (Order order : orderList) {
                String orderDate = order.getOrderDate() != null
                        ? order.getOrderDate().toString().substring(0, 16)
                        : "N/A";

                orderTableModel.addRow(new Object[]{
                        order.getOrderId(),
                        order.getPaymentType(),
                        String.format("%,d원", order.getTotalPrice()),
                        orderDate
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "주문 조회 중 오류가 발생했습니다.\n" + e.getMessage(),
                    "오류",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void showOrderDetail() {
        String orderIdInput = JOptionPane.showInputDialog(
                this,
                "조회할 주문 ID를 입력하세요.",
                "주문 상세 조회"
        );

        if (orderIdInput == null) {
            return;
        }

        int orderId;
        try {
            orderId = Integer.parseInt(orderIdInput.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "주문 ID는 숫자로 입력해주세요.");
            return;
        }

        Order order = orderService.getOrderById(orderId);

        if (order == null) {
            JOptionPane.showMessageDialog(this, "존재하지 않는 주문 번호입니다.");
            return;
        }

        List<OrderItem> itemList = orderService.getOrderItemsByOrderId(orderId);
        detailTableModel.setRowCount(0);

        if (itemList != null && !itemList.isEmpty()) {
            for (OrderItem item : itemList) {
                Product product = orderService.getProductById(item.getProductId());
                String productName = product != null
                        ? product.getProductName()
                        : "알 수 없음";

                int subTotal = item.getOrderPrice() * item.getQuantity();

                detailTableModel.addRow(new Object[]{
                        item.getProductId(),
                        productName,
                        String.format("%,d원", item.getOrderPrice()),
                        item.getQuantity(),
                        String.format("%,d원", subTotal)
                });
            }
        }

        JOptionPane.showMessageDialog(
                this,
                "주문번호 : " + order.getOrderId() +
                        "\n결제수단 : " + order.getPaymentType() +
                        "\n주문일시 : " + order.getOrderDate() +
                        "\n총 결제 금액 : " +
                        String.format("%,d원", order.getTotalPrice())
        );
    }

    private void updateOrderItemQuantity() {
        String orderIdInput = JOptionPane.showInputDialog(
                this,
                "수량을 변경할 주문 ID를 입력하세요.",
                "주문 상품 수량 변경"
        );

        if (orderIdInput == null) {
            return;
        }

        int orderId;
        try {
            orderId = Integer.parseInt(orderIdInput.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "주문 ID는 숫자로 입력해주세요.");
            return;
        }

        Order order = orderService.getOrderById(orderId);

        if (order == null) {
            JOptionPane.showMessageDialog(this, "존재하지 않는 주문번호입니다.");
            return;
        }

        List<OrderItem> itemList = orderService.getOrderItemsByOrderId(orderId);

        if (itemList == null || itemList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "주문 상품이 없습니다.");
            return;
        }

        String[] itemChoices = new String[itemList.size()];

        for (int i = 0; i < itemList.size(); i++) {
            OrderItem item = itemList.get(i);
            Product product = orderService.getProductById(item.getProductId());

            String productName = product != null
                    ? product.getProductName()
                    : "알 수 없음";

            itemChoices[i] =
                    "상품ID: " + item.getProductId() +
                            " | " + productName +
                            " | 현재 수량: " + item.getQuantity();
        }

        String selected = (String) JOptionPane.showInputDialog(
                this,
                "수량을 변경할 상품을 선택하세요.",
                "상품 선택",
                JOptionPane.QUESTION_MESSAGE,
                null,
                itemChoices,
                itemChoices[0]
        );

        if (selected == null) {
            return;
        }

        int selectedIndex = 0;

        for (int i = 0; i < itemChoices.length; i++) {
            if (itemChoices[i].equals(selected)) {
                selectedIndex = i;
                break;
            }
        }

        OrderItem selectedItem = itemList.get(selectedIndex);

        String quantityInput = JOptionPane.showInputDialog(
                this,
                "새로운 수량을 입력하세요.\n현재 수량 : " +
                        selectedItem.getQuantity(),
                "수량 변경"
        );

        if (quantityInput == null) {
            return;
        }

        int newQuantity;

        try {
            newQuantity = Integer.parseInt(quantityInput.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "수량은 숫자로 입력해주세요.");
            return;
        }

        if (newQuantity <= 0) {
            JOptionPane.showMessageDialog(this, "수량은 1개 이상이어야 합니다.");
            return;
        }

        JOptionPane.showMessageDialog(
                this,
                "수량 변경 기능은 OrderService 구현에 맞춰 연결해야 합니다."
        );
    }

    private void cancelOrder() {
        String orderIdInput = JOptionPane.showInputDialog(
                this,
                "취소할 주문 ID를 입력하세요.",
                "주문 취소"
        );

        if (orderIdInput == null) {
            return;
        }

        int orderId;

        try {
            orderId = Integer.parseInt(orderIdInput.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "주문 ID는 숫자로 입력해주세요.");
            return;
        }

        Order order = orderService.getOrderById(orderId);

        if (order == null) {
            JOptionPane.showMessageDialog(this, "존재하지 않는 주문번호입니다.");
            return;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                "주문번호 " + orderId + "번을 취소하시겠습니까?\n" +
                        "총 금액 : " +
                        String.format("%,d원", order.getTotalPrice()),
                "주문 취소 확인",
                JOptionPane.YES_NO_OPTION
        );

        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        JOptionPane.showMessageDialog(
                this,
                "현재 cancelOrder()는 Service 구현에 맞춰 연결해야 합니다."
        );
    }
}