package com.tenco.View;

import com.tenco.Service.ProductService;
import com.tenco.dto.Product;
import com.tenco.util.util;

import java.io.Console;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

public class ProductView {

    private final ProductService service = new ProductService();
    private final Scanner sc = new Scanner(System.in, consoleCharset());

    private static Charset consoleCharset() {
        Console console = System.console();
        return console != null ? console.charset() : Charset.defaultCharset();
    }


    public void start() {
        System.out.println("=== 무인 편의점 상품 관리 시스템 ===");

        while (true) {
            printMenu();
            int choice = readInt("선택: ");

            try {
                switch (choice) {
                    case 1: addProduct(); break;
                    case 2: getProductName(); break;
                    case 3: updateProduct(); break;
                    case 4: deleteProduct(); break;
                    case 5: getProduct(); break;
                    case 6: searchProductByName(); break;
                    case 7: searchProductByBarcode(); break;
                    case 8: searchProductByStock(); break;
                    case 9:
                        System.out.println("프로그램을 종료합니다.");
                        util.close();
                        sc.close();
                        return;
                    default:
                        System.out.println("1~9 사이의 숫자를 입력하세요");
                }

            } catch (SQLException e) {
                System.out.println("오류: " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println("[메뉴]");
        System.out.println("1. 상품 추가");
        System.out.println("2. 상품 조회");
        System.out.println("3. 상품 수정");
        System.out.println("4. 상품 삭제");
        System.out.println("5. 상품 상세 조회");
        System.out.println("6. 상품명 검색");
        System.out.println("7. 바코드로 상품 조회");
        System.out.println("8. 재고 부족 상품 조회");
        System.out.println("9. 종료");
    }

    private void addProduct() throws SQLException {
        System.out.println("상품명: ");
        String productName = sc.nextLine().trim();
        if (productName.isEmpty()) {
            System.out.println("상품명은 필수입니다.");
            return;
        }

        System.out.println("가격: ");
        int price = sc.nextInt();
        sc.nextLine();
        if (price == 0 || price < 0) {
            System.out.println("가격은 필수입니다.");
            return;
        }

        System.out.println("바코드: ");
        String barcode = sc.nextLine().trim();
        if (barcode.isEmpty()) {
            System.out.println("바코드는 필수입니다.");
            return;
        }

        System.out.println("유통기한 (예시: xxxx-xx-xx): ");
        LocalDate expirationDate = LocalDate.parse(sc.nextLine().trim());

        System.out.println("재고: ");
        int stock = sc.nextInt();
        sc.nextLine();

        System.out.println("분류: ");
        String category = sc.nextLine();

        Product product = Product.builder()
                .productName(productName)
                .price(price)
                .barcode(barcode)
                .expirationDate(expirationDate)
                .stock(stock)
                .category(category)
                .build();
        service.addProduct(product);
        System.out.println("'" + productName + "' 상품이 추가되었습니다.");
    }

    private void getProductName() {
        List<Product> productList = service.getProductName();
        System.out.println("\n === 상품 목록 ===");
        if (productList.isEmpty()) {
            System.out.println("등록된 상품이 없습니다.");
        } else {
            System.out.println("--------------------------");
            for (Product p : productList) {
                System.out.printf("상품명: %s%n", p.getProductName());
            }
        }
    }

    private void updateProduct() throws SQLException {
        System.out.println("수정할 상품 ID: ");
        int productId = sc.nextInt();
        sc.nextLine();

        System.out.println("상품명: ");
        String productName = sc.nextLine().trim();
        if (productName.isEmpty()) {
            System.out.println("상품명은 필수입니다.");
            return;
        }

        System.out.println("가격: ");
        int price = sc.nextInt();
        sc.nextLine();
        if (price == 0 || price < 0) {
            System.out.println("가격은 필수입니다.");
            return;
        }

        System.out.println("바코드: ");
        String barcode = sc.nextLine().trim();
        if (barcode.isEmpty()) {
            System.out.println("바코드는 필수입니다.");
            return;
        }

        System.out.println("유통기한 (예시: xxxx-xx-xx): ");
        LocalDate expirationDate = LocalDate.parse(sc.nextLine().trim());

        System.out.println("재고: ");
        int stock = sc.nextInt();
        sc.nextLine();

        System.out.println("분류: ");
        String category = sc.nextLine();

        Product product = Product.builder()
                .productId(productId)
                .productName(productName)
                .price(price)
                .barcode(barcode)
                .expirationDate(expirationDate)
                .stock(stock)
                .category(category)
                .build();
        service.updateProduct(product);
        System.out.println("'" + productName + "' 상품이 변경되었습니다.");
    }

    private void deleteProduct() throws SQLException {
        System.out.println("삭제할 상품 ID: ");
        int productId = sc.nextInt();
        sc.nextLine();
        Product product = Product.builder()
                .productId(productId)
                .build();
        service.deleteProduct(product);
        System.out.println(productId + "번 상품이 삭제되었습니다.");
    }

    private void getProduct() {
        List<Product> productList = service.getProduct();
        System.out.println("\n === 상품 상세 목록 ===");
        if (productList.isEmpty()) {
            System.out.println("등록된 상품이 없습니다.");
        } else {
            System.out.println("--------------------------");
            for (Product p : productList) {
                System.out.println("ID: " + p.getProductId());
                System.out.println("상품명: " + p.getProductName());
                System.out.println("가격: " + p.getPrice());
                System.out.println("바코드: " + p.getBarcode());
                System.out.println("유통기한: " + p.getExpirationDate());
                System.out.println("재고: " + p.getStock());
                System.out.println("분류: " + p.getCategory());
                System.out.println("---------------------------");
            }
        }
    }

    private void searchProductByName() throws SQLException {
        System.out.println("검색 상품: ");
        String productName = sc.nextLine().trim();
        if (productName.isEmpty()) {
            System.out.println("검색어를 입력해주세요.");
            return;
        }

        List<Product> productList = service.searchProductByName(productName);
        System.out.println("\n=== 검색 결과 ===");
        if (productList.isEmpty()) {
            System.out.println("검색 결과가 없습니다.");
        } else {
            for (Product p : productList) {
                System.out.println("ID: " + p.getProductId());
                System.out.println("상품명: " + p.getProductName());
                System.out.println("가격: " + p.getPrice());
                System.out.println("바코드: " + p.getBarcode());
                System.out.println("유통기한: " + p.getExpirationDate());
                System.out.println("재고: " + p.getStock());
                System.out.println("분류: " + p.getCategory());
                System.out.println("---------------------------");
            }
        }
    }

    private void searchProductByBarcode() throws SQLException {
        System.out.println("상품 바코드: ");
        String productBarcode = sc.nextLine().trim();
        if (productBarcode.isEmpty()) {
            System.out.println("바코드를 입력해주세요.");
            return;
        }

        List<Product> productList = service.searchProductByBarcode(productBarcode);
        System.out.println("\n=== 검색 결과 ===");
        if (productList.isEmpty()) {
            System.out.println("검색 결과가 없습니다.");
        } else {
            for (Product p : productList) {
                System.out.println("ID: " + p.getProductId());
                System.out.println("상품명: " + p.getProductName());
                System.out.println("가격: " + p.getPrice());
                System.out.println("바코드: " + p.getBarcode());
                System.out.println("유통기한: " + p.getExpirationDate());
                System.out.println("재고: " + p.getStock());
                System.out.println("분류: " + p.getCategory());
                System.out.println("---------------------------");
            }
        }
    }

    private void searchProductByStock() throws SQLException {
        List<Product> productList = service.searchProductByStock();
        System.out.println("\n === 상품 상세 목록 ===");
        if (productList.isEmpty()) {
            System.out.println("재고가 부족한 상품이 없습니다.");
        } else {
            System.out.println("--------------------------");
            for (Product p : productList) {
                System.out.println("ID: " + p.getProductId());
                System.out.println("상품명: " + p.getProductName());
                System.out.println("가격: " + p.getPrice());
                System.out.println("바코드: " + p.getBarcode());
                System.out.println("유통기한: " + p.getExpirationDate());
                System.out.println("재고: " + p.getStock());
                System.out.println("분류: " + p.getCategory());
                System.out.println("---------------------------");
            }
        }
    }


    private int readInt(String prompt) {
        while (true) {
            System.out.println(prompt);
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("숫자를 입력해주세요.");
            }
        }
    }

}
