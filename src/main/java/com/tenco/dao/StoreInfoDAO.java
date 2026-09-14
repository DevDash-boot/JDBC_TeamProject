package com.tenco.dao;

import com.tenco.dto.StoreInfo;
import com.tenco.util.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class StoreInfoDAO {

    // 매점 전체 조회
    public List<StoreInfo> allStoreInfo(){
        List<StoreInfo> storeInfoList = new ArrayList<>();
        String sql = """
                SELECT * FROM store;
                """;
        try (Connection conn = util.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = pstmt.executeQuery()) {
                    printStore(rs, storeInfoList);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return storeInfoList;
    }

    // 매점 지역별 조회
    public List<StoreInfo> LocationStore(String location){
        List<StoreInfo> storeInfoList = new ArrayList<>();
        String sql = """
                SELECT * FROM store
                WHERE location = ? ;
                """;
        try (Connection conn = util.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, location);
                try (ResultSet rs = pstmt.executeQuery()) {
                    printStore(rs, storeInfoList);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return storeInfoList;
    }

    private static void printStore(ResultSet rs, List<StoreInfo> storeInfoList) throws SQLException {
        while(rs.next()){
            storeInfoList.add(StoreInfo.builder()
                    .storeId(rs.getInt("store_id"))
                    .storeName(rs.getString("store_name"))
                    .openTime(rs.getTime("open_time").toLocalTime())
                    .closeTime(rs.getTime("close_time").toLocalTime())
                    .storeTell(rs.getString("store_tell"))
                    .Location(rs.getString("location"))
                    .build());
        }
    }
}