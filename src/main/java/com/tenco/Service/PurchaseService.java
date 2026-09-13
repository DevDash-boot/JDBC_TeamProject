package com.tenco.Service;

import com.tenco.dao.PurchaseDAO;
import com.tenco.dto.PurchaseDto;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// getAllPurchases List
// existList boolean
// purcahseProduct  void
public class PurchaseService {
    PurchaseDAO purchaseDAO = new PurchaseDAO();

    // 기능 A. 발주상품 전체 조회.
    public List<PurchaseDto> getAllPurchases() throws SQLException {
        return purchaseDAO.getAllPurchases();
    }

    // 기능 B. 상품 ID로 발주 상품 단건 조회.


    // 기능 C. 발주 목록에 새로 추가 / 발주 신청.


    // 기능 D. 상품 ID로 발주목록에서 제거.



}
