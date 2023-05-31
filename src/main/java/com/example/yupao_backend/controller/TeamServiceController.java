package com.example.yupao_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.yupao_backend.common.BaseResponse;
import com.example.yupao_backend.common.DeleteRequest;
import com.example.yupao_backend.common.ErrorCode;
import com.example.yupao_backend.common.ResultUtils;
import com.example.yupao_backend.exception.BussinessException;
import com.example.yupao_backend.module.domain.Team;
import com.example.yupao_backend.module.domain.User;
import com.example.yupao_backend.module.domain.UserTeam;
import com.example.yupao_backend.module.dto.TeamQuery;
import com.example.yupao_backend.module.request.TeamAddRequest;
import com.example.yupao_backend.module.request.TeamJoinRequest;
import com.example.yupao_backend.module.request.TeamQuitRequest;
import com.example.yupao_backend.module.request.TeamUpdateRequest;
import com.example.yupao_backend.module.vo.TeamUserVO;
import com.example.yupao_backend.module.vo.UserVO;
import com.example.yupao_backend.service.TeamService;
import com.example.yupao_backend.service.UserService;
import com.example.yupao_backend.service.UserTeamService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/team")
@CrossOrigin(origins = {"http://127.0.0.1:5173"}, allowCredentials = "true")
public class TeamServiceController {

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamService userTeamService;

    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest httpServletRequest) {
        if (teamAddRequest == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        long teamId = teamService.addTeam(team, loginUser);
        return ResultUtils.success(teamId);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest httpServletRequest) {
        if (teamUpdateRequest == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean result = teamService.updateTeam(teamUpdateRequest, loginUser);
        if (!result) {
            throw new BussinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(long id) {
        if (id <= 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        if (team == null) {
            throw new BussinessException(ErrorCode.SYSTEM_ERROR, "未找到相关查询数据");
        }
        return ResultUtils.success(team);
    }

    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> getTeams(TeamQuery teamQuery, HttpServletRequest httpServletRequest) {
        if (teamQuery == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        Boolean isAdmin = userService.isAdmin(httpServletRequest);
        List<TeamUserVO> userTeamList = teamService.listTeams(teamQuery, isAdmin);
        // 1. 获取查询出来的所有队伍 ID
        List<Long> teamIds = userTeamList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        // 2. 判断当前登录用户是否加入查询出来的队伍，完善TeamUserVO 的 hasJoin 字段.
        try {
            // 使用 try-catch 捕捉异常, 为了用户未登录也能查询队伍信息，队伍信息的 hasJoin 默认为 false，
            User loginUser = userService.getLoginUser(httpServletRequest);
            QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
            userTeamQueryWrapper.eq("userId", loginUser.getId());
            userTeamQueryWrapper.in("teamId", teamIds);
            userTeamQueryWrapper.select("teamId");
            // 查询出来的所有队伍中，用户加入的队伍 Id 信息
            Set<Long> userHasJoinTeams = userTeamService.list(userTeamQueryWrapper).stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
            // 将用户已经加入的队伍的 TeamUserVO 的 hasJoin 字段设置为 true
            userTeamList.forEach(team -> {
                if (userHasJoinTeams.contains(team.getId())) {
                    team.setHasJoin(true);
                }
            });
        } catch (Exception e) {}
        // 3. 查询每个队伍加入的人数
        QueryWrapper<UserTeam> teamJoinNumQuery = new QueryWrapper<>();
        teamJoinNumQuery.in("teamId", teamIds);
        // ID => UserTeam 条数
        Map<Long, List<UserTeam>> teamIdUserTeamList = userTeamService.list(teamJoinNumQuery).stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        userTeamList.forEach(team -> {
            team.setJoinNum(teamIdUserTeamList.getOrDefault(team.getId(), new ArrayList<>()).size());
        });
        return ResultUtils.success(userTeamList);
    }

    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamsByPage(TeamQuery teamQuery) {
        if (teamQuery == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        // 配置 Page 的参数 因为这里 teamQuery 有默认值，所以不用判空
        Page<Team> page = new Page<>(teamQuery.getPageNum(), teamQuery.getPageSize());
        // QueryWrapper 会根据传递的 team 中的字段值去进行条件搜索，这样不能支持模糊查询
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        Page<Team> resultPage = teamService.page(page, queryWrapper);
        return ResultUtils.success(resultPage);
    }

    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest httpServletRequest) {
        if (teamJoinRequest == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResultUtils.success(result);
    }

    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest httpServletRequest) {
        if (teamQuitRequest == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
        return ResultUtils.success(result);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest httpServletRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        // 获取需要删除的队伍 ID
        long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(httpServletRequest);
        boolean result = teamService.removeTeam(id, loginUser);
        if (!result) {
            throw new BussinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 获取当前登录用户创建的队伍列表
     *
     * @param httpServletRequest
     * @return
     */
    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVO>> listMyTeams(TeamQuery teamQuery, HttpServletRequest httpServletRequest) {
        if (teamQuery == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        teamQuery.setUserId(loginUser.getId());
        // 查询本人创建的所有状态的队伍
        teamQuery.setStatuses(Arrays.asList(0, 1, 2));
        List<TeamUserVO> teams = teamService.listTeams(teamQuery, true);
        // 因为这个系统没有房主和创建人的区别，所以创建人=房主，所以用户如果创建了这个房间，那么这个用户一定在这个房间
        teams.forEach(teamUserVO -> teamUserVO.setHasJoin(true));
        return ResultUtils.success(teams);
    }

    /**
     * 获取当前登录用户加入的队伍列表
     *
     * @param httpServletRequest
     * @return
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> listMyJoinTeams(TeamQuery teamQuery, HttpServletRequest httpServletRequest) {
        if (teamQuery == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        // 查询当前用户加入的队伍关联信息
        QueryWrapper<UserTeam> query = new QueryWrapper<>();
        query.eq("userId", loginUser.getId());
        List<UserTeam> userTeamList = userTeamService.list(query);
        // 获取不重复的队伍 ID, 虽然队伍 ID 实际上不会重复, 防止脏数据的存在还是去重一下
        Map<Long, List<UserTeam>> listMap = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        List<Long> idList = new ArrayList<>(listMap.keySet());
        // 添加一下 teamQuery 的查询条件
        teamQuery.setIds(idList);
        // 查询本人加入的所有状态的队伍
        teamQuery.setStatuses(Arrays.asList(0, 1, 2));
        List<TeamUserVO> result = teamService.listTeams(teamQuery, true);
        // 因为这里是获取当前登录用户加入的队伍列表，所有 hasJoin 字段应该为 true.
        result.forEach(teamUserVO -> teamUserVO.setHasJoin(true));
        return ResultUtils.success(result);
    }
}
