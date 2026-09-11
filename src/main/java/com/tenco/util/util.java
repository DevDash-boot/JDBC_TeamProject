package com.tenco.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

//코드 수정 - [HikariCP] 커넥션 풀을 관리하는 클래스
public class util {
    private static final String URL = "jdbc:mysql://localhost:3306/convenience_store?serverTimezone=Asia/Seoul";
    private static final String DB_USER = System.getenv("DB_USER");
    private static final String DB_PASSWORD = System.getenv("DB_PASSWORD");

    private static final HikariDataSource dataSource;

    // static 블록은 이 클래스가 처음 사용되는 순간 딱 한번 실행된다.
    // 즉, 첫 getConnection() 호출 시점에 커넥션 풀이 만들어지고, 그 뒤로는 재사용한다.
    static{
        HikariConfig config = new HikariConfig();

        // 1. 기본 연결 정보 설정
        config.setJdbcUrl(URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);

        // 2. 커넥션 풀 크기 설정
        config.setMaximumPoolSize(10); // 동시에 최대 10개 연결 유지(스프링 부트 기본값)
        config.setMinimumIdle(5);   // 요청이 없어도 최소 5개 준비 상태로 유지

        // 3. 풀이 가득 찼을 때 빈 연결을 기다리는 최대 시간(밀리초 단위)
        config.setConnectionTimeout(3000);

        // 4. 풀 생성(이 시점에 실제 DB 연결 객체 n개가 설정된다.)
        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    // 프로그램 종료 시 풀 전체를 정리하는 기능
    public static void close(){
        if (!dataSource.isClosed()){
            dataSource.close();
        }
    }
}