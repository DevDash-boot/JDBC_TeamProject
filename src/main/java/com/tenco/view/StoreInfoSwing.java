package com.tenco.view;

import com.tenco.Service.StoreInfoService;
import com.tenco.dto.StoreInfo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class StoreInfoSwing extends JPanel {
    private final StoreInfoService storeInfoService = new StoreInfoService();
    private final DefaultTableModel tableModel;
    private final JTextField locationField;

    public StoreInfoSwing() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 상단
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(Color.WHITE);

        JLabel title = new JLabel("매장 정보");
        title.setFont(new Font("맑은 고딕", Font.BOLD, 24));

        JButton allButton = new JButton("전체 매장");
        locationField = new JTextField(15);
        JButton locationButton = new JButton("위치 검색");

        topPanel.add(title);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(allButton);
        topPanel.add(new JLabel("위치 :"));
        topPanel.add(locationField);
        topPanel.add(locationButton);

        add(topPanel, BorderLayout.NORTH);

        // 테이블
        String[] columns = {"번호", "매장이름", "오픈시간", "마감시간", "전화번호", "위치"};

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(tableModel);
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 14));

        add(new JScrollPane(table), BorderLayout.CENTER);

        // 버튼 이벤트
        allButton.addActionListener(e -> allStoreInfo());
        locationButton.addActionListener(e -> locationStore());

        // 처음 화면에 전체 매장 출력
        allStoreInfo();
    }

    // 매장 전체 정보
    private void allStoreInfo() {
        try {
            List<StoreInfo> storeInfoList = storeInfoService.allStoreInfo();
            tableModel.setRowCount(0);

            for (StoreInfo s : storeInfoList) {
                tableModel.addRow(new Object[]{
                        s.getStoreId(),
                        s.getStoreName(),
                        s.getOpenTime(),
                        s.getCloseTime(),
                        s.getStoreTell(),
                        s.getLocation()
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "매장 정보를 가져오는 중 오류가 발생했습니다.\n" + e.getMessage(),
                    "오류",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    // 매장 위치별 정보
    private void locationStore() {
        String location = locationField.getText().trim();

        if (location.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "찾을 매장 위치를 입력해주세요.",
                    "입력 오류",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            List<StoreInfo> storeInfoList = storeInfoService.LocationStore(location);
            tableModel.setRowCount(0);

            for (StoreInfo s : storeInfoList) {
                tableModel.addRow(new Object[]{
                        s.getStoreId(),
                        s.getStoreName(),
                        s.getOpenTime(),
                        s.getCloseTime(),
                        s.getStoreTell(),
                        s.getLocation()
                });
            }

            if (storeInfoList.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "해당 위치의 매장이 없습니다.",
                        "검색 결과",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "검색 중 오류가 발생했습니다.\n" + e.getMessage(),
                    "오류",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}