package com.tenco;

import com.tenco.view.ProductView;
import com.tenco.dao.ProductDAO;

import java.sql.SQLException;

public class ProductTest {

    public static void main(String[] args) throws SQLException {


        ProductDAO productDAO = new ProductDAO();
        // 전체 상품 상세 조회
//        List<Product> productList = productDAO.getProduct();
//        for (int i = 0; i < productList.size(); i++) {
//            System.out.println(productList.get(i).toString());
//        }

        // -----------------------------------------------------------------------

        // 이름만 조회
//        List<Product> productList = productDAO.getProductName();
//        for (int i = 0; i < productList.size(); i++) {
//            System.out.println(productList.get(i).getProductName());
//        }

        // -----------------------------------------------------------------------

        // 상품 등록 테스트
//        ProductService productService = new ProductService();
//        Product product = Product.builder()
//                .productName("123")
//                .price(12000)
//                .barcode("123123")
//                .expirationDate(LocalDate.of(2026, 9, 14))
//                .stock(2 )
//                .category("num")
//                .build();
//        productService.addProduct(product);

        // -----------------------------------------------------------------------

        // 상품 수정 테스트
//        ProductService productService = new ProductService();
//        Product product = Product.builder()
//                .productId(12)
//                .productName("456")
//                .price(42000)
//                .barcode("123123")
//                .expirationDate(LocalDate.of(2026, 9, 14))
//                .stock(1)
//                .category("num")
//                .build();
//        productService.updateProduct(product);

        // -----------------------------------------------------------------------

        // 상품 삭제 테스트
//        ProductService productService = new ProductService();
//        Product product = Product.builder()
//                .productId(12)
//                        .build();
//        productService.deleteProduct(product);

        // -----------------------------------------------------------------------

        // 상품명 검색
//        Product product = new Product();
//        List<Product> productList = productDAO.searchProductByName("삼각김밥");
//        System.out.println("전체 조회된 row 수: " + productList.size());
//        System.out.println(productList.get(0).toString());

        // -----------------------------------------------------------------------

        // 바코드로 상품 조회
//        Product product = new Product();
//        List<Product> productList = productDAO.searchProductByBarcode("880100000001");
//        System.out.println("전체 조회된 row 수: " + productList.size());
//        System.out.println(productList.get(0).toString());


        // -----------------------------------------------------------------------

        // 재고 부족 상품 조회(재고 부족 기준 10개)
//        Product product = new Product();
//        List<Product> productList = productDAO.searchProductByStock();
//        System.out.println("전체 조회된 row 수: " + productList.size());
//        for (int i = 0; i < productList.size(); i++) {
//            System.out.println(productList.get(i).toString());
//        }

        // -----------------------------------------------------------------------

        // stock이 0이면 status 0
//        ProductService productService = new ProductService();
//        productService.searchProductByStock();

        ProductView productView = new ProductView();
        productView.start();

    }
}
