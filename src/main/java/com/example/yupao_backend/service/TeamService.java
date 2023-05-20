package com.example.yupao_backend.service;

import com.example.yupao_backend.module.domain.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yupao_backend.module.domain.User;
import com.example.yupao_backend.module.dto.TeamQuery;
import com.example.yupao_backend.module.request.TeamJoinRequest;
import com.example.yupao_backend.module.request.TeamUpdateRequest;
import com.example.yupao_backend.module.vo.TeamUserVO;

import java.util.List;

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

    /**
     * 多条件查询队伍列表
     * @param teamQuery
     * @param isAdmin
     * @return
     */
    List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 更新队伍信息
     * @param teamUpdateRequest 更新队伍的信息
     * @param loginUser 当前登录用户
     * @return 更新结果 boolean
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     * 加入队伍
     * @param teamJoinRequest 需要加入的队伍信息
     * @param loginUser 当前登录用户
     * @return
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);
}
