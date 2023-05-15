package com.example.yupao_backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yupao_backend.module.domain.UserTeam;
import com.example.yupao_backend.service.UserTeamService;
import com.example.yupao_backend.mapper.UserTeamMapper;
import org.springframework.stereotype.Service;

/**
* @author Administrator
* @description 针对表【user_team(用户队伍关系表)】的数据库操作Service实现
* @createDate 2023-05-11 22:05:35
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

}




