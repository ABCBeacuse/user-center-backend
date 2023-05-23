package com.example.yupao_backend.module.request;

import lombok.Data;

import java.io.Serializable;

@Data
public class TeamQuitRequest implements Serializable {

    private static final long serialVersionUID = -2330003374231913323L;

    /**
     * 队伍 ID
     */
    private Long teamId;
}
