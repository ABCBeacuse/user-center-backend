package com.example.yupao_backend.service;

import com.example.yupao_backend.module.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yupao_backend.module.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author yupi
* @description 针对表【user(用户表)】的数据库操作Service
* @createDate 2023-02-08 16:04:51
*/
public interface UserService extends IService<User> {


    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param planetCode
     * @return 新用户 ID
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode);

    /**
     * 用户登录
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param request 用来拿到 session，记录用户的登录态
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户信息脱敏
     * @param originUser
     * @return
     */
    User getSafetyUser(User originUser);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    int userLogout( HttpServletRequest request );

    /**
     * 根据标签查询用户
     * @param tagNameList
     * @return
     */
    List<User> searchUsersByTags(List<String> tagNameList);

    /**
     * 修改用户信息
     * @param user
     * @param loginUser
     * @return
     */
    int updateUser(User user, User loginUser);

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取根据当前登录用户返回的推荐列表
     * @param pageNum
     * @param pageSize
     * @param userId
     * @return
     */
    List<User> getRecommendUsers(long pageNum, long pageSize, long userId);

    /**
     * 传递参数 request，判断当前登录用户是否为管理员
     * @param request
     * @return
     */
    Boolean isAdmin(HttpServletRequest request);

    /**
     * 传递参数 User，判断传递的 User 是否为管理员，方法重载
     * @param user
     * @return
     */
    Boolean isAdmin(User user);

    /**
     * 获取当前登录用户的推荐用户
     *
     * @param loginUser
     * @return
     */
    List<UserVO> matchUsers(Long num, User loginUser);
}
