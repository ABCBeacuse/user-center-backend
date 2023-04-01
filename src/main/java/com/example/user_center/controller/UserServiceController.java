package com.example.user_center.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.user_center.common.BaseResponse;
import com.example.user_center.common.ErrorCode;
import com.example.user_center.common.ResultUtils;
import com.example.user_center.exception.BussinessException;
import com.example.user_center.module.domain.User;
import com.example.user_center.module.domain.request.UserLoginRequest;
import com.example.user_center.module.domain.request.UserRegisterRequest;
import com.example.user_center.service.UserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.user_center.constant.UserConstant.ADMIN_ROLE;
import static com.example.user_center.constant.UserConstant.USER_LOGIN_STATE;

@RestController
@RequestMapping("/user")
public class UserServiceController {

    @Resource
    private UserService userService;

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        if (userRegisterRequest == null){
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)){
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        long result = userService.userRegister(userAccount, userPassword, checkPassword, planetCode);
        return ResultUtils.success(result);
    }

    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
        if(userLoginRequest == null){
            throw new BussinessException(ErrorCode.NULL_ERROR);
        }

        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();

        if(StringUtils.isAnyBlank(userAccount, userPassword)){
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }

        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }

    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request){
        if( request == null ){
            throw new BussinessException(ErrorCode.NULL_ERROR);
        }
        int result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request){
        if( request == null ){
            throw new BussinessException(ErrorCode.NULL_ERROR);
        }
        User currentUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if( currentUser == null ){
            throw new BussinessException(ErrorCode.NOT_LOGIN);
        }
        Long userId = currentUser.getId();

        // 因为用户的一些数据可能会更新频繁，所以最好是操作数据库来取最新数据
        currentUser = userService.getById(userId);
        User safetyUser = userService.getSafetyUser(currentUser);
        return ResultUtils.success(safetyUser);
    }

    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String userName, HttpServletRequest request){
        if( !isAdmin(request) ){
            throw new BussinessException(ErrorCode.NOT_AUTH);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(userName)){
            queryWrapper.like("username", userName);
        }

        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody Long userId, HttpServletRequest request ){
        if( !isAdmin(request) ){
            throw new BussinessException(ErrorCode.NOT_AUTH);
        }
        if( userId <= 0 ) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }

        boolean result = userService.removeById(userId);
        return ResultUtils.success(result);
    }

    /**
     * 是否为管理员
     * @param httpServletRequest
     * @return
     */
    private boolean isAdmin(HttpServletRequest httpServletRequest){
        Object userObj = httpServletRequest.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }
}
