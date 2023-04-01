package com.example.user_center.module.domain.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserRegisterRequest implements Serializable {


    private static final long serialVersionUID = 8828398970856589222L;

    /**
     * 用户账户
     */
    private String userAccount;

    /**
     * 用户账户密码
     */
    private String userPassword;

    /**
     * 用户核检面
     */
    private String checkPassword;

    /**
     * 星球编号
     */
    private String planetCode;
}
