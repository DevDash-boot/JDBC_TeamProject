package com.tenco.dao;

import com.tenco.dto.ProductDTO;
import com.tenco.util.util;

import java.sql.*;


public class ProductDAO {

    // 재고 차감
    public int outStock(Connection conn, int productId, int quantity) {
        String sql = "UPDATE product SET stock = stock - ? WHERE product_id = ? AND stock >=?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setInt(1, quantity);
            pstmt.setInt(2, productId);
            pstmt.setInt(3, quantity);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("재고 차감 중 오류 발생", e);
        }
    }


    public ProductDTO selectProductById(Connection conn, int productId) throws SQLException {
        String sql = "SELECT product_id, product_name, price, stock FROM product WHERE product_id = ?";

        ProductDTO product = null;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1,productId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    product = new ProductDTO();
                    product.setProductId(rs.getInt("product_id"));
                    product.setProductName(rs.getString("product_name"));
                    product.setPrice(rs.getInt("price"));
                    product.setStock(rs.getInt("stock"));
                }
            }
        }
        return product;
    }

}