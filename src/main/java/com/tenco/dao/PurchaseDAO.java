package com.tenco.dao;

import com.mysql.cj.protocol.Resultset;
import com.tenco.dto.PurchaseDto;
import com.tenco.util.Util;

import javax.xml.transform.Result;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PurchaseDAO {
        private boolean hasProduct = true; // 발주 목록에 이미 상품이 존재하는지 판단할 변수.

    // 기능 A. 발주 가능한 상품 전체 조회.
    public List<PurchaseDto> getAllPurchases() {
        List<PurchaseDto> purchaseList = new ArrayList<>();
        try (Connection conn = Util.getConnection()) {
            String existSql = """
                    select p.product_id ,  pr.product_name  , p.quantity , p.unit_price
                    from purchase p join product pr on p.product_id = pr.product_id;                
                    """;
            try (PreparedStatement existPstmt = conn.prepareStatement(existSql)) {
                try (ResultSet rs = existPstmt.executeQuery()) {
                    while (rs.next()) {
                        purchaseList.add(PurchaseDto.builder()
                                .product_id(rs.getInt("product_id"))
                                .name(rs.getString("product_name"))
                                .qauntity(rs.getInt("quantity"))
                                .unitPrice(rs.getInt("unit_price"))
                                .build());
                    }
                    for (PurchaseDto p : purchaseList) System.out.println(p);
                    return purchaseList;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    // 기능 B. id로 상품이 발주 목록에 있는지.
    public PurchaseDto existList(int id) throws SQLException {
        try (Connection conn = Util.getConnection()) {
            PurchaseDto purchaseDto = null;
            String alreadySql = """                 
                    select p.* , pr.product_name  
                    from purchase p join product pr on p.product_id = pr.product_id
                    where p.product_id = ?
                    """;
            try (PreparedStatement alreadyPstmt = conn.prepareStatement(alreadySql)) {
                alreadyPstmt.setInt(1, id);
                try (ResultSet rs = alreadyPstmt.executeQuery()) {
                    if (rs.next()) {
                        purchaseDto = PurchaseDto.builder()
                                .purchaseId(rs.getInt("purchase_id"))
                                .product_id(rs.getInt("product_id"))
                                .name(rs.getString("product_name"))
                                .qauntity(rs.getInt("quantity"))
                                .unitPrice(rs.getInt("unit_price"))
                                .totlaPrice(rs.getInt("total_price"))
                                .build();
                        System.out.println(purchaseDto.toString());
                        hasProduct = true;
                    }
                    return purchaseDto;
                }
            }
        } catch (SQLException e) {
            throw new SQLException(e);
        }
    }


    // 기능 C. 이 상품을 몇개 발주할지 + 총 발주 가격? + 실제 존재하는 발주 상품인지. -- 발주 신청.
    public void purcahseProduct(int id, int quantity) throws SQLException {
        int price = 0;

        // 실제 발주 가능한 상품인지 -->  몇개 발주할지 (발주,총 발주 가격 수정)
        // 새로 발주하는 상품이라면 추가(insert) , 기존에 있던 발주상품이라면 수정(update)

        try (Connection conn = Util.getConnection()) {
            // 1 - 1. 발주 목록에 없는 새로 발주할 상품이라면. insert로 purchase테이블에 등록.
            // 2 - 1. product테이블의 price를 가져와야하므로 select로 먼저 price저장.
            if(!hasProduct) {
                    String priceSql = """
                        select price from product 
                        where product_id = ?
                        """;
                    try (PreparedStatement pricePstmt = conn.prepareStatement(priceSql)) {
                        pricePstmt.setInt(1 , id);
                        try (ResultSet rs = pricePstmt.executeQuery()) {
                            if (rs.next()) price = rs.getInt("price");
                        }
                    }

                    // 2 - 2. 위에서 price 가져왔으니 활용.
                    String newPurchaseSql = """
                            insert into purchase(product_id , quantity , unit_price , total_price)
                            values(? , ? , ? , ?);
                            """;
                    try (PreparedStatement newPurchasePstmt = conn.prepareStatement(newPurchaseSql)) {
                        newPurchasePstmt.setInt(1 , id);
                        newPurchasePstmt.setInt(2 , quantity);
                        newPurchasePstmt.setInt(3 , price);
                        newPurchasePstmt.setInt(4 , quantity * price);
                        int i = newPurchasePstmt.executeUpdate();
                        if (i <= 0) throw new SQLException("발주에 실패하였습니다.");
                        else System.out.println(i + "건이 새로 발주 목록에 추가되었습니다.");
                    }
            }


            // 1 - 2. 기존에 있던 발주상품이라면 수량만 추가로 발주.
            else {
                String purchaseSql = """
                        update purchase set quantity = quantity + ?  , total_price = quantity * unit_price 
                        where product_id = ?
                        """;
                try (PreparedStatement purchasePstmt = conn.prepareStatement(purchaseSql)) {
                    purchasePstmt.setInt(1, quantity);
                    purchasePstmt.setInt(2, id);
                    int i = purchasePstmt.executeUpdate();

                    if (i <= 0) throw new SQLException("발주에 실패하였습니다.");
                    else System.out.println(i + "건이 발주에 성공하였습니다.");
                }
            }

                // 3. 발주 성공했으면 다시 화면에 총 발주가격 , 발주한 상품 , 수량 다시 보여주기.
                String totalSql = """
                                select p.product_id , pr.product_name ,  p.quantity , p.total_price  
                                from purchase p join product pr on p.product_id = pr.product_id
                                where pr.product_id = ?                           
                                """;
                try (PreparedStatement totalPstmt = conn.prepareCall(totalSql)) {
                    totalPstmt.setInt(1, id);
                    ResultSet rs = totalPstmt.executeQuery();
                    if (rs.next()) System.out.printf("발주 상품ID : %d , 발주 상품명 : %s , 발주 상품수량 : %d , 발주 상품 총 가격 : %d",
                            rs.getInt("product_id"), rs.getString("product_name"),
                            rs.getInt("quantity"), rs.getInt("total_price"));
                }

    } catch (SQLException e) {
            throw new SQLException(e);
        }
    }
}



