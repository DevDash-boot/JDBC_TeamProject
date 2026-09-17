package com.tenco.Service;

import com.tenco.dao.StoreInfoDAO;
import com.tenco.dto.StoreInfo;

import java.util.List;

public class StoreInfoService {
    private final StoreInfoDAO storeInfoDAO = new StoreInfoDAO();
    // 매장 전체 조회
    public List<StoreInfo> allStoreInfo(){
        return storeInfoDAO.allStoreInfo();
    }

    // 선택 매장 조회
    public List<StoreInfo> LocationStore(String location){
        if(location == null || location.trim().isEmpty()){
            System.out.println("찾으실 매장 위치를 입력해주세요.");
        }
        return storeInfoDAO.LocationStore(location);
    }
}