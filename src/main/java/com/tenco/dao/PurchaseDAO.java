package com.tenco.dao;

import com.tenco.dto.PurchaseDto;
import com.tenco.util.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PurchaseDAO {

    // 기능 A. 발주 가능한 상품 전체 조회.
    public List<PurchaseDto> getAllPurchases() throws SQLException {
        List<PurchaseDto> purchaseList = new ArrayList<>();
        try (Connection conn = util.getConnection()) {
            String existSql = """
                    select p.product_id ,  pr.product_name  , p.quantity , p.unit_price , p.total_price
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
                                .totlaPrice(rs.getInt("total_price"))
                                .build());
                    }
//                    for (PurchaseDto p : purchaseList) System.out.println(p);
                    return purchaseList;
                }
            }
        }
    }


    // 기능 B. id로 상품이 발주 목록에 있는지.
    public PurchaseDto existList(int id) throws SQLException {
        try (Connection conn = util.getConnection()) {
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
                                .product_id(rs.getInt("product_id"))
                                .name(rs.getString("product_name"))
                                .qauntity(rs.getInt("quantity"))
                                .unitPrice(rs.getInt("unit_price"))
                                .totlaPrice(rs.getInt("total_price"))
                                .build();
                    }
                    return purchaseDto;
                }
            }
        }
    }


    // 기능 C. 이 상품을 몇개 발주할지 + 총 발주 가격? + 실제 존재하는 발주 상품인지. -- 발주 신청.
    public void purchaseProduct(int id, int quantity) throws SQLException {
        int price = 0;
        PurchaseDto purchaseDto = existList(id); // 실제 발주 가능한 상품인지 -->  몇개 발주할지 (발주,총 발주 가격 수정)
        // 새로 발주하는 상품이라면 추가(insert) , 기존에 있던 발주상품이라면 수정(update)

        try (Connection conn = util.getConnection()) {
            // 1 - 1. 발주 목록에 없는 새로 발주할 상품이라면. insert로 purchase테이블에 등록.

            // 2 - 1. product테이블의 price를 가져와야하므로 select로 먼저 price저장.
            if(purchaseDto == null) {
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

                    String checkproductSql = """
                            select product_id from product
                            where product_id = ?
                            """;

                    try (PreparedStatement checkPstmt = conn.prepareStatement(checkproductSql)) {
                        checkPstmt.setInt(1 , id);
                        try (ResultSet rs = checkPstmt.executeQuery()) {
                           if(!rs.next()) throw new SQLException("상품이 아예 존재하지 않습니다.");
                        }
                    }

                    try (PreparedStatement newPurchasePstmt = conn.prepareStatement(newPurchaseSql)) {
                        newPurchasePstmt.setInt(1 , id);
                        newPurchasePstmt.setInt(2 , quantity);
                        newPurchasePstmt.setInt(3 , price);
                        newPurchasePstmt.setInt(4 , quantity * price);
                        int i = newPurchasePstmt.executeUpdate();
                        if(i > 0) System.out.println(i + "건이 새로 발주 목록에 추가되었습니다. ---  id : " + id);
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
                    if (rs.next()) {
                        System.out.printf("발주 상품ID : %d , 발주 상품명 : %s , 발주 상품수량 : %d , 발주 상품 총 가격 : %d\n",
                                rs.getInt("product_id"), rs.getString("product_name"),
                                rs.getInt("quantity"), rs.getInt("total_price"));
                    }
                }
    }
    }


    // 기능 D. id로 발주 목록에서 제거.(발주 취소)
    public int deletePurchase(int id) throws SQLException {
        try (Connection conn = util.getConnection()) {
            String deleteSql = """
                    delete from purchase
                    where product_id = ?
                    """;

            try (PreparedStatement deletePstmt = conn.prepareStatement(deleteSql)) {
                    deletePstmt.setInt(1 , id);
                return deletePstmt.executeUpdate();
            }
        }
    }


    // 기능 E. id로 발주 수량 차감. -- 만들기만 함.
    public int substractPurchase(int id , int quantity) throws SQLException {
        try (Connection conn = util.getConnection()) {
            String substractSql = """
                    update purchase set quantity = quantity - ?
                    where product_id = ? and quantity - ? >= 0
                    """;

            try (PreparedStatement substractPstmt = conn.prepareStatement(substractSql)) {
                substractPstmt.setInt(1 , quantity);
                substractPstmt.setInt(2 , id);
                substractPstmt.setInt(3 , quantity);

                return substractPstmt.executeUpdate();
            }
        }
    }
}



