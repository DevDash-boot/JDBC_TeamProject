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

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"상품 ID", "상품명", "발주 수량", "개당 가격", "총 발주 가격"}, 0
    );

    private final JTable table = new JTable(tableModel);

    public PurchaseSwing() {
        setLayout(new BorderLayout());

        // 왼쪽 메뉴
        JPanel menuPanel = new JPanel(new GridLayout(5, 1, 10, 10));
        menuPanel.setPreferredSize(new Dimension(200, 0));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        JButton allButton = new JButton("발주 목록 전체 조회");
        JButton searchButton = new JButton("상품 ID로 발주 조회");
        JButton addButton = new JButton("발주 신청");
        JButton deleteButton = new JButton("발주 취소");
        JButton subtractButton = new JButton("발주 수량 차감");

        menuPanel.add(allButton);
        menuPanel.add(searchButton);
        menuPanel.add(addButton);
        menuPanel.add(deleteButton);
        menuPanel.add(subtractButton);

        add(menuPanel, BorderLayout.WEST);

        // 오른쪽 내용
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("발주 / 입고 관리");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 24));

        contentPanel.add(titleLabel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        // 버튼 이벤트
        allButton.addActionListener(e -> purchaseAllView(contentPanel));
        searchButton.addActionListener(e -> purchaseSearch(contentPanel));
        addButton.addActionListener(e -> purchaseAdd());
        deleteButton.addActionListener(e -> purchaseDelete());
        subtractButton.addActionListener(e -> purchaseSubtract());
    }

    // 발주 목록 전체 조회
    private void purchaseAllView(JPanel contentPanel) {
        try {
            List<PurchaseDto> purchaseList = purchaseService.getAllPurchases();

            tableModel.setRowCount(0);

            for (PurchaseDto p : purchaseList) {
                tableModel.addRow(new Object[]{
                        p.getProductId(),
                        p.getName(),
                        p.getQauntity(),
                        p.getUnitPrice(),
                        p.getTotlaPrice()
                });
            }

            showTable(contentPanel, "발주 목록 전체 조회");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "발주 목록 조회 중 오류가 발생했습니다.\n" + e.getMessage(),
                    "오류",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // 상품 ID로 발주 조회
    private void purchaseSearch(JPanel contentPanel) {
        String input = JOptionPane.showInputDialog(this, "조회할 상품 ID를 입력해주세요.");

        if (input == null) {
            return;
        }

        try {
            int productId = Integer.parseInt(input);
            PurchaseDto p = purchaseService.existList(productId);

            if (p == null) {
                JOptionPane.showMessageDialog(
                        this,
                        "해당 상품의 발주 내역이 없습니다.",
                        "조회 결과",
                        JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }

            tableModel.setRowCount(0);
            tableModel.addRow(new Object[]{
                    p.getProductId(),
                    p.getName(),
                    p.getQauntity(),
                    p.getUnitPrice(),
                    p.getTotlaPrice()
            });

            showTable(contentPanel, "상품 ID로 발주 조회");

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "상품 ID는 숫자로 입력해주세요.",
                    "입력 오류",
                    JOptionPane.WARNING_MESSAGE
            );
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "발주 조회 중 오류가 발생했습니다.\n" + e.getMessage(),
                    "오류",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // 발주 신청
    private void purchaseAdd() {
        JTextField productIdField = new JTextField();
        JTextField quantityField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
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
            int productId = Integer.parseInt(productIdField.getText());
            int quantity = Integer.parseInt(quantityField.getText());

            if (productId <= 0 || quantity <= 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "상품 ID와 발주 수량은 1 이상이어야 합니다.",
                        "입력 오류",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            if (quantity > 20) {
                JOptionPane.showMessageDialog(
                        this,
                        "발주 수량은 최대 20개입니다.",
                        "입력 오류",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            purchaseService.purcahseProduct(productId, quantity);

            JOptionPane.showMessageDialog(
                    this,
                    "발주 신청이 완료되었습니다.",
                    "발주 신청",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "상품 ID와 수량은 숫자로 입력해주세요.",
                    "입력 오류",
                    JOptionPane.WARNING_MESSAGE
            );
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "발주 신청 중 오류가 발생했습니다.\n" + e.getMessage(),
                    "오류",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // 발주 취소
    private void purchaseDelete() {
        String input = JOptionPane.showInputDialog(
                this,
                "발주 목록에서 제거할 상품 ID를 입력해주세요."
        );

        if (input == null) {
            return;
        }

        try {
            int productId = Integer.parseInt(input);

            int result = JOptionPane.showConfirmDialog(
                    this,
                    "상품 ID " + productId + "의 발주를 취소하시겠습니까?",
                    "발주 취소",
                    JOptionPane.YES_NO_OPTION
            );

            if (result != JOptionPane.YES_OPTION) {
                return;
            }

            purchaseService.deletePurchase(productId);

            JOptionPane.showMessageDialog(
                    this,
                    "발주가 취소되었습니다.",
                    "발주 취소",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "상품 ID는 숫자로 입력해주세요.",
                    "입력 오류",
                    JOptionPane.WARNING_MESSAGE
            );
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "발주 취소 중 오류가 발생했습니다.\n" + e.getMessage(),
                    "오류",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // 발주 수량 차감
    private void purchaseSubtract() {
        JTextField productIdField = new JTextField();
        JTextField amountField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.add(new JLabel("상품 ID"));
        panel.add(productIdField);
        panel.add(new JLabel("차감 수량"));
        panel.add(amountField);

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
            int productId = Integer.parseInt(productIdField.getText());
            int amount = Integer.parseInt(amountField.getText());

            if (productId <= 0 || amount <= 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "상품 ID와 차감 수량은 1 이상이어야 합니다.",
                        "입력 오류",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            purchaseService.substractPurchase(productId, amount);

            JOptionPane.showMessageDialog(
                    this,
                    "발주 수량이 차감되었습니다.",
                    "발주 수정",
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "상품 ID와 수량은 숫자로 입력해주세요.",
                    "입력 오류",
                    JOptionPane.WARNING_MESSAGE
            );
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "발주 수량 차감 중 오류가 발생했습니다.\n" + e.getMessage(),
                    "오류",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // 테이블 출력
    private void showTable(JPanel contentPanel, String titleText) {
        contentPanel.removeAll();

        JLabel title = new JLabel(titleText);
        title.setFont(new Font("맑은 고딕", Font.BOLD, 20));

        contentPanel.add(title, BorderLayout.NORTH);
        contentPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        contentPanel.revalidate();
        contentPanel.repaint();
    }
}