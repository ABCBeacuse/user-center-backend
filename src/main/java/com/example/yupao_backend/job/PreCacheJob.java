package com.example.yupao_backend.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.yupao_backend.module.domain.User;
import com.example.yupao_backend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 缓存预热
 *
 * @author yupi
 */
@Component
@Slf4j
public class PreCacheJob {

    @Resource
    private UserService userService;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    // 重点用户
    private List<Long> mainUserList = Arrays.asList(1L);

    // 预热推荐用户
    @Scheduled(cron = "0 34 16 * * *")
    public void doCacheRecommendUser() {
        RLock rLock = redissonClient.getLock("yupao:precachejob:docache:lock");
        // 只有一个线程能获取到
        try {
            if(rLock.tryLock(0,-1L, TimeUnit.MILLISECONDS)) {
                System.out.println("get lock" + Thread.currentThread().getId());
                QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                Page<User> userPage = userService.page(new Page<>(1, 20), queryWrapper);
                ValueOperations<String, Object> redisOpsOption = redisTemplate.opsForValue();
                for (Long userID : mainUserList) {
                    String rediskey = String.format("yupao:user:recommed:%s", userID);
                    try {
                        redisOpsOption.set(rediskey, userPage.getRecords().stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList()), 30000, TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        log.error("redis set error :", e);
                    }
                }
                System.out.println("定时任务执行完毕:");
            }else {
                System.out.println("Failed To get lock" + Thread.currentThread().getId());
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser error:", e);
        } finally {
            // 查看这个锁是不是当前线程添加的，需要放到 finally，保证无论 try 代码块是否报错，锁都会释放
            if(rLock.isHeldByCurrentThread()){
                System.out.println("unlock" + Thread.currentThread().getId());
                rLock.unlock();
            }
        }
    }
}
