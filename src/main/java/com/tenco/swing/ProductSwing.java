package com.tenco.swing;

import com.tenco.Service.ProductService;
import com.tenco.dto.Product;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ProductSwing extends JPanel {
    private final ProductService productService = new ProductService();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"상품 ID", "상품명", "가격", "바코드", "유통기한", "재고", "분류"}, 0);
    private final JTable table = new JTable(tableModel);
    private final JPanel contentPanel = new JPanel(new BorderLayout());

    public ProductSwing() {
        setLayout(new BorderLayout());

        JPanel menuPanel = new JPanel(new GridLayout(8, 1, 10, 10));
        menuPanel.setPreferredSize(new Dimension(200, 0));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        JButton addButton = new JButton("상품 추가");
        JButton nameButton = new JButton("상품명 목록");
        JButton updateButton = new JButton("상품 수정");
        JButton deleteButton = new JButton("상품 삭제");
        JButton allButton = new JButton("상품 전체 조회");
        JButton searchNameButton = new JButton("상품명 검색");
        JButton searchBarcodeButton = new JButton("상품 ID 검색");
        JButton stockButton = new JButton("재고 부족 상품");

        menuPanel.add(addButton);
        menuPanel.add(nameButton);
        menuPanel.add(updateButton);
        menuPanel.add(deleteButton);
        menuPanel.add(allButton);
        menuPanel.add(searchNameButton);
        menuPanel.add(searchBarcodeButton);
        menuPanel.add(stockButton);

        add(menuPanel, BorderLayout.WEST);

        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("상품 관리");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        add(contentPanel, BorderLayout.CENTER);

        addButton.addActionListener(e -> addProduct());
        nameButton.addActionListener(e -> getProductName());
        updateButton.addActionListener(e -> updateProduct());
        deleteButton.addActionListener(e -> deleteProduct());
        allButton.addActionListener(e -> getProduct());
        searchNameButton.addActionListener(e -> searchProductByName());
        searchBarcodeButton.addActionListener(e -> searchProductById());
        stockButton.addActionListener(e -> searchProductByStock());
    }

    // 상품 추가
    private void addProduct() {
        JTextField productNameField = new JTextField();
        JTextField priceField = new JTextField();
        JTextField barcodeField = new JTextField();
        JTextField expirationDateField = new JTextField();
        JTextField stockField = new JTextField();
        JTextField categoryField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(6, 2, 5, 5));
        panel.add(new JLabel("상품명"));
        panel.add(productNameField);
        panel.add(new JLabel("가격"));
        panel.add(priceField);
        panel.add(new JLabel("바코드"));
        panel.add(barcodeField);
        panel.add(new JLabel("유통기한"));
        panel.add(expirationDateField);
        panel.add(new JLabel("재고"));
        panel.add(stockField);
        panel.add(new JLabel("분류"));
        panel.add(categoryField);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "상품 추가",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) return;

        try {
            String productName = productNameField.getText().trim();
            int price = Integer.parseInt(priceField.getText().trim());
            String barcode = barcodeField.getText().trim();
            LocalDate expirationDate = LocalDate.parse(expirationDateField.getText().trim());
            int stock = Integer.parseInt(stockField.getText().trim());
            String category = categoryField.getText().trim();

            if (productName.isEmpty()) {
                showWarning("상품명은 필수입니다.");
                return;
            }

            if (price <= 0) {
                showWarning("가격은 1 이상이어야 합니다.");
                return;
            }

            if (barcode.isEmpty()) {
                showWarning("바코드는 필수입니다.");
                return;
            }

            if (stock < 0) {
                showWarning("재고는 0 이상이어야 합니다.");
                return;
            }

            Product product = Product.builder()
                    .productName(productName)
                    .price(price)
                    .barcode(barcode)
                    .expirationDate(expirationDate)
                    .stock(stock)
                    .category(category)
                    .build();

            productService.addProduct(product);

            showMessage("'" + productName + "' 상품이 추가되었습니다.");

            // 상품 추가 후 전체 상품 목록 갱신
            refreshProductTable();

        } catch (NumberFormatException e) {
            showWarning("가격과 재고는 숫자로 입력해주세요.");
        } catch (java.time.format.DateTimeParseException e) {
            showWarning("유통기한은 yyyy-MM-dd 형식으로 입력해주세요.");
        } catch (SQLException e) {
            showError("상품 추가 중 오류가 발생했습니다.", e);
        }
    }

    // 상품명 목록 조회
    private void getProductName() {
        try {
            List<Product> productList = productService.getProductName();

            tableModel.setRowCount(0);

            for (Product p : productList) {
                tableModel.addRow(new Object[]{
                        p.getProductId(),
                        p.getProductName()
                });
            }

            showTable("상품명 목록");

        } catch (SQLException e) {
            showError("상품 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    // 상품 수정
    private void updateProduct() {
        JTextField productIdField = new JTextField();
        JTextField productNameField = new JTextField();
        JTextField priceField = new JTextField();
        JTextField barcodeField = new JTextField();
        JTextField expirationDateField = new JTextField();
        JTextField stockField = new JTextField();
        JTextField categoryField = new JTextField();

        JPanel panel = new JPanel(new GridLayout(7, 2, 5, 5));
        panel.add(new JLabel("상품 ID"));
        panel.add(productIdField);
        panel.add(new JLabel("상품명"));
        panel.add(productNameField);
        panel.add(new JLabel("가격"));
        panel.add(priceField);
        panel.add(new JLabel("바코드"));
        panel.add(barcodeField);
        panel.add(new JLabel("유통기한"));
        panel.add(expirationDateField);
        panel.add(new JLabel("재고"));
        panel.add(stockField);
        panel.add(new JLabel("분류"));
        panel.add(categoryField);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "상품 수정",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result != JOptionPane.OK_OPTION) return;

        try {
            int productId = Integer.parseInt(productIdField.getText().trim());
            String productName = productNameField.getText().trim();
            int price = Integer.parseInt(priceField.getText().trim());
            String barcode = barcodeField.getText().trim();
            LocalDate expirationDate = LocalDate.parse(expirationDateField.getText().trim());
            int stock = Integer.parseInt(stockField.getText().trim());
            String category = categoryField.getText().trim();

            if (productId <= 0) {
                showWarning("상품 ID는 1 이상이어야 합니다.");
                return;
            }

            if (productName.isEmpty()) {
                showWarning("상품명은 필수입니다.");
                return;
            }

            if (price <= 0) {
                showWarning("가격은 1 이상이어야 합니다.");
                return;
            }

            if (barcode.isEmpty()) {
                showWarning("바코드는 필수입니다.");
                return;
            }

            if (stock < 0) {
                showWarning("재고는 0 이상이어야 합니다.");
                return;
            }

            Product product = Product.builder()
                    .productId(productId)
                    .productName(productName)
                    .price(price)
                    .barcode(barcode)
                    .expirationDate(expirationDate)
                    .stock(stock)
                    .category(category)
                    .build();

            productService.updateProduct(product);

            showMessage("'" + productName + "' 상품이 변경되었습니다.");

            // 상품 수정 후 전체 상품 목록 갱신
            refreshProductTable();

        } catch (NumberFormatException e) {
            showWarning("상품 ID, 가격, 재고는 숫자로 입력해주세요.");
        } catch (java.time.format.DateTimeParseException e) {
            showWarning("유통기한은 yyyy-MM-dd 형식으로 입력해주세요.");
        } catch (SQLException e) {
            showError("상품 수정 중 오류가 발생했습니다.", e);
        }
    }

    // 상품 삭제
    private void deleteProduct() {
        String input = JOptionPane.showInputDialog(
                this,
                "삭제할 상품 ID를 입력해주세요."
        );

        if (input == null) return;

        try {
            int productId = Integer.parseInt(input.trim());

            if (productId <= 0) {
                showWarning("상품 ID는 1 이상이어야 합니다.");
                return;
            }

            int result = JOptionPane.showConfirmDialog(
                    this,
                    productId + "번 상품을 삭제하시겠습니까?",
                    "상품 삭제",
                    JOptionPane.YES_NO_OPTION
            );

            if (result != JOptionPane.YES_OPTION) return;

            Product product = Product.builder()
                    .productId(productId)
                    .build();

            productService.deleteProduct(product);

            showMessage(productId + "번 상품이 삭제되었습니다.");

            // 상품 삭제 후 전체 상품 목록 갱신
            refreshProductTable();

        } catch (NumberFormatException e) {
            showWarning("상품 ID는 숫자로 입력해주세요.");
        } catch (SQLException e) {
            showError("상품 삭제 중 오류가 발생했습니다.", e);
        }
    }

    // 상품 전체 조회
    private void getProduct() {
        try {
            List<Product> productList = productService.getProduct();

            tableModel.setRowCount(0);

            for (Product p : productList) {
                addProductRow(p);
            }

            showTable("상품 전체 조회");

        } catch (SQLException e) {
            showError("상품 조회 중 오류가 발생했습니다.", e);
        }
    }

    // 상품명 검색
    private void searchProductByName() {
        String productName = JOptionPane.showInputDialog(
                this,
                "검색할 상품명을 입력해주세요."
        );

        if (productName == null) return;

        productName = productName.trim();

        if (productName.isEmpty()) {
            showWarning("검색어를 입력해주세요.");
            return;
        }

        try {
            List<Product> productList =
                    productService.searchProductByName(productName);

            tableModel.setRowCount(0);

            for (Product p : productList) {
                addProductRow(p);
            }

            if (productList.isEmpty()) {
                showMessage("검색 결과가 없습니다.");
            }

            showTable("상품명 검색 결과");

        } catch (SQLException e) {
            showError("상품명 검색 중 오류가 발생했습니다.", e);
        }
    }

    // 상품 ID 검색
    private void searchProductById() {
        String input = JOptionPane.showInputDialog(
                this,
                "검색할 상품 ID를 입력해주세요."
        );

        if (input == null) return;

        input = input.trim();

        if (input.isEmpty()) {
            showWarning("상품 ID를 입력해주세요.");
            return;
        }

        try {
            int productId = Integer.parseInt(input);

            if (productId <= 0) {
                showWarning("상품 ID는 1 이상이어야 합니다.");
                return;
            }

            List<Product> productList =
                    productService.searchProductById(productId);

            tableModel.setRowCount(0);

            for (Product p : productList) {
                addProductRow(p);
            }

            if (productList.isEmpty()) {
                showMessage("검색 결과가 없습니다.");
            }

            showTable("상품 ID 검색 결과");

        } catch (NumberFormatException e) {
            showWarning("상품 ID는 숫자로 입력해주세요.");
        } catch (SQLException e) {
            showError("상품 ID 검색 중 오류가 발생했습니다.", e);
        }
    }

    // 재고 부족 상품 조회
    private void searchProductByStock() {
        try {
            List<Product> productList =
                    productService.searchProductByStock();

            tableModel.setRowCount(0);

            for (Product p : productList) {
                addProductRow(p);
            }

            if (productList.isEmpty()) {
                showMessage("재고가 부족한 상품이 없습니다.");
            }

            showTable("재고 부족 상품");

        } catch (SQLException e) {
            showError("재고 부족 상품 조회 중 오류가 발생했습니다.", e);
        }
    }

    // 상품 테이블 행 추가
    private void addProductRow(Product p) {
        tableModel.addRow(new Object[]{
                p.getProductId(),
                p.getProductName(),
                p.getPrice(),
                p.getBarcode(),
                p.getExpirationDate(),
                p.getStock(),
                p.getCategory()
        });
    }

    // 상품 전체 목록 새로고침
    private void refreshProductTable() {
        try {
            List<Product> productList = productService.getProduct();

            tableModel.setRowCount(0);

            for (Product p : productList) {
                addProductRow(p);
            }

            showTable("상품 전체 조회");

        } catch (SQLException e) {
            showError("상품 목록 갱신 중 오류가 발생했습니다.", e);
        }
    }

    // 테이블 화면 표시
    private void showTable(String titleText) {
        contentPanel.removeAll();

        JLabel title = new JLabel(titleText);
        title.setFont(new Font("맑은 고딕", Font.BOLD, 20));

        contentPanel.add(title, BorderLayout.NORTH);
        contentPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        table.setRowHeight(28);

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // 경고 메시지
    private void showWarning(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "입력 오류",
                JOptionPane.WARNING_MESSAGE
        );
    }

    // 일반 메시지
    private void showMessage(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                "알림",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    // 오류 메시지
    private void showError(String message, Exception e) {
        JOptionPane.showMessageDialog(
                this,
                message + "\n" + e.getMessage(),
                "오류",
                JOptionPane.ERROR_MESSAGE
        );
    }
}
