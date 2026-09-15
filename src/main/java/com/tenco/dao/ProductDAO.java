package com.tenco.dao;

import com.tenco.dto.ProductDTO;
import java.sql.Connection;
import java.sql.SQLException;

public interface ProductDAO {
    // 재고 차감
    int outStock(Connection conn, int productId, int quantity);

    // 단건 조회
    ProductDTO selectProductById(Connection conn, int productId) throws SQLException;
}