package com.example.yupao_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yupao_backend.common.ErrorCode;
import com.example.yupao_backend.exception.BussinessException;
import com.example.yupao_backend.mapper.TeamMapper;
import com.example.yupao_backend.module.domain.Team;
import com.example.yupao_backend.module.domain.User;
import com.example.yupao_backend.module.domain.UserTeam;
import com.example.yupao_backend.module.dto.TeamQuery;
import com.example.yupao_backend.module.enums.TeamStatusEnums;
import com.example.yupao_backend.module.request.TeamJoinRequest;
import com.example.yupao_backend.module.request.TeamUpdateRequest;
import com.example.yupao_backend.module.vo.TeamUserVO;
import com.example.yupao_backend.module.vo.UserVO;
import com.example.yupao_backend.service.TeamService;
import com.example.yupao_backend.service.UserService;
import com.example.yupao_backend.service.UserTeamService;
import lombok.EqualsAndHashCode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

    @Resource
    private UserService userService;

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
        if(expireTime != null && new Date().after(expireTime)) {
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

    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        // teamQuery 为空时，表示查询所有的队伍；组合查询条件
        if(teamQuery != null) {
            //这里的 if 判断语句避免不了，写在这里 或者 写在 SQL 查询语句中
            Long teamId = teamQuery.getId();
            if(teamId != null && teamId > 0) {
                queryWrapper.eq("id", teamId);
            }
            // 使用关键词查询，用户输入关键词，同时从 队名 和 描述 里面查询
            String searchText = teamQuery.getSearchText();
            if(StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description",searchText));
            }
            String teamName = teamQuery.getName();
            if(StringUtils.isNotBlank(teamName)) {
                queryWrapper.like("name", teamName);
            }
            String description = teamQuery.getDescription();
            if(StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            Integer maxNum = teamQuery.getMaxNum();
            // 查询最大人数相等的
            if(maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxNum", maxNum);
            }
            Long userId = teamQuery.getUserId();
            // 根据创建人来查询
            if(userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }
            // 根据状态来查询
            Integer status = Optional.ofNullable(teamQuery.getStatus()).orElse(0) ;
            TeamStatusEnums statusEnum = Optional.ofNullable(TeamStatusEnums.getEnumByValue(status)).orElse(TeamStatusEnums.PUBLIC);
            if(!isAdmin && !TeamStatusEnums.PUBLIC.equals(statusEnum)) {
                throw new BussinessException(ErrorCode.NOT_AUTH);
            }
            queryWrapper.eq("status",statusEnum.getValue());
        }
        // 不展示已过期的队伍
        // expireTime is Null or expireTime > new Date()
        // lambda 条件表达式，会将子方法的中的条件表达式拼接到 SQL 语句中。
        queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        List<Team> teamList = this.list(queryWrapper);
        if(CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }
        List<TeamUserVO> resultTeamList = new ArrayList<>();
        for(Team team : teamList) {
            Long createId = team.getUserId();
            if(createId == null) {
                continue;
            }
            User createUser = userService.getById(createId);
            // 用户信息脱敏
            UserVO createUserVO = new UserVO();
            BeanUtils.copyProperties(createUser, createUserVO);
            // 队伍信息和用户信息封装到 TeamUserVO 中
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            teamUserVO.setCreateUser(createUserVO);
            // 添加到返回列表
            resultTeamList.add(teamUserVO);
        }

        return resultTeamList;
    }

    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        if(teamUpdateRequest == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamUpdateRequest.getId();
        if(teamId == null || teamId <= 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        Team oldTeam = this.getById(teamId);
        if(oldTeam == null) {
            throw new BussinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        // 只有管理员或者创建者才能修改队伍信息
        long userId = loginUser.getId();
        if(userId != oldTeam.getUserId() && !userService.isAdmin(loginUser)) {
            throw new BussinessException(ErrorCode.NOT_AUTH);
        }
        String oldTeamPassword = oldTeam.getPassword();
        // 先获取新传递的最新密码，保存起来
        String newTeamPassword = teamUpdateRequest.getPassword();
        // 为了防止传递的是 公开状态，但是却设置密码。保证数据库中的原加密时设置的密码不变，如果再次设置为加密状态，但是没有传递密码，则沿用原队伍密码。
        teamUpdateRequest.setPassword(oldTeamPassword);
        // 如果队伍状态改为加密状态，必须要传递密码
        TeamStatusEnums teamUpdateStatusEnums = TeamStatusEnums.getEnumByValue(teamUpdateRequest.getStatus());
        // todo 现在如果队伍之前就是加密的，本身就有密码，但是当再次修改为加密状态时没有传递密码（意思是不修改密码），也会报错。调整一下
        if (TeamStatusEnums.SECRET.equals(teamUpdateStatusEnums)) {
            if (StringUtils.isAllBlank(newTeamPassword, oldTeamPassword)) {
                throw new BussinessException(ErrorCode.PARAMS_ERROR, "加密房间必须要设置密码");
            }
            // 加密状态下如果未传递新的密码，则继续沿用旧密码。
            if("".equals(newTeamPassword)){
                newTeamPassword = oldTeamPassword;
            }
            teamUpdateRequest.setPassword(newTeamPassword);
        }
        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, updateTeam);
        /*
         * 这里的 updateById 中传入的 team 对象一定要有一个 id, id 指的是要更新哪条信息的 id
         * 返回 false 的情况：1. 传入的 team 的所有字段和原来的一样 2. 传入的 team 的 id 值在数据库找不到
         * 只要传递的不是新的值（对象中的属性），默认都不会更新
         */
        // todo 选择式更新，如果前端没有传递这个字段，后端就不要去将数据库中原有的字段修改为 ""
        return this.updateById(updateTeam);
    }

    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if (teamJoinRequest == null || loginUser == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamJoinRequest.getTeamId();
        if(teamId == null || teamId <= 0){
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        // 加入存在的队伍
        if (team == null ) {
            throw new BussinessException(ErrorCode.NULL_ERROR, "加入的队伍不存在");
        }
        Date expireTime = team.getExpireTime();
        // 加入未过期的队伍
        if (expireTime.before(new Date())) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "加入的队伍已过期");
        }
        // 禁止加入私有的队伍
        TeamStatusEnums teamStatus = TeamStatusEnums.getEnumByValue(team.getStatus());
        if (TeamStatusEnums.PRIVATE.equals(teamStatus)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有的队伍");
        }
        String requestPassword = teamJoinRequest.getPassword();
        // 如果加入的队伍是加密的，必须密码匹配才可以
        if(TeamStatusEnums.SECRET.equals(teamStatus)) {
            if(StringUtils.isBlank(requestPassword) || !requestPassword.equals(team.getPassword())) {
                throw new BussinessException(ErrorCode.PARAMS_ERROR, "队伍密码错误");
            }
        }
        // 用户最多加入 5 个队伍
        long userId = loginUser.getId();
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        long joinTeamsNum = userTeamService.count(queryWrapper);
        if (joinTeamsNum >= 5) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "加入队伍数量超过上限");
        }
        // 只能加入未满的队伍
        queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        long hasJoinTeamNum = userTeamService.count(queryWrapper);
        if (hasJoinTeamNum >= team.getMaxNum()) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "队伍人数已超过上限");
        }
        // 不能重复加入已加入的队伍(userId = ? && teamId =?)
        queryWrapper.eq("userId", userId);
        long count = userTeamService.count(queryWrapper);
        if (count > 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "用户已加入该队伍");
        }
        // 新增队伍-用户关联信息
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        return userTeamService.save(userTeam);
    }
}




