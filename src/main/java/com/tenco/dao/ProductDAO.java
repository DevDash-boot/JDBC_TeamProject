package com.tenco.dao;

import com.tenco.dto.ProductDTO;
import java.sql.Connection;
import java.sql.SQLException;

public interface ProductDAO {
    // 재고 차감
    int outStock(Connection conn, int productId, int quantity);

    // 재고 복구 (주문 취소 시)
    public int inStock(Connection conn, int productId, int quantity) throws SQLException;

    // 단건 조회
    ProductDTO selectProductById(Connection conn, int productId) throws SQLException;
}