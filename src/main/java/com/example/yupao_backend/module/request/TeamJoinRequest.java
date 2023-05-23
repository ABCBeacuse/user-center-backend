package com.example.yupao_backend.module.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户加入队伍请求体
 *
 */
@Data
public class TeamJoinRequest implements Serializable {

    private static final long serialVersionUID = -8525771984948623957L;

    /**
     * 需要加入的队伍 Id
     */
    private Long teamId;

    /**
     * 队伍密码
     */
    private String password;
}
