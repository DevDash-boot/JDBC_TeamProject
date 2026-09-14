package com.tenco.view;

import com.tenco.Service.PurchaseService;
import com.tenco.dto.PurchaseDto;

import java.sql.SQLException;
import java.util.Scanner;

public class PurchaseView {
    PurchaseService purchaseService = new PurchaseService();
    Scanner scanner = new Scanner(System.in);

    // TODO 관리자 받아서 관리자 이름 보이게
    public void purchaseView() throws SQLException {
        try {

            while (true) {
                System.out.println("------------------ 관리자 : " + "------------------");
                System.out.println("====== 발주/입고 관리기능 ======.");
                System.out.print("1.발주 목록 전체 조회 \t2.상품 ID로 발주 조회.\t3.발주 신청\t4.발주목록에서 제거(발주 취소)\t" +
                        " 5.발주 수량 차감(발주 수정) \t6.메인 화면\n입력 : ");
                int menu = scanner.nextInt();

                switch (menu) {
                    case 1:
                        purchaseAllView();
                        break;

                    case 2:
                        System.out.print("조회할 상품 ID를 입력해주세요 : ");
                        int selectId = scanner.nextInt();
                        purchaseSingleView(selectId);
                        break;

                    case 3:
                        System.out.print("발주 신청할 상품 ID : ");
                        int addId = scanner.nextInt();
                        System.out.print("발주할 수랑 (최대 20개) : ");
                        int quantity = scanner.nextInt();
                        purchaseService.purcahseProduct(addId , quantity);
                        break;

                    case 4:
                        System.out.print("발주 목록에서 제거할 상품 ID : ");
                        int deleteId = scanner.nextInt();
                        purchaseService.deletePurchase(deleteId);
                        break;

                    case 5:
                        System.out.print("차감시킬 상품 ID : ");
                        int subId = scanner.nextInt();
                        System.out.print("차감시킬 수량 : ");
                        int amount = scanner.nextInt();
                        purchaseService.substractPurchase(subId , amount);
                        break;

                    case 6:
                        // TODO 메인화면 view 메서드.
                        break;

                    default:
                        System.out.println("메뉴는 1 ~ 5번에서 골라주세요.");
                }
            }
        }catch(SQLException e){
            throw new SQLException(e);
        }
        }

        // 전체조회를 사용자가 보기 편하게.
        public void purchaseAllView() throws SQLException {
            for (PurchaseDto p : purchaseService.getAllPurchases()) {
                System.out.printf("상품 ID : %d , 상품명 : %s , 발주 수량 : %d , 개당 상품가격 : %d , 총 발주가격 : %d\n" , p.getProduct_id() ,
                        p.getName() , p.getQauntity() , p.getUnitPrice() , p.getTotlaPrice());
                System.out.println();
            }
        }

     // 단건 조회를 사용자가 보기 편하게.
    public void purchaseSingleView(int id) throws SQLException {
        PurchaseDto p = purchaseService.existList(id);
            System.out.printf("상품 ID : %d , 상품명 : %s , 발주 수량 : %d , 개당 상품가격 : %d , 총 발주가격 : %d\n" , p.getProduct_id() ,
                    p.getName() , p.getQauntity() , p.getUnitPrice() , p.getTotlaPrice());
        }
    }


