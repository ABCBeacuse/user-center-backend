package com.example.yupao_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.yupao_backend.common.BaseResponse;
import com.example.yupao_backend.common.ErrorCode;
import com.example.yupao_backend.common.ResultUtils;
import com.example.yupao_backend.exception.BussinessException;
import com.example.yupao_backend.module.domain.Team;
import com.example.yupao_backend.module.domain.User;
import com.example.yupao_backend.module.dto.TeamQuery;
import com.example.yupao_backend.module.request.TeamAddRequest;
import com.example.yupao_backend.service.TeamService;
import com.example.yupao_backend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/team")
@CrossOrigin(origins = {"http://127.0.0.1:5173"}, allowCredentials = "true")
public class TeamServiceController {

    @Resource
    private UserService userService;

    @Resource
    private TeamService teamService;

    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest httpServletRequest) {
        if(teamAddRequest == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        long teamId = teamService.addTeam(team, loginUser);
        return ResultUtils.success(teamId);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody long id) {
        if(id <= 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = teamService.removeById(id);
        if(!result){
            throw new BussinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }
        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody Team team) {
        if(team == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        /*
        * 这里的 updateById 中传入的 team 对象一定要有一个 id, id 指的是要更新哪条信息的 id
        * 返回 false 的情况：1. 传入的 team 的所有字段和原来的一样 2. 传入的 team 的 id 值在数据库找不到
        */
        boolean result = teamService.updateById(team);
        if(!result) {
            throw new BussinessException(ErrorCode.SYSTEM_ERROR, "更新失败");
        }
        return ResultUtils.success(true);
    }

    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(long id) {
        if(id <= 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = teamService.getById(id);
        if(team == null) {
            throw new BussinessException(ErrorCode.SYSTEM_ERROR, "未找到相关查询数据");
        }
        return ResultUtils.success(team);
    }

    @GetMapping("/list")
    public BaseResponse<List<Team>> getTeams(TeamQuery teamQuery) {
        if(teamQuery == null){
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        // 需要做一个 TeamQuery 到 Team 的映射，这里先做一个强转。将 teamQuery 的字段值全部设置给 team
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        // QueryWrapper 会根据传递的 team 中的字段值去进行条件搜索，这样不能支持模糊查询
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        List<Team> list = teamService.list(queryWrapper);
        return ResultUtils.success(list);
    }

    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> listTeamsByPage(TeamQuery teamQuery) {
        if(teamQuery == null){
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
}
