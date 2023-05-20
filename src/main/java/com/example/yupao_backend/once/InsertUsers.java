package com.example.yupao_backend.once;
import java.util.Date;

import com.example.yupao_backend.mapper.UserMapper;
import com.example.yupao_backend.module.domain.User;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;

@Component
public class InsertUsers {

    @Resource
    private UserMapper userMapper;

    /**
     * fixedRate = Long.MAX_VALUE 用来保证该定时任务只执行一次
     * StopWatch 为 spring 自带的一个工具类，用来记录程序执行时间
     */
//    @Scheduled(initialDelay = 5000, fixedRate = Long.MAX_VALUE)
    public void doInsertUsers(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
//        final int INSERT_NUM = 1000;
//        for (int i = 0; i < INSERT_NUM; i++) {
//            User user = new User();
//            user.setUsername("fakeYupi");
//            user.setAvatarUrl("https://thirdwx.qlogo.cn/mmopen/vi_32/KMPSJp38ibxXer96QwxNBtbFmXOWzOiaWpWKYdG9jJib04UVRVPIs1AroHEXf0GuZTAnp0AUNkfiagaYF2fl9eNVRw/132");
//            user.setGender(0);
//            user.setUserPassword("123456");
//            user.setUserAccount("");
//            user.setPhone("");
//            user.setEmail("");
//            user.setUserStatus(0);
//            user.setTags("");
//            user.setUserRole(0);
//            user.setPlanetCode("123412");
//            user.setUserProfile("");
//        }
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }
}
