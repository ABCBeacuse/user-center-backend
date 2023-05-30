package com.example.yupao_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.yupao_backend.common.BaseResponse;
import com.example.yupao_backend.common.ErrorCode;
import com.example.yupao_backend.common.ResultUtils;
import com.example.yupao_backend.exception.BussinessException;
import com.example.yupao_backend.module.domain.User;
import com.example.yupao_backend.module.request.UserLoginRequest;
import com.example.yupao_backend.module.request.UserRegisterRequest;
import com.example.yupao_backend.module.vo.UserVO;
import com.example.yupao_backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.yupao_backend.constant.UserConstant.USER_LOGIN_STATE;

@RestController
@RequestMapping("/user")
@CrossOrigin(origins = {"http://127.0.0.1:5173"}, allowCredentials = "true")
@Slf4j
public class UserServiceController {

    @Resource
    private UserService userService;

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        return ResultUtils.success(result);
    }

    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        if (userLoginRequest == null) {
            throw new BussinessException(ErrorCode.NULL_ERROR);
        }

        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();

        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }

        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request) {
        if (request == null) {
            throw new BussinessException(ErrorCode.NULL_ERROR);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        if (request == null) {
            throw new BussinessException(ErrorCode.NULL_ERROR);
        }
        User currentUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (currentUser == null) {
            throw new BussinessException(ErrorCode.NOT_LOGIN);
        }
        Long userId = currentUser.getId();

        // 因为用户的一些数据可能会更新频繁，所以最好是操作数据库来取最新数据
        currentUser = userService.getById(userId);
        User safetyUser = userService.getSafetyUser(currentUser);
        return ResultUtils.success(safetyUser);
    }

    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String userName, HttpServletRequest request) {
        if (!userService.isAdmin(request)) {
            throw new BussinessException(ErrorCode.NOT_AUTH);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(userName)) {
            queryWrapper.like("username", userName);
        }

        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    /**
     * 获取推荐用户
     *
     * @param request
     * @return
     */
    @GetMapping("/recommend")
    public BaseResponse<List<User>> recommendUsers(long pageNum, long pageSize, HttpServletRequest request) {
        // getLoginUser() 方法已经对是否登录做出了判断
        User loginUser = userService.getLoginUser(request);
        List<User> userList = userService.getRecommendUsers(pageNum, pageSize, loginUser.getId());
        return ResultUtils.success(userList);
    }

    @PostMapping("/update")
    public BaseResponse<Integer> updateUser(@RequestBody User user, HttpServletRequest request) {
        if (user == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        int result = userService.updateUser(user, loginUser);
        return ResultUtils.success(result);
    }

    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        List<User> userList = userService.searchUsersByTags(tagNameList);
        return ResultUtils.success(userList);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody Long userId, HttpServletRequest request) {
        if (!userService.isAdmin(request)) {
            throw new BussinessException(ErrorCode.NOT_AUTH);
        }
        if (userId <= 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }

        boolean result = userService.removeById(userId);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户的推荐用户
     *
     * @param num  返回 top N
     * @param httpServletRequest
     * @return
     */
    @GetMapping("/match")
    public BaseResponse<List<UserVO>> matchUsers(@RequestParam Long num, HttpServletRequest httpServletRequest) {
        if (num <= 0 || num > 20) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpServletRequest);
        List<UserVO> result = userService.matchUsers(num, loginUser);
        return ResultUtils.success(result);
    }
}
