package com.example.yupao_backend.service;

import com.example.yupao_backend.module.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yupao_backend.module.domain.User;

/**
* @author Administrator
* @description 针对表【team(队伍表)】的数据库操作Service
* @createDate 2023-05-11 17:44:33
*/
public interface TeamService extends IService<Team> {

    /**
     * 创建队伍
     *
     * @param team 传入的需要创建的队伍信息
     * @param loginUser 当前登录用户
     * @return  创建成功的队伍 id
     */
    long addTeam(Team team, User loginUser);
}
