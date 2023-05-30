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
import com.example.yupao_backend.module.request.TeamQuitRequest;
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
import java.util.*;

/**
 * @author Administrator
 * @description 针对表【team(队伍表)】的数据库操作Service实现
 * @createDate 2023-05-11 17:44:33
 */
@EqualsAndHashCode
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    @Resource
    private UserTeamService userTeamService;

    @Resource
    private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addTeam(Team team, User loginUser) {
        //  a. 请求参数是否为空？
        if (team == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        //  b. 是否登录，未登录不允许创建
        if (loginUser == null) {
            throw new BussinessException(ErrorCode.NOT_LOGIN);
        }
        // 提取当前登录用户的id，有些公司的代码审核要求变量定义后必须在 10 行之内使用。但是添加上了 final 后就会放松这个变量的使用要求。
        // final 表示变量的值不会发生变化。
        final long userId = loginUser.getId();
        //  c. 校验信息
        //    ⅰ. 队伍人数 >1 且 <= 20    因为 Integer 是一个包装类，可能为 null
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum < 1 || maxNum > 20) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "队伍人数不符合要求");
        }
        //    ⅱ. 队伍标题 <= 20
        String teamName = team.getName();
        if (StringUtils.isBlank(teamName) || teamName.length() > 20) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "队伍名称不能为空，并且长度不能超过 20 个字符");
        }
        //    ⅲ. 队伍描述 <= 512
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "队伍描述过长");
        }
        //    ⅳ. status 是否公开（ int 值校验 ）不传默认为 0 （ 公开 ）
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnums statusEnum = TeamStatusEnums.getEnumByValue(status);
        if (statusEnum == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "队伍状态设置错误");
        }
        //    ⅴ. 如果 status 是加密状态，一定要有密码，且密码 <= 32
        String password = team.getPassword();
        if (TeamStatusEnums.SECRET.equals(statusEnum)) {
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new BussinessException(ErrorCode.PARAMS_ERROR, "队伍密码不符合要求");
            }
        }
        //    ⅵ. 超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if (expireTime != null && new Date().after(expireTime)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "超时时间 > 当前时间");
        }
        //    ⅶ. 校验用户最多创建 5 个队伍
        // todo 存在 bug，如果用户在一瞬间点击 100 次，会添加 100 个队伍。需要添加 锁。
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        long hasTeamNum = this.count(queryWrapper);
        if (hasTeamNum >= 5) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "该用户创建队伍已达上限");
        }
        //  d. 插入队伍信息到队伍表（这里需要用到事务，因为 队伍表和下面的关系表需要同步，两个表需要记录同步，要有都有）
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        // Mybatis 在数据插入完毕后, 会自动给这个对象设置一个 Id, 会回写。
        Long teamId = team.getId();
        if (!result || teamId == null) {
            // 这里抛异常后，事务会进行回滚
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }
        //  e. 插入用户 => 队伍关系到关系表，
        /*
         * 建议引入 service，不要引入原始的 mapper 了，因为你不知道插入关系表是不是有什么额外的校验。所以最好调用 service。
         * mapper 中一般不会做太多校验，service 中会对业务加强校验。除非你要加非常严格的限制，可以使用 mapper。
         */
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());

        result = userTeamService.save(userTeam);
        if (!result) {
            // 这里抛异常后，事务会进行回滚
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "创建队伍失败");
        }

        return teamId;
    }

    @Override
    public List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        // teamQuery 为空时，表示查询所有的队伍；组合查询条件
        if (teamQuery != null) {
            //这里的 if 判断语句避免不了，写在这里 或者 写在 SQL 查询语句中
            Long teamId = teamQuery.getId();
            if (teamId != null && teamId > 0) {
                queryWrapper.eq("id", teamId);
            }
            List<Long> ids = teamQuery.getIds();
            // 根据 id 列表查询
            if (CollectionUtils.isNotEmpty(ids)) {
                queryWrapper.in("id", ids);
            }
            // 使用关键词查询，用户输入关键词，同时从 队名 和 描述 里面查询
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            String teamName = teamQuery.getName();
            if (StringUtils.isNotBlank(teamName)) {
                queryWrapper.like("name", teamName);
            }
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            Integer maxNum = teamQuery.getMaxNum();
            // 查询最大人数相等的
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxNum", maxNum);
            }
            Long userId = teamQuery.getUserId();
            // 根据创建人来查询
            if (userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }
