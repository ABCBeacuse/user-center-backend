package com.example.user_center.module.domain.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 8839806114308352074L;

    /**
     * 用户账户
     */
    private String userAccount;

    /**
     * 用户密码
     */
    private String userPassword;
}
