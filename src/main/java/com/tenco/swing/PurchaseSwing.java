package com.tenco.swing;

import com.tenco.Service.PurchaseService;
import com.tenco.dto.PurchaseDto;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class PurchaseSwing extends JPanel {

    private final PurchaseService purchaseService = new PurchaseService();
    private JTable table;
    private DefaultTableModel tableModel;

    public PurchaseSwing() {
        initUI();
        purchaseAllView();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        JLabel titleLabel = new JLabel("발주 / 입고 관리", SwingConstants.CENTER);
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        add(titleLabel, BorderLayout.NORTH);

        String[] columns = {
                "발주 ID", "상품 ID", "상품명",
                "발주 수량", "개당 가격", "총 발주 가격"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(25);
        table.getTableHeader().setReorderingAllowed(false);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 3, 10, 10));

        JButton allButton = new JButton("전체 조회");
        JButton searchButton = new JButton("상품 ID 조회");
        JButton addButton = new JButton("발주 신청");
        JButton deleteButton = new JButton("발주 취소");
        JButton subtractButton = new JButton("발주 수량 차감");
        JButton closeButton = new JButton("닫기");

        buttonPanel.add(allButton);
        buttonPanel.add(searchButton);
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(subtractButton);
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);

        allButton.addActionListener(e -> purchaseAllView());
        searchButton.addActionListener(e -> searchPurchase());
        addButton.addActionListener(e -> addPurchase());
        deleteButton.addActionListener(e -> deletePurchase());
        subtractButton.addActionListener(e -> subtractPurchase());

        closeButton.addActionListener(e -> {
            Container parent = getParent();
            if (parent != null) {
                parent.remove(this);
                parent.revalidate();
                parent.repaint();
            }
        });
    }

    private void purchaseAllView() {
        try {
            List<PurchaseDto> list = purchaseService.getAllPurchases();
            tableModel.setRowCount(0);

            for (PurchaseDto p : list) {
                tableModel.addRow(new Object[]{
                        p.getPurchaseId(),
                        p.getProductId(),
                        p.getName(),
                        p.getQauntity(),
                        p.getUnitPrice(),
                        p.getTotlaPrice()
                });
            }
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void searchPurchase() {
        String input = JOptionPane.showInputDialog(this, "조회할 상품 ID를 입력해주세요.");

        if (input == null) {
            return;
        }

        try {
            int productId = Integer.parseInt(input.trim());
            PurchaseDto p = purchaseService.existList(productId);

            tableModel.setRowCount(0);
            tableModel.addRow(new Object[]{
                    p.getPurchaseId(),
                    p.getProductId(),
                    p.getName(),
                    p.getQauntity(),
                    p.getUnitPrice(),
                    p.getTotlaPrice()
            });
        } catch (NumberFormatException e) {
            showError("상품 ID는 숫자로 입력해주세요.");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void addPurchase() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField productIdField = new JTextField();
        JTextField quantityField = new JTextField();

        panel.add(new JLabel("상품 ID"));
        panel.add(productIdField);
        panel.add(new JLabel("발주 수량"));
        panel.add(quantityField);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "발주 신청",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            int productId = Integer.parseInt(productIdField.getText().trim());
            int quantity = Integer.parseInt(quantityField.getText().trim());

            purchaseService.purcahseProduct(productId, quantity);

            JOptionPane.showMessageDialog(this, "발주 신청이 완료되었습니다.");
            purchaseAllView();
        } catch (NumberFormatException e) {
            showError("상품 ID와 수량은 숫자로 입력해주세요.");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void deletePurchase() {
        String input = JOptionPane.showInputDialog(this, "삭제할 발주 ID를 입력해주세요.");

        if (input == null) {
            return;
        }

        try {
            int purchaseId = Integer.parseInt(input.trim());

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "정말 발주를 취소하시겠습니까?",
                    "발주 취소",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            purchaseService.deletePurchase(purchaseId);

            JOptionPane.showMessageDialog(this, "발주가 취소되었습니다.");
            purchaseAllView();
        } catch (NumberFormatException e) {
            showError("발주 ID는 숫자로 입력해주세요.");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void subtractPurchase() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField purchaseIdField = new JTextField();
        JTextField quantityField = new JTextField();

        panel.add(new JLabel("발주 ID"));
        panel.add(purchaseIdField);
        panel.add(new JLabel("차감 수량"));
        panel.add(quantityField);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "발주 수량 차감",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            int purchaseId = Integer.parseInt(purchaseIdField.getText().trim());
            int quantity = Integer.parseInt(quantityField.getText().trim());

            purchaseService.substractPurchase(purchaseId, quantity);

            JOptionPane.showMessageDialog(this, "발주 수량이 차감되었습니다.");
            purchaseAllView();
        } catch (NumberFormatException e) {
            showError("발주 ID와 수량은 숫자로 입력해주세요.");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void showError(String message) {
        if (message == null || message.isEmpty()) {
            message = "알 수 없는 오류가 발생했습니다.";
        }

        JOptionPane.showMessageDialog(
                this,
                message,
                "오류",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