//            // 根据状态来查询
//            Integer status = Optional.ofNullable(teamQuery.getStatus()).orElse(0);
//            TeamStatusEnums statusEnum = Optional.ofNullable(TeamStatusEnums.getEnumByValue(status)).orElse(TeamStatusEnums.PUBLIC);
//            if (!isAdmin && !TeamStatusEnums.PUBLIC.equals(statusEnum)) {
//                throw new BussinessException(ErrorCode.NOT_AUTH);
//            }
//            queryWrapper.eq("status", statusEnum.getValue());
            List<Integer> statuses = Optional.ofNullable(teamQuery.getStatuses()).orElse(Collections.singletonList(0));
            // 根据状态列表来查询
            if (!isAdmin && (statuses.contains(1) || statuses.contains(2))) {
                throw new BussinessException(ErrorCode.NOT_AUTH);
            }
            queryWrapper.in("status", statuses);
        }
        // 不展示已过期的队伍
        // expireTime is Null or expireTime > new Date()
        // lambda 条件表达式，会将子方法的中的条件表达式拼接到 SQL 语句中。
        queryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        List<Team> teamList = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }
        List<TeamUserVO> resultTeamList = new ArrayList<>();
        for (Team team : teamList) {
            Long createId = team.getUserId();
            if (createId == null) {
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
        if (teamUpdateRequest == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamUpdateRequest.getId();
        Team oldTeam = this.getTeamById(teamId);
        // 只有管理员或者创建者才能修改队伍信息
        long userId = loginUser.getId();
        if (userId != oldTeam.getUserId() && !userService.isAdmin(loginUser)) {
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
            if ("".equals(newTeamPassword)) {
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
        Team team = this.getTeamById(teamId);
        Date expireTime = team.getExpireTime();
        // 加入未过期的队伍
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "加入的队伍已过期");
        }
        // 禁止加入私有的队伍
        TeamStatusEnums teamStatus = TeamStatusEnums.getEnumByValue(team.getStatus());
        if (TeamStatusEnums.PRIVATE.equals(teamStatus)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有的队伍");
        }
        String requestPassword = teamJoinRequest.getPassword();
        // 如果加入的队伍是加密的，必须密码匹配才可以
        if (TeamStatusEnums.SECRET.equals(teamStatus)) {
            if (StringUtils.isBlank(requestPassword) || !requestPassword.equals(team.getPassword())) {
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


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        if (teamQuitRequest == null || loginUser == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        final long userId = loginUser.getId();
        Long teamId = teamQuitRequest.getTeamId();
        // 查看队伍是否存在
        Team team = this.getTeamById(teamId);
        // 查看是否已经加入了队伍
        // 封装查询条件，可以把查询条件传递到一个 UserTeam 对象中
        UserTeam queryUserTeam = new UserTeam();
        queryUserTeam.setTeamId(teamId);
        queryUserTeam.setUserId(userId);
        QueryWrapper<UserTeam> userIdAndTeamId = new QueryWrapper<>(queryUserTeam);
        long count = userTeamService.count(userIdAndTeamId);
        if (count == 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "未加入队伍");
        }
        // 查看队伍人数
        long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
        // 剩余一人
        if (teamHasJoinNum == 1) {
            // 删除队伍表中的队伍信息
            this.removeById(teamId);
        } else {
            // 剩余至少两个人
            // 是否为队长，默认 team 表中的 userId 为队长
            if (userId == team.getUserId()) {
                // 是队长，退出后需要将权限转移给第二个加入队伍的人（先来后到）
                QueryWrapper<UserTeam> queryNextLeader = new QueryWrapper<>();
                queryNextLeader.eq("teamId", teamId);
                // 不需要查询队伍中全部的用户，只需要根据用户队伍关系的 id 升序排列，取前两条信息即可
                // .last 方法可以在 Mybatis-plus 生成的 SQL 语句尾部添加上输入的 SQL 语句
                queryNextLeader.last("order by id asc limit 2");
                List<UserTeam> leadersRelationList = userTeamService.list(queryNextLeader);
                if (CollectionUtils.isEmpty(leadersRelationList) || leadersRelationList.size() <= 1) {
                    // 如果出现查询出来的关系列表的大小，出现空或者成员不足两个，则说明系统异常。
                    throw new BussinessException(ErrorCode.SYSTEM_ERROR);
                }
                // 更改 team 的 userId 为 teamLeaders 中的第二个用户
                Team updateTeamMes = new Team();
                // updateById 需要传递 id
                updateTeamMes.setId(teamId);
                updateTeamMes.setUserId(leadersRelationList.get(1).getUserId());
                // 更新 team 表的信息
                boolean result = this.updateById(updateTeamMes);
                if (!result) {
                    throw new BussinessException(ErrorCode.SYSTEM_ERROR, "更新队伍队长失败");
                }
            }
        }
        // 不管是不是队长，都是要删除 UserTeam 表的关联信息
        return userTeamService.remove(userIdAndTeamId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTeam(long id, User loginUser) {
        if (id <= 0 || loginUser == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getTeamById(id);
        Long teamId = team.getId();
        // 判断当前登录用户是否为队伍队长
        if (!team.getUserId().equals(loginUser.getId())) {
            throw new BussinessException(ErrorCode.NOT_AUTH, "无删除权限");
        }
        // 删除该队伍的用户-队伍关联信息
        QueryWrapper<UserTeam> teamQueryWrapper = new QueryWrapper<>();
        teamQueryWrapper.eq("teamId", teamId);
        boolean removeRelations = userTeamService.remove(teamQueryWrapper);
        if (!removeRelations) {
            throw new BussinessException(ErrorCode.SYSTEM_ERROR, "删除队伍关联信息失败");
        }
        // 删除队伍信息
        return this.removeById(teamId);
    }

    /**
     * 根据队伍 ID 获取队伍信息
     *
     * @param teamId
     * @return
     */
    private Team getTeamById(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        // 判断队伍是否存在
        if (team == null) {
            throw new BussinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        return team;
    }

    /**
     * 获取队伍中的用户数量
     *
     * @param teamId
     * @return
     */
    private long countTeamUserByTeamId(Long teamId) {
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        return userTeamService.count(queryWrapper);
    }
}




