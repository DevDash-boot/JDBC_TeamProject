package com.tenco.dao;

import com.tenco.dto.Product;
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
                    select p.purchase_id , p.product_id ,  pr.product_name  , p.quantity , p.unit_price , p.total_price
                    from purchase p join product pr on p.product_id = pr.product_id;                
                    """;
            try (PreparedStatement existPstmt = conn.prepareStatement(existSql)) {
                try (ResultSet rs = existPstmt.executeQuery()) {
                    while (rs.next()) {
                        purchaseList.add(PurchaseDto.builder()
                                .purchaseId(rs.getInt("purchase_id"))
                                .productId(rs.getInt("product_id"))
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


    // 기능 B. 발주id로 상품이 발주 목록에 있는지.
    public PurchaseDto existList(int id) throws SQLException {
        try (Connection conn = util.getConnection()) {
            PurchaseDto purchaseDto = null;
            String alreadySql = """                 
                    select p.* , pr.product_name  
                    from purchase p join product pr on p.product_id = pr.product_id
                    where p.purchase_id = ?
                    """;

            // 상품 테이블에도 실제 존재하는 상품인지 확인.
            Product product = new ProductDAO().selectProductById(conn , id);
            if (product == null) throw new SQLException("ID가 " + id + "인 상품은 아예 존재하지 않습니다.");

            try (PreparedStatement alreadyPstmt = conn.prepareStatement(alreadySql)) {
                alreadyPstmt.setInt(1, id);
                try (ResultSet rs = alreadyPstmt.executeQuery()) {
                    if (rs.next()) {
                        purchaseDto = PurchaseDto.builder()
                                .purchaseId(rs.getInt("purchase_id"))
                                .productId(rs.getInt("product_id"))
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


    // 기능 C. 이 상품을 몇개 발주할지 + 총 발주 가격? + 실제 존재하는 발주 상품인지. -- 발주 신청. --> 즉시, 상품테이블의 stock에 추가.
    public int purchaseProduct(Connection conn , int id, int quantity) throws SQLException {
        int price = 0;
            int i = 0;
            // 1 - 1. 발주 목록에 없는 새로 발주할 상품이라면. insert로 purchase테이블에 등록.

            // 2 - 1. product테이블의 price를 가져와야하므로 select로 먼저 price저장.
            String priceSql = """
                        select price from product 
                        where product_id = ?
                        """;
            try (PreparedStatement pricePstmt = conn.prepareStatement(priceSql)) {
                pricePstmt.setInt(1 , id);
                try (ResultSet rs = pricePstmt.executeQuery()) {
                    if (rs.next()) price = rs.getInt("price");
                    else throw new SQLException("ID가 " + id + "인 상품은 아예 존재하지 않습니다.");
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
                 i = newPurchasePstmt.executeUpdate();
            }

            // 3. 발주 성공했으면 다시 화면에 총 발주가격 , 발주한 상품 , 수량 다시 보여주기.
            String totalSql = """
                    select p.product_id , pr.product_name ,  p.quantity , p.total_price  
                    from purchase p join product pr on p.product_id = pr.product_id
                    where pr.product_id = ?                           
                    """;
            try (PreparedStatement totalPstmt = conn.prepareStatement(totalSql)) {
                totalPstmt.setInt(1, id);
                ResultSet rs = totalPstmt.executeQuery();
                if (rs.next()) {
                    System.out.printf("발주 상품ID : %d , 발주 상품명 : %s , 발주 상품수량 : %d , 발주 상품 총 가격 : %d\n",
                            rs.getInt("product_id"), rs.getString("product_name"),
                            rs.getInt("quantity"), rs.getInt("total_price"));
                }
                return i;
            }
        }



    // 기능 D. purchaseId로 발주 목록에서 제거.(발주 취소) --> 상품테이블의 stock에서 차감.
    public PurchaseDto deletePurchase(Connection conn , int id) throws SQLException {

            PurchaseDto purchaseDto = new PurchaseDto();
            String  existPurchaseSql = """
                    select purchase_id , quantity , product_id from purchase
                    where purchase_id = ?
                    """;
            String deleteSql = """
                    delete from purchase
                    where purchase_id = ?
                    """;


            try (PreparedStatement existPstmt = conn.prepareStatement(existPurchaseSql)) {
                existPstmt.setInt(1 , id);
                try (ResultSet rs = existPstmt.executeQuery()) {
                    if(!rs.next()) throw new SQLException();

                    purchaseDto.setQauntity(rs.getInt("quantity"));
                    purchaseDto.setProductId(rs.getInt("product_id"));
                }
            }

            try (PreparedStatement deletePstmt = conn.prepareStatement(deleteSql)) {
                deletePstmt.setInt(1 , id);
                deletePstmt.executeUpdate();
                return purchaseDto;
            }
        }



    // 기능 E. 발주 id로 발주 수량 차감. -- 만들기만 함.
    public int[] substractPurchase(Connection conn , int id , int quantity) throws SQLException {
            int[] i = new int[2];
            int price = 0;
            String substractSql = """
                    update purchase set quantity = quantity - ? , total_price = total_price - ?
                    where purchase_id = ? and quantity - ? >= 0
                    """;
            String  existPurchaseSql = """
                    select purchase_id , product_id , unit_price from purchase
                    where purchase_id = ?
                    """;

        try (PreparedStatement existPstmt = conn.prepareStatement(existPurchaseSql)) {
            existPstmt.setInt(1 , id);
            try (ResultSet rs = existPstmt.executeQuery()) {
                if(!rs.next()) throw new SQLException("ID : " + id + "는 목록에 존재하지 않습니다.");
                price = rs.getInt("unit_price");
                i[0] = rs.getInt("product_id");
            }
        }

            try (PreparedStatement substractPstmt = conn.prepareStatement(substractSql)) {
                substractPstmt.setInt(1 , quantity);
                substractPstmt.setInt(2 , price * quantity);
                substractPstmt.setInt(3 , id);
                substractPstmt.setInt(4 , quantity);
                i[1] = substractPstmt.executeUpdate();
                return i;
            }
        }
    }




