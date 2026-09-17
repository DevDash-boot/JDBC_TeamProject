package com.tenco.dto;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Admin {
    private int adminId;
    private String loginId;
    private String password;
    private String name;
}