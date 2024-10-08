package com.example.yupao_backend.module.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 队伍和用户的包装类
 *
 * @author yupi
 */
@Data
public class TeamUserVO implements Serializable {

    private static final long serialVersionUID = 8588912220439045969L;

    /**
     * 队伍 ID
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 创建人 id
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 当前登录用户是否加入该队伍
     */
    private boolean hasJoin = false;

    /**
     * 当前队伍加入的人数
     */
    private Integer joinNum;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 创建人信息
     */
    UserVO createUser;
}
