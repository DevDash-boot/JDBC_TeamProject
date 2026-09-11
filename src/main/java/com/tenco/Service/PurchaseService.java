package com.tenco.Service;

import com.tenco.dao.PurchaseDAO;
import com.tenco.dto.PurchaseDto;

import java.util.ArrayList;
import java.util.List;

// getAllPurchases List
// existList boolean
// purcahseProduct  void
public class PurchaseService {
    PurchaseDAO purchaseDAO = new PurchaseDAO();

    // 발주상품 전체 조회.
    public List<PurchaseDto> getAllPurchases() {
        return purchaseDAO.getAllPurchases();
    }

    // ID로 발주 상품 단건 조회.




}
