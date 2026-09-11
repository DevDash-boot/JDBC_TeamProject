package com.tenco;

import com.tenco.dao.ProductDAO;
import com.tenco.dto.Product;

import java.util.List;

//TIP 코드를 <b>실행</b>하려면 <shortcut actionId="Run"/>을(를) 누르거나
// 에디터 여백에 있는 <icon src="AllIcons.Actions.Execute"/> 아이콘을 클릭하세요.
public class Main {
    public static void main(String[] args) {

        ProductDAO productDAO = new ProductDAO();
        // 전체 상품 상세 조회
//        List<Product> productList = productDAO.getProduct();
//        for (int i = 0; i < productList.size(); i++) {
//            System.out.println(productList.get(i).toString());
//        }

        // 이름만 조회
        List<Product> productList = productDAO.getProductName();
        for (int i = 0; i < productList.size(); i++) {
            System.out.println(productList.get(i));
        }


    }
}