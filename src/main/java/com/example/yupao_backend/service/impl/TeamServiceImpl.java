package com.example.yupao_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yupao_backend.common.ErrorCode;
import com.example.yupao_backend.exception.BussinessException;
import com.example.yupao_backend.module.domain.Team;
import com.example.yupao_backend.module.domain.User;
import com.example.yupao_backend.module.domain.UserTeam;
import com.example.yupao_backend.module.enums.TeamStatusEnums;
import com.example.yupao_backend.service.TeamService;
import com.example.yupao_backend.mapper.TeamMapper;
import com.example.yupao_backend.service.UserTeamService;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Optional;

/**
* @author Administrator
* @description 针对表【team(队伍表)】的数据库操作Service实现
* @createDate 2023-05-11 17:44:33
*/
@EqualsAndHashCode
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService{

    @Resource
    private UserTeamService userTeamService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addTeam(Team team, User loginUser) {
        //  a. 请求参数是否为空？
        if(team == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        //  b. 是否登录，未登录不允许创建
        if(loginUser == null) {
            throw new BussinessException(ErrorCode.NOT_LOGIN);
        }
        // 提取当前登录用户的id，有些公司的代码审核要求变量定义后必须在 10 行之内使用。但是添加上了 final 后就会放松这个变量的使用要求。
        // final 表示变量的值不会发生变化。
        final long userId = loginUser.getId();
        //  c. 校验信息
        //    ⅰ. 队伍人数 >1 且 <= 20    因为 Integer 是一个包装类，可能为 null
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if(maxNum < 1 || maxNum > 20) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "队伍人数不符合要求");
        }
        //    ⅱ. 队伍标题 <= 20
        String teamName = team.getName();
        if(StringUtils.isBlank(teamName) || teamName.length() > 20) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"队伍名称不能为空，并且长度不能超过 20 个字符");
        }
        //    ⅲ. 队伍描述 <= 512
        String description = team.getDescription();
        if(StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"队伍描述过长");
        }
        //    ⅳ. status 是否公开（ int 值校验 ）不传默认为 0 （ 公开 ）
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnums statusEnum = TeamStatusEnums.getEnumByValue(status);
        if(statusEnum == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"队伍状态设置错误");
        }
        //    ⅴ. 如果 status 是加密状态，一定要有密码，且密码 <= 32
        String password = team.getPassword();
        if(TeamStatusEnums.SECRET.equals(statusEnum)) {
            if(StringUtils.isBlank(password) || password.length() > 32) {
                throw new BussinessException(ErrorCode.PARAMS_ERROR,"队伍密码不符合要求");
            }
        }
        //    ⅵ. 超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if(new Date().after(expireTime)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"超时时间 > 当前时间");
        }
        //    ⅶ. 校验用户最多创建 5 个队伍
        // todo 存在 bug，如果用户在一瞬间点击 100 次，会添加 100 个队伍。需要添加 锁。
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId",userId);
        long hasTeamNum = this.count(queryWrapper);
        if(hasTeamNum >= 5){
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"该用户创建队伍已达上限");
        }
        //  d. 插入队伍信息到队伍表（这里需要用到事务，因为 队伍表和下面的关系表需要同步，两个表需要记录同步，要有都有）
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        // Mybatis 在数据插入完毕后, 会自动给这个对象设置一个 Id, 会回写。
        Long teamId = team.getId();
        if(!result || teamId == null) {
            // 这里抛异常后，事务会进行回滚
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"创建队伍失败");
        }
        //  e. 插入用户 => 队伍关系到关系表，
        /*
         * 建议引入 service，不要引入原始的 mapper 了，因为你不知道插入关系表是不是有什么额外的校验。所以最好调用 service。
         * mapper 中一般不会做太多校验，service 中会对业务加强校验。除非你要加非常严格的限制，可以使用 mapper。
         */

        UserTeam userTeam = new UserTeam();;
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());

        result = userTeamService.save(userTeam);
        if(!result) {
            // 这里抛异常后，事务会进行回滚
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"创建队伍失败");
        }
        
        return teamId;
    }
}




