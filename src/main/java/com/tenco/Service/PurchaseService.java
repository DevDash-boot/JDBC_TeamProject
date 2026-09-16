package com.tenco.Service;

import com.tenco.dao.ProductDAO;
import com.tenco.dao.PurchaseDAO;
import com.tenco.dto.PurchaseDto;
import com.tenco.util.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;


public class PurchaseService {
    PurchaseDAO purchaseDAO = new PurchaseDAO();
    ProductDAO productDAO = new ProductDAO();

    // 기능 A. 발주상품 전체 조회.
    public List<PurchaseDto> getAllPurchases() throws SQLException {
        return purchaseDAO.getAllPurchases();
    }


    // 기능 B. 상품 ID로 발주 상품 단건 조회.
    public PurchaseDto existList(int id) throws SQLException {
        if (id <= 0 || purchaseDAO.existList(id) == null) {
            System.out.println("id가 " + id + "인 상품은 발주 목록에 없습니다.");
        }
        return purchaseDAO.existList(id);
    }


    // 기능 C. 발주 목록에 새로 추가 / 발주 신청. ---> 상품 테이블에 추가해야하므로 [ 발주신청 + 상품테이블의 추가기능] 을 하나의 트랜잭션으로.
    public void purcahseProduct(int id, int quantity) throws SQLException {
        if (id <= 0) {
            throw new SQLException("유효한 ID를 넣어주세요.");
        } else if (quantity <= 0 || 20 <= quantity) {
            throw new SQLException("발주 신청 수량은 한번에 최대 20개까지만 가능합니다.");
        }
        Connection conn = null;
        try {
            conn = util.getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작.

            purchaseDAO.purchaseProduct(id, quantity);
            productDAO.addAmount(id, quantity);
            conn.commit();

        } catch (SQLException e) {
            if (conn != null) conn.rollback(); // catch문에 왔다면 문제가 생긴것이므로 rollback.
            throw new SQLException(e.getMessage());
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }


    // 기능 D. 상품 ID로 발주목록에서 제거.
    public void deletePurchase(int id) throws SQLException {
        if (id <= 0) {
            throw new SQLException("유효한 ID를 입력해주세요.");
        }
        int i = purchaseDAO.deletePurchase(id);
        if (i <= 0) throw new SQLException("id가 " + id + "인 상품은 발주목록에 없습니다.");
        else System.out.println(i + "건이 발주목록에서 삭제 되었습니다. 발주취소 되었습니다. ---  id : " + id);
    }


    // 기능 E. 상품 ID로 발주 수량 차감. -- 만들기만 함
    // 트랜잭션 처리
    // 1. 발주 수정
    // 2. product 재고 변경
    public void substractPurchase(int id, int quantity) throws SQLException {
        if (id <= 0 || quantity <= 0) {
            throw new SQLException("유효한 ID와 수량을 입력해주세요.");
        }
        Connection conn = null;
        try {
            conn = util.getConnection();
            // 자동 커밋 해제(트랜잭션 시작)
            conn.setAutoCommit(false);

            // 발주 수량 차감
            int i = purchaseDAO.substractPurchase(conn, id, quantity);
            if (i <= 0) throw new
                    SQLException(" 유효한 ID를 입력해주세요. 입력되는 수량은 현재 발주된 상품의 수량을 초과할 수 없습니다. \n");
            // 상품 재고 증가
            int j = productDAO.inStock(conn, id, quantity);
            if(j == 0 ) throw new SQLException("상품 재고 증가 실패");

            // 전부 성공
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        }
    }
}
