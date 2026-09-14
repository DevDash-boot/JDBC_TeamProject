package com.tenco;

import com.tenco.view.OrderItemSwing;
import com.tenco.view.OrderItemView;
import com.tenco.view.StoreInfoSwing;

import javax.swing.*;
import java.awt.*;

public class Main extends JFrame {
    private boolean isAdmin = false;
    private final JPanel contentPanel = new JPanel(new BorderLayout());

    public Main() {
        setTitle("무인편의점 재고 관리 시스템");
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        add(createHeader(), BorderLayout.NORTH);
        add(createMenu(), BorderLayout.WEST);

        contentPanel.setBackground(Color.WHITE);
        contentPanel.add(createHomePanel(), BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);
    }

    // 상단
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setPreferredSize(new Dimension(0, 70));
        header.setBackground(new Color(35, 45, 65));

        JLabel title = new JLabel("  무인편의점 재고 관리 시스템");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("맑은 고딕", Font.BOLD, 24));

        JLabel status = new JLabel("관리자 로그아웃 상태  ");
        status.setForeground(Color.WHITE);
        status.setFont(new Font("맑은 고딕", Font.PLAIN, 14));

        header.add(title, BorderLayout.WEST);
        header.add(status, BorderLayout.EAST);

        return header;
    }

    // 왼쪽 메뉴
    private JPanel createMenu() {
        JPanel menu = new JPanel();
        menu.setPreferredSize(new Dimension(210, 0));
        menu.setBackground(new Color(245, 247, 250));
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));

        JButton productButton = createMenuButton("상품관리");
        productButton.addActionListener(e -> {
            if (!checkAdmin()) return;
            showMessage("상품관리 화면은 아직 준비 중입니다.");
        });
        menu.add(productButton);
        menu.add(Box.createVerticalStrut(10));

        JButton orderButton = createMenuButton("주문 / 결제");
        orderButton.addActionListener(e ->
                showMessage("주문 / 결제 화면은 아직 준비 중입니다."));
        menu.add(orderButton);
        menu.add(Box.createVerticalStrut(10));

        JButton orderItemButton = createMenuButton("주문한 상품");
        orderItemButton.addActionListener(e -> {
            showPanel(new OrderItemSwing());
        });
        menu.add(orderItemButton);
        menu.add(Box.createVerticalStrut(10));

        JButton purchaseButton = createMenuButton("발주 / 입고");
        purchaseButton.addActionListener(e -> {
            if (!checkAdmin()) return;
            showMessage("발주 / 입고 화면은 아직 준비 중입니다.");
        });
        menu.add(purchaseButton);
        menu.add(Box.createVerticalStrut(10));

        JButton adminButton = createMenuButton("관리자 정보");
        adminButton.addActionListener(e -> {
            if (!checkAdmin()) return;
            showMessage("관리자 정보 화면은 아직 준비 중입니다.");
        });
        menu.add(adminButton);
        menu.add(Box.createVerticalStrut(10));

        JButton loginButton = createMenuButton("관리자 로그인");
        loginButton.addActionListener(e -> {
            isAdmin = true;
            JOptionPane.showMessageDialog(this, "관리자 로그인 상태입니다.");
        });
        menu.add(loginButton);
        menu.add(Box.createVerticalStrut(10));

        // 매장 정보
        JButton storeInfoButton = createMenuButton("매장 정보");
        storeInfoButton.addActionListener(e -> {
            showPanel(new StoreInfoSwing());
        });
        menu.add(storeInfoButton);

        menu.add(Box.createVerticalGlue());

        // 프로그램 종료
        JButton exitButton = createMenuButton("프로그램 종료");
        exitButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "프로그램을 종료하시겠습니까?",
                    "종료",
                    JOptionPane.YES_NO_OPTION
            );

            if (result == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        });
        menu.add(exitButton);

        return menu;
    }

    // 메뉴 버튼 생성
    private JButton createMenuButton(String text) {
        JButton button = new JButton(text);
        button.setMaximumSize(new Dimension(180, 45));
        button.setPreferredSize(new Dimension(180, 45));
        button.setFont(new Font("맑은 고딕", Font.PLAIN, 15));
        button.setFocusPainted(false);
        return button;
    }

    // 메인 화면
    private JPanel createHomePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBackground(Color.WHITE);

        JLabel title = new JLabel("무인편의점 관리 시스템");
        title.setFont(new Font("맑은 고딕", Font.BOLD, 32));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel description = new JLabel("왼쪽 메뉴에서 원하는 기능을 선택하세요.");
        description.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        description.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel complete = new JLabel("현재 완성된 기능 : 주문한 상품 / 매장 정보");
        complete.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        complete.setAlignmentX(Component.CENTER_ALIGNMENT);

        box.add(title);
        box.add(Box.createVerticalStrut(15));
        box.add(description);
        box.add(Box.createVerticalStrut(20));
        box.add(complete);

        panel.add(box);

        return panel;
    }

    // 관리자 확인
    private boolean checkAdmin() {
        if (!isAdmin) {
            JOptionPane.showMessageDialog(
                    this,
                    "관리자 로그인이 필요합니다.",
                    "접근 제한",
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        return true;
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    // 화면 변경
    private void showPanel(JPanel panel) {
        contentPanel.removeAll();
        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Main main = new Main();
            main.setVisible(true);
        });
    }
}