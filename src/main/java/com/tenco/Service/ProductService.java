package com.tenco.Service;

import com.tenco.dao.ProductDAO;
import com.tenco.dto.Product;

import java.sql.SQLException;
import java.util.List;

public class ProductService {

    private ProductDAO productDAO = new ProductDAO();

    // 1. 상품 추가
    // id 바코드 필수
    public void addProduct(Product product) throws SQLException {
        if (product.getProductName() == null || product.getProductName().trim().isEmpty() ||
                product.getBarcode() == null || product.getBarcode().trim().isEmpty() ||
                product.getPrice() == 0){
            throw new SQLException("상품명, 가격, 바코드는 필수 입력 항목입니다.");
        }
        productDAO.addProduct(product);
    }

    // 2. 상품 이름 조회
    public List<Product> getProductName() throws SQLException {
        return productDAO.getProductName();
    }

    // 3. 상품 수정
    public void updateProduct(Product product) throws SQLException {

        if (product.getProductName() == null || product.getProductName().trim().isEmpty()) {
            throw new SQLException("상품명은 필수 입력입니다.");
        }
        if (product.getPrice() <= 0) {
            throw new SQLException("가격은 0보다 커야합니다.");
        }
        if (product.getStock() < 0) {
            throw new SQLException("재고는 0보다 작으면 안 됩니다.");
        }

        int rows = productDAO.updateProduct(product);

        if (rows == 0) {
            throw new SQLException("존재하지 않는 상품입니다.");
        }
    }

    // 4. 상품 삭제
    public void deleteProduct(Product product) throws SQLException {

        if (product.getProductId() <= 0) {
            throw new SQLException("상품 번호가 올바르지 않습니다.");
        }

        productDAO.deleteProduct(product.getProductId());
    }

    // 5. 전체 상품 조회
    public List<Product> getProduct()throws SQLException {
        return productDAO.getProduct();
    }

    // 6. 상품명 검색
    public List<Product> searchProductByName(String productName) throws SQLException {
        if (productName == null || productName.trim().isEmpty()) {
            throw new SQLException("검색어를 입력해주세요");
        }
        return productDAO.searchProductByName(productName);
    }

    // 7. 아이디로 상품 조회
    public List<Product> searchProductById(int productId) throws SQLException {
        if (productId <= 0) {
            throw new SQLException("상품의 ID를 입력해주세요");
        }
        return productDAO.searchProductById(productId);
    }

    // 8. 재고 부족 상품 조회 << 재고 부족 기준: 10개)
    public List<Product> searchProductByStock() throws SQLException {
        return productDAO.searchProductByStock();
    }

    // 9. stock이 0이면 status false
    public void updateStatus() {
        productDAO.updateStatus();
    }

}
