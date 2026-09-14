package com.tenco.view;

import com.tenco.Service.StoreInfoService;
import com.tenco.dto.StoreInfo;
import com.tenco.util.util;

import java.util.List;
import java.util.Scanner;

public class StoreInfoView {
    private final StoreInfoService storeInfoService = new StoreInfoService();
    private final Scanner scanner = new Scanner(System.in);

    public void start() {
        System.out.println("===== 매장 정보 =====");
        while (true) {
            System.out.println("\n1. 매장 전체 정보");
            System.out.println("2. 매장 위치별 정보");
            int choice = readInt("\n선택 : ");
            try {
                switch (choice) {
                    case 1:
                        allStoreInfo();
                        break;
                    case 2:
                        LocationStore();
                        break;
                    case 0:
                        System.out.println("프로그램을 종료합니다.");
                        ;
                        util.close();
                        scanner.close();
                        return;
                    default:
                        System.out.println("0~2 사이의 숫자를 입력하세요.");
                }
            } catch (Exception e) {
                System.out.println("오류: " + e.getMessage());
            }
        }
    }

    private void allStoreInfo() {
        System.out.println("[전체 매장 정보]");
        List<StoreInfo> storeInfoList = storeInfoService.allStoreInfo();
        System.out.println("번호|매장이름|오픈시간| 마감시간 | 전화번호 | 위치");
        System.out.println("────────────────────────────────────────────────────");
        for (StoreInfo s : storeInfoList) {
            System.out.printf("%d | %s | %s | %s | %s | %s\n",
                    s.getStoreId(), s.getStoreName(), s.getOpenTime(),
                    s.getCloseTime(), s.getStoreTell(), s.getLocation());
        }
    }

    private void LocationStore() {
        System.out.print("찾을 매장 위치: ");
        String location = scanner.nextLine();
        List<StoreInfo> storeInfoList = storeInfoService.LocationStore(location);
        System.out.println("번호|매장이름|오픈시간| 마감시간 | 전화번호 | 위치");
        System.out.println("────────────────────────────────────────────────────");
        for (StoreInfo s : storeInfoList) {
            System.out.printf("%d | %s | %s | %s | %s | %s\n",
                    s.getStoreId(), s.getStoreName(), s.getOpenTime(),
                    s.getCloseTime(), s.getStoreTell(), s.getLocation());
        }
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("숫자를 입력해주세요.");
            }
        }
    }
}