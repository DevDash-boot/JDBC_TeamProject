package com.tenco.swing;

import com.tenco.Main;
import com.tenco.Service.AuthService;
import com.tenco.dto.Admin;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class AdminSwing extends JPanel {

    private final Main main;
    private final AuthService service = new AuthService();

    // 현재 로그인한 관리자 정보
    private Integer currentAdminId = null;
    private String currentAdminName = null;

    // 화면 구성
    private final JPanel contentPanel = new JPanel(new BorderLayout());

    // 로그인 상태 표시
    private final JLabel loginStatusLabel = new JLabel("로그아웃 상태");

    // 관리자 목록 테이블
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"관리자 ID", "로그인 ID", "이름"}, 0
    );

    private final JTable adminTable = new JTable(tableModel);

    public AdminSwing(Main main) {
        this.main = main;

        setLayout(new BorderLayout());

        JPanel menuPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton loginButton = new JButton("로그인");
        JButton registerButton = new JButton("관리자 등록");
        JButton listButton = new JButton("전체 목록 조회");
        JButton logoutButton = new JButton("로그아웃");

        menuPanel.add(loginButton);
        menuPanel.add(registerButton);
        menuPanel.add(listButton);
        menuPanel.add(logoutButton);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statusPanel.add(loginStatusLabel);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(menuPanel, BorderLayout.WEST);
        topPanel.add(statusPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(contentPanel, BorderLayout.CENTER);

        loginButton.addActionListener(e -> loginMenu());

        registerButton.addActionListener(e -> {
            if (requireAdmin("신규 관리자 등록")) {
                registerMenu();
            }
        });

        listButton.addActionListener(e -> {
            if (requireAdmin("관리자 전체 목록 조회")) {
                showAllAdmins();
            }
        });

        logoutButton.addActionListener(e -> logout());

        showWelcome();
    }

    // 관리자 로그인
    private void loginMenu() {
        if (isAdminLoggedIn()) {
            JOptionPane.showMessageDialog(
                    this,
                    "현재 로그인 상태입니다.",
                    "로그인",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        contentPanel.removeAll();

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("관리자 로그인");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 22));

        JLabel idLabel = new JLabel("로그인 ID");
        JTextField idField = new JTextField(20);

        JLabel passwordLabel = new JLabel("비밀번호");
        JPasswordField passwordField = new JPasswordField(20);

        JButton loginButton = new JButton("로그인");

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        formPanel.add(titleLabel, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        formPanel.add(idLabel, gbc);

        gbc.gridx = 1;
        formPanel.add(idField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        formPanel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        formPanel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        formPanel.add(loginButton, gbc);

        contentPanel.add(formPanel, BorderLayout.CENTER);

        loginButton.addActionListener(e -> {
            String loginId = idField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (loginId.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "로그인 ID를 입력해주세요."
                );
                idField.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "비밀번호를 입력해주세요."
                );
                passwordField.requestFocus();
                return;
            }

            try {
                Admin admin = service.authenticationAdmin(loginId, password);

                if (admin == null) {
                    JOptionPane.showMessageDialog(
                            this,
                            "로그인 실패!\nID 또는 비밀번호를 잘못 입력했습니다.",
                            "로그인 실패",
                            JOptionPane.ERROR_MESSAGE
                    );
                } else {
                    currentAdminId = admin.getAdminId();
                    currentAdminName = admin.getName();

                    // Main에도 로그인 정보 전달
                    main.loginAdmin(admin);

                    updateLoginStatus();

                    JOptionPane.showMessageDialog(
                            this,
                            currentAdminName + " 관리자님이 로그인했습니다!",
                            "로그인 성공",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    showWelcome();
                }

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "로그인 오류!\n" + ex.getMessage(),
                        "오류",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
        refreshPanel();
    }

    // 관리자 등록
    private void registerMenu() {
        contentPanel.removeAll();

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("신규 관리자 등록");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 22));

        JLabel idLabel = new JLabel("로그인 ID");
        JTextField idField = new JTextField(20);

        JLabel passwordLabel = new JLabel("비밀번호");
        JPasswordField passwordField = new JPasswordField(20);

        JLabel nameLabel = new JLabel("이름");
        JTextField nameField = new JTextField(20);

        JButton registerButton = new JButton("관리자 등록");

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        formPanel.add(titleLabel, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        formPanel.add(idLabel, gbc);

        gbc.gridx = 1;
        formPanel.add(idField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        formPanel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        formPanel.add(passwordField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        formPanel.add(nameLabel, gbc);

        gbc.gridx = 1;
        formPanel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        formPanel.add(registerButton, gbc);

        contentPanel.add(formPanel, BorderLayout.CENTER);

        registerButton.addActionListener(e -> {
            String loginId = idField.getText().trim();
            String password = new String(passwordField.getPassword());
            String name = nameField.getText().trim();

            if (loginId.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "로그인 ID를 입력해주세요."
                );
                idField.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "비밀번호를 입력해주세요."
                );
                passwordField.requestFocus();
                return;
            }

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "이름을 입력해주세요."
                );
                nameField.requestFocus();
                return;
            }

            Admin admin = Admin.builder()
                    .loginId(loginId)
                    .password(password)
                    .name(name)
                    .build();

            try {
                service.registrationAdmin(admin);

                JOptionPane.showMessageDialog(
                        this,
                        "신규 관리자가 성공적으로 등록되었습니다!",
                        "등록 성공",
                        JOptionPane.INFORMATION_MESSAGE
                );

                idField.setText("");
                passwordField.setText("");
                nameField.setText("");

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "등록 실패!\n" + ex.getMessage(),
                        "등록 실패",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        refreshPanel();
    }

    // 관리자 전체 목록
    private void showAllAdmins() {
        contentPanel.removeAll();
        tableModel.setRowCount(0);

        List<Admin> adminList = service.getAllAdmin();

        if (adminList == null || adminList.isEmpty()) {
            JLabel emptyLabel = new JLabel(
                    "등록된 관리자가 없습니다.",
                    SwingConstants.CENTER
            );
            contentPanel.add(
                    emptyLabel,
                    BorderLayout.CENTER
            );
        } else {
            for (Admin admin : adminList) {
                tableModel.addRow(new Object[]{
                        admin.getAdminId(),
                        admin.getLoginId(),
                        admin.getName()
                });
            }

            JScrollPane scrollPane =
                    new JScrollPane(adminTable);

            contentPanel.add(
                    scrollPane,
                    BorderLayout.CENTER
            );
        }

        refreshPanel();
    }

    // 로그아웃
    private void logout() {
        if (!isAdminLoggedIn()) {
            JOptionPane.showMessageDialog(
                    this,
                    "현재 로그인 상태가 아닙니다.",
                    "로그아웃",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        JOptionPane.showMessageDialog(
                this,
                currentAdminName + " 관리자님이 로그아웃되었습니다."
        );

        currentAdminId = null;
        currentAdminName = null;

        // Main에도 로그아웃 정보 전달
        main.logoutAdmin();

        updateLoginStatus();
        showWelcome();
    }

    // 관리자 권한 확인
    private boolean requireAdmin(String menuName) {
        if (!isAdminLoggedIn()) {
            JOptionPane.showMessageDialog(
                    this,
                    "관리자만 " + menuName + " 기능을 이용할 수 있습니다.",
                    "접근 권한 없음",
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        return true;
    }

    // 로그인 여부
    private boolean isAdminLoggedIn() {
        return currentAdminId != null;
    }

    // 로그인 상태 표시
    private void updateLoginStatus() {
        if (isAdminLoggedIn()) {
            loginStatusLabel.setText(
                    "로그인: " + currentAdminName + " 관리자"
            );
        } else {
            loginStatusLabel.setText("로그아웃 상태");
        }
    }

    // 처음 화면
    private void showWelcome() {
        contentPanel.removeAll();

        JLabel label = new JLabel(
                "관리자 관리 시스템",
                SwingConstants.CENTER
        );

        label.setFont(
                new Font(
                        "맑은 고딕",
                        Font.BOLD,
                        24
                )
        );

        contentPanel.add(
                label,
                BorderLayout.CENTER
        );

        refreshPanel();
    }

    // 화면 갱신
    private void refreshPanel() {
        contentPanel.revalidate();
        contentPanel.repaint();
    }
}