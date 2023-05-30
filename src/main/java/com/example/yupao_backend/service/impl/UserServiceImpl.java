package com.example.yupao_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.yupao_backend.common.ErrorCode;
import com.example.yupao_backend.exception.BussinessException;
import com.example.yupao_backend.mapper.UserMapper;
import com.example.yupao_backend.module.domain.User;
import com.example.yupao_backend.module.vo.UserVO;
import com.example.yupao_backend.service.UserService;
import com.example.yupao_backend.utils.AlgorithmUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.example.yupao_backend.constant.UserConstant.ADMIN_ROLE;
import static com.example.yupao_backend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户服务实现类
 *
 * @author yupi
 * @description 针对表【user(用户表)】的数据库操作Service实现
 * @createDate 2023-02-08 16:04:51
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "yupi";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String planetCode) {
        // 1. 非空
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword, planetCode)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        // 2. 长度
        if (userAccount.length() < 4) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "用户账号长度过短");
        }
        if (userPassword.length() < 8) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "用户密码长度过短");
        }
        // 3. 星球编号长度不超过 5 
        if (planetCode.length() > 5) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "用户星球编号长度过长");
        }
        // 3.账户不能包含特殊字符
        String regEx = "[ _`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]|\n|\r|\t";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(userAccount);
        boolean isNotVaild = m.find();
        if (isNotVaild) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "用户账号包含非法字符");
        }
        // 4.密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "密码和校验密码不相同");
        }
        // 5.账户不重复 需要连接数据库查询，如果放在前面会造成资源浪费
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount", userAccount);
        Long count = userMapper.selectCount(userQueryWrapper);
        if (count > 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "账户已被注册");
        }

        // 查看星球编号是否重复
        userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("planetCode", planetCode);
        count = userMapper.selectCount(userQueryWrapper);
        if (count > 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "星球编号已被注册");
        }

        // 6.校验完毕，对密码进行加密后，插入数据库
        String encryResult = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes(StandardCharsets.UTF_8));
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryResult);
        user.setPlanetCode(planetCode);
        // 这里使用 userMapper 也可以，也可以使用 ServiceImpl 当中实现的方法
        boolean save = this.save(user);
        if (!save) {
            throw new BussinessException(ErrorCode.SYSTEM_ERROR);
        }
        return user.getId();
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1.非空
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BussinessException(ErrorCode.NULL_ERROR, "账户或者密码为空");
        }
        // 2.账号长度不小于4位
        if (userAccount.length() < 4) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "账号长度过长");
        }
        // 3.密码长度不小于8位
        if (userPassword.length() < 8) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
        }
        // 4.校验用户账号的非法字符
        String regEx = "[ _`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]|\n|\r|\t";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(userAccount);
        if (m.find()) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "账号存在非法字符");
        }
        // 5.查看账户和密码是否匹配
        String encryptPassWord = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes(StandardCharsets.UTF_8));
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassWord);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            log.info("Login failed, userAccount and userPassword cannot match.");
            throw new BussinessException(ErrorCode.PARAMS_ERROR, "用户不存在");
        }
        // 6.用户信息系脱敏
        User safeUser = getSafetyUser(user);

        //7.记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, safeUser);
        return safeUser;
    }

    /**
     * 用户信息脱敏
     *
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser) {

        if (originUser == null) {
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
        safeUser.setTags(originUser.getTags());
        safeUser.setUserProfile(originUser.getUserProfile());

        return safeUser;
    }

    /**
     * 清空 request 中的 session
     *
     * @param request
     * @return
     */
    @Override
    public int userLogout(HttpServletRequest request) {

        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    /**
     * 根据标签搜索用户
     *
     * @param tagNameList 用户要拥有的标签
     * @return
     */
    @Override
    public List<User> searchUsersByTags(List<String> tagNameList) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        // 内存查询
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        // 先查询所有用户
        List<User> userList = userMapper.selectList(queryWrapper);
        Gson gson = new Gson();
        return userList.parallelStream().filter(user -> {
            String tagsStr = user.getTags();
            // 有的用户没有 tags ，tags 为 null，防止 NPE
            if (StringUtils.isBlank(tagsStr)) {
                return false;
            }
            Set<String> tempTagNameSet = gson.fromJson(tagsStr, new TypeToken<Set<String>>() {
            }.getType());
            // tempTagNameSet 可能为空 ，如果 tempTagNameSet 为空，tempTagNameSet 的值就是 new HashSet，否则就是自身值。
            tempTagNameSet = Optional.ofNullable(tempTagNameSet).orElse(new HashSet<>());
            for (String tagName : tagNameList) {
                if (!tempTagNameSet.contains(tagName)) {
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());
    }

    /**
     * 根据标签搜索用户（SQL 查询版）
     *
     * @param tagNameList 用户要拥有的标签
     * @return
     */
    @Deprecated
    private List<User> searchUsersByTagsBySQL(List<String> tagNameList) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        for (String tagName : tagNameList) {
            // 拼接 and 查询语句 like '%Java%' and like '%C++%'
            queryWrapper = queryWrapper.like("tags", tagName);
        }
        List<User> userList = userMapper.selectList(queryWrapper);
        // 用户信息脱敏
        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());
    }

    /**
     * @param user
     * @param loginUser
     * @return
     */
    @Override
    public int updateUser(User user, User loginUser) {
        long userId = user.getId();
        // id 都大于等于 0
        if (userId <= 0) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 补充校验，如果用户没有传任何要更新的值，就直接报错，不用执行 update 语句

        // 判断是否为管理员或者用户本人，管理员可以修改任何人的信息，不是管理员的话只能修改自己的信息
        if (!isAdmin(loginUser) && user.getId() != loginUser.getId()) {
            throw new BussinessException(ErrorCode.NOT_AUTH);
        }

        User oldUser = userMapper.selectById(userId);
        if (oldUser == null) {
            throw new BussinessException(ErrorCode.NULL_ERROR);
        }
        // 返回更新条数，一般通过Id更新，结果为 0 或 1
        return userMapper.updateById(user);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        if (request == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        // 每次获取当前用户，都要进行一次判空
        if (userObj == null) {
            throw new BussinessException(ErrorCode.NOT_AUTH);
        }
        return (User) userObj;
    }

    /**
     * 根据不同登录用户返回不同的用户推荐列表（添加 Redis 缓存）
     *
     * @param pageNum
     * @param pageSize
     * @param userId
     * @return
     */
    @Override
    public List<User> getRecommendUsers(long pageNum, long pageSize, long userId) {
        String redisKey = String.format("yupao:user:recommed:%s", userId);
        ValueOperations<String, Object> redisOperations = redisTemplate.opsForValue();
        // 先查缓存中是否存储
        List<User> userList = (List<User>) redisOperations.get(redisKey);
        if (userList != null) {
            return userList;
        }
        // 如果缓存中没有存储, 就去查数据库
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        Page<User> userPage = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        userList = userPage.getRecords().stream().map(this::getSafetyUser).collect(Collectors.toList());
        // 查数据库后写缓存
        try {
            // 使用 try-catch 包括, 即使写缓存写失败了也会正常返回数据信息。30s 过期
            redisOperations.set(redisKey, userList, 30000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("redis set key error", e);
        }
        return userList;
    }

    /**
     * 判断用户是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public Boolean isAdmin(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return user != null && user.getUserRole() == ADMIN_ROLE;
    }

    /**
     * 判断用户是否为管理员
     *
     * @param loginuser
     * @return
     */
    @Override
    public Boolean isAdmin(User loginuser) {
        return loginuser != null && loginuser.getUserRole() == ADMIN_ROLE;
    }

    /**
     * 根据当前登录用户的 tags 来匹配其他用户（暴力遍历所有用户，来获得与当前登录用户的相似度评分）
     *
     * @param num       返回 top num 个推荐用户
     * @param loginUser
     * @return
     */
    @Override
    public List<UserVO> matchUsers(Long num, User loginUser) {
        if (loginUser == null) {
            throw new BussinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "tags");
        queryWrapper.isNotNull("tags");
        // 查询出数据库中 tags 不为 Null 的用户数据。
        List<User> userList = this.list(queryWrapper);
        String tags = loginUser.getTags();
        // 将 JSON 格式的 tags 转换为一个 List<String>
        Gson gson = new Gson();
        List<String> loginUserTagsList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        // 使用 List<Pair<User, Integer>> 来存储用户及用户评分, 用户 -> 评分. 使用 List 是为了使用 sorted 方法，根据 value 来进行降序排序。
        // SortedMap 也可以实现，但是需要编写 compartor; 也有一种方式是将 Map 转换为 List ，然后使用 sorted 方法。
        List<Pair<User, Integer>> userPairList = new ArrayList<>();
        for (User user : userList) {
            // 将每个 user 用户的 tags 也转换为 List<String>
            String userTags = user.getTags();
            if (StringUtils.isBlank(userTags) || user.getId() == loginUser.getId()) {
                // 如果用户的 tags 为空, 或者为当前登录用户，则跳过该用户。检查下一个用户
                continue;
            }
            List<String> userTagsList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            // 计算最小编辑距离
            int distance = AlgorithmUtils.minDistance(loginUserTagsList, userTagsList);
            userPairList.add(new Pair<>(user, distance));
        }
        // 返回 top N ( 使用 List 的 sorted 按照编辑距离 distance 由小到大进行序排列 )
        List<Pair<User, Integer>> userTopPairList = userPairList.stream().sorted(Comparator.comparingInt(Pair::getValue)).limit(num).collect(Collectors.toList());
        // 获取 top N user 的 id 列表
        List<Long> userTopIds = userTopPairList.stream().map(Pair -> Pair.getKey().getId()).collect(Collectors.toList());
        // 根据 Top Id 查询用户详细信息
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        // todo in 查询没有顺序，原有的顺序又被打乱。 Tip：知道原有顺序，可以通过 Map 创建一个 id => user 的映射 解决。
        userQueryWrapper.in("id", userTopIds);
        // 根据 user 的 Id 进行分组，id -> List<UserVo> 的映射，但是因为 id 为唯一的，所以 List<UserVo> 的 size 为 1.
        Map<Long, List<UserVO>> userIdUserListMap = this.list(userQueryWrapper).stream().map(user -> {
            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            return userVO;
        }).collect(Collectors.groupingBy(UserVO::getId));
        // 根据 userTopIds 里面的 id 顺序 和 userIdUserListMap，重新组成一个 List<UserVO> 列表
        List<UserVO> finalUserVoList = new ArrayList<>();
        for (Long id : userTopIds) {
            // List<UserVo> 的 size 为 1.
            finalUserVoList.add(userIdUserListMap.get(id).get(0));
        }
        return finalUserVoList;
    }
}




