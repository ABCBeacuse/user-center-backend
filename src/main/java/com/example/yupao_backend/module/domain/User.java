package com.example.yupao_backend.module.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户表
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User implements Serializable {
    /**
     * 用户 ID
     */
    @TableId(type = IdType.AUTO)
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
     * 用户密码
     */
    private String userPassword;

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
     * 更新时间
     */
    private String tags;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer isDelete;

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

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}