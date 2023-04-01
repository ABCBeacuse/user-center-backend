package com.example.user_center.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.user_center.common.ErrorCode;
import com.example.user_center.exception.BussinessException;
import com.example.user_center.module.domain.User;
import com.example.user_center.service.UserService;
import com.example.user_center.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.user_center.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现类
 * @author yupi
 * @description 针对表【user(用户表)】的数据库操作Service实现
 * @createDate 2023-02-08 16:04:51
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService {

    @Resource
    private UserMapper userMapper;

    /**
     *  盐值，混淆密码
     */
    private static final String SALT = "yupi";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode)  {
        // 1. 非空
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)){
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        // 2. 长度
        if (userAccount.length()< 4){
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"用户账号长度过短");
        }
        if (userPassword.length() < 8){
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"用户密码长度过短");
        }
        // 3. 星球编号长度不超过 5 
        if (planetCode.length() > 5){
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"用户星球编号长度过长");
        }
        // 3.账户不能包含特殊字符
        String regEx = "[ _`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]|\n|\r|\t";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(userAccount);
        boolean isNotVaild = m.find();
        if (isNotVaild) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"用户账号包含非法字符");
        }
        // 4.密码和校验密码相同
        if(!userPassword.equals(checkPassword)){
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"密码和校验密码不相同");
        }
        // 5.账户不重复 需要连接数据库查询，如果放在前面会造成资源浪费
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount",userAccount);
        Long count = userMapper.selectCount(userQueryWrapper);
        if (count > 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"账户已被注册");
        }
        
        // 查看星球编号是否重复
        userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("planetCode", planetCode);
        count = userMapper.selectCount(userQueryWrapper);
        if (count > 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"星球编号已被注册");
        }

        // 6.校验完毕，对密码进行加密后，插入数据库
        String encryResult = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes(StandardCharsets.UTF_8));
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryResult);
        user.setPlanetCode(planetCode);
        // 这里使用 userMapper 也可以，也可以使用 ServiceImpl 当中实现的方法
        boolean save = this.save(user);
        if (!save){
            throw new BussinessException(ErrorCode.SYSTEM_ERROR);
        }
        return user.getId();
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1.非空
        if (StringUtils.isAnyBlank(userAccount, userPassword)){
            throw new BussinessException(ErrorCode.NULL_ERROR,"账户或者密码为空");
        }
        // 2.账号长度不小于4位
        if (userAccount.length() < 4){
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"账号长度过长");
        }
        // 3.密码长度不小于8位
        if (userPassword.length() < 8){
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"密码长度过短");
        }
        // 4.校验用户账号的非法字符
        String regEx = "[ _`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]|\n|\r|\t";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(userAccount);
        if(m.find()){
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"账号存在非法字符");
        }
        // 5.查看账户和密码是否匹配
        String encryptPassWord = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes(StandardCharsets.UTF_8));
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        queryWrapper.eq("userPassword",encryptPassWord);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null){
            log.info("Login failed, userAccount and userPassword cannot match.");
            throw new BussinessException(ErrorCode.PARAMS_ERROR,"用户不存在");
        }
        // 6.用户信息系脱敏
        User safeUser = getSafetyUser(user);

        //7.记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safeUser);
        return safeUser;
    }

    /**
     * 用户信息脱敏
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser){

        if(originUser == null){
            return null;
        }

        User safeUser = new User();
        safeUser.setId(originUser.getId());
        safeUser.setUsername(originUser.getUsername());
        safeUser.setAvatarUrl(originUser.getAvatarUrl());
        safeUser.setGender(originUser.getGender());
        safeUser.setUserAccount(originUser.getUserAccount());
        safeUser.setPhone(originUser.getPhone());
        safeUser.setEmail(originUser.getEmail());
        safeUser.setCreateTime(originUser.getCreateTime());
        safeUser.setUserStatus(originUser.getUserStatus());
        safeUser.setUserRole(originUser.getUserRole());
        safeUser.setPlanetCode(originUser.getPlanetCode());

        return safeUser;
    }

    /**
     * 清空 request 中的 session
     *
     * @param request
     * @return
     */
    @Override
    public int userLogout(HttpServletRequest request ){

        // 移除登录态
        request.getSession().removeAttribute( USER_LOGIN_STATE );
        return 1;
    }
}




