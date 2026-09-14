package com.tenco;

import com.tenco.Service.PurchaseService;
import com.tenco.View.PurchaseView;
import com.tenco.dao.PurchaseDAO;

import java.sql.SQLException;

//TIP 코드를 <b>실행</b>하려면 <shortcut actionId="Run"/>을(를) 누르거나
// 에디터 여백에 있는 <icon src="AllIcons.Actions.Execute"/> 아이콘을 클릭하세요.
public class Main {
    public static void main(String[] args) throws SQLException {
        // 기능 A. 발주 가능상품 전체 조회 테스트.
//               new PurchaseDAO().getAllPurchases();

        // 기능 B. id 로 발주 목록에 존재하는지 판단.
//        new PurchaseService().existList(2222);

        // 기능 C. 발주 신청. + 발주 신청했으면 수량 , 총가격 조회.
//        new PurchaseDAO().purcahseProduct(2 , 22);

        // 기능 D. product_id로 발주 목록에서 제거(발주 취소)
//        new PurchaseDAO().deletePurchase(2);

//        new PurchaseService().purcahseProduct(2 , 1000);


        //  View 생성 후 Main에서 실행.
        new PurchaseView().purchaseView();
    }
}