package com.tenco.dao;

import com.tenco.dto.Product;
import com.tenco.util.util;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 상품번호, 상품명, 가격, 바코드, 유통기한, 재고, 카테고리
 * <p>
 * CRUD
 * 1. 상품 등록
 * 2. 상품 조회
 * 3. 상품 수정
 * 4. 상품 삭제
 * ----------------------
 * 5. 상품 상세 조회
 * 6. 상품명으로 검색
 * ----------------------
 * 7. 바코드로 상품 조회
 * 8. 재고 부족 상품 조회
 * 9. 재고가 0이면 상태 = false
 */

public class ProductDAO {


    // 1. 상품 등록
    public int addProduct(Product product) {
        int rows = 0;

        String sql = """
                INSERT INTO product (product_name, price, barcode, expiration_date, stock, category)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = util.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, product.getProductName());
                pstmt.setInt(2, product.getPrice());
                pstmt.setString(3, product.getBarcode());
                pstmt.setDate(4, Date.valueOf(product.getExpirationDate()));
                pstmt.setInt(5, product.getStock());
                pstmt.setString(6, product.getCategory());

                rows = pstmt.executeUpdate();
            }
            System.out.println(rows + "행이 추가되었습니다.");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    // 2. 상품 조회
    public List<Product> getProductName() {
        List<Product> productList = new ArrayList<>();

        String sql = """
                SELECT product_name FROM product
                """;

        try (Connection conn = util.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = pstmt.executeQuery()) {

                    while (rs.next()) {
                        Product product = new Product();
                        product.setProductName(rs.getString("product_name"));
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return productList;
    }

    // 3. 상품 수정
    public int updateProduct(Product product) {
        int rows = 0;

        String sql = """
                UPDATE product
                SET product_name = ?, price = ?, stock = ?, category = ? 
                WHERE product_id = ?
                """;

        try (Connection conn = util.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, product.getProductName());
                pstmt.setInt(2, product.getPrice());
                pstmt.setInt(3, product.getStock());
                pstmt.setString(4, product.getCategory());
                pstmt.setInt(5, product.getProductId());

                rows = pstmt.executeUpdate();
                System.out.println(rows + "행이 수정되었습니다.");
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }


    // 4. 상품 삭제
    public int deleteProduct(int productId) {
        int rows = 0;

        String sql = """
                DELETE FROM product
                WHERE product_id = ?
                """;

        try (Connection conn = util.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, productId);

                rows = pstmt.executeUpdate();
                System.out.println(rows + "행이 삭제되었습니다.");
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return 0;
    }

    // 5. 상품 상세 조회
    public List<Product> getProduct() {
        List<Product> productList = new ArrayList<>();

        String sql = """
                SELECT * FROM product
                """;

        try (Connection conn = util.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = pstmt.executeQuery()) {

                    while (rs.next()) {
                        productList.add(createProduct(rs));
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return productList;
    }

    // 6. 상품명을 검색
    public List<Product> searchProductByName(String productName) {
        List<Product> productList = new ArrayList<>();

        String sql = """
                SELECT * FROM product WHERE product_name LIKE ?
                """;

        try (Connection conn = util.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, "%" + productName + "%");
                try (ResultSet rs = pstmt.executeQuery()) {

                    while (rs.next()) {
                        productList.add(createProduct(rs));
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return productList;
    }

    // 7. 바코드로 상품 조회
    public List<Product> searchProductByBarcode(String productBarcode) {
        List<Product> productList = new ArrayList<>();

        String sql = """
                SELECT * FROM product WHERE barcode LIKE ?
                """;

        try (Connection conn = util.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, "%" + productBarcode + "%");
                try (ResultSet rs = pstmt.executeQuery()) {

                    while (rs.next()) {
                        productList.add(createProduct(rs));
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return productList;
    }

    // 8. 재고 부족 상품 조회 << 재고 부족 기준: 10개)
    public List<Product> searchProductByStock() {
        List<Product> productList = new ArrayList<>();

        String sql = """
                SELECT * FROM product WHERE stock <= 10
                """;

        try (Connection conn = util.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = pstmt.executeQuery()) {

                    while (rs.next()) {
                        productList.add(createProduct(rs));
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return productList;
    }

    // 9. 재고가 0이면 상태 = false => UPDATE?
    public int updateStatus() {
        int rows = 0;

        String sql = """
                UPDATE product SET status = 0 WHERE stock = 0
                """;

        try (Connection conn = util.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    rows = pstmt.executeUpdate();
                    System.out.println(rows + "행이 수정되었습니다.");
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }


    private Product createProduct(ResultSet rs) throws SQLException {
        Product product = Product.builder()
                .productId(rs.getInt("product_id"))
                .productName(rs.getString("product_name"))
                .price(rs.getInt("price"))
                .barcode(rs.getString("barcode"))
                .expirationDate(rs.getDate("expiration_date").toLocalDate())
                .stock(rs.getInt("stock"))
                .category(rs.getString("category"))
                .build();

        return product;
    }


}
