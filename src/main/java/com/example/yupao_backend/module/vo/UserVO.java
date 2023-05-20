package com.example.yupao_backend.module.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户信息包装类（脱敏）
 *
 * @author yupi
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 6440094629959283217L;

    /**
     * 用户 ID
     */
    private long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户头像
     */
    private String avatarUrl;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 用户状态
     */
    private Integer userStatus;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 用户标签
     */
    private String tags;

    /**
     * 用户角色  0 - 普通用户  1 - 管理员
     */
    private Integer userRole;

    /**
     * 星球编号
     */
    private String planetCode;

    /**
     * 用户简介
     */
    private String userProfile;

}
