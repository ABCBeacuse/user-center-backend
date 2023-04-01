package com.example.user_center.service;

import com.example.user_center.module.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class UserServiceTest {

    @Resource
    private UserService userService;


    @Test
    public void userInsert(){
        User user = new User();
        user.setUsername("aaa");
        user.setAvatarUrl("https://profile.csdnimg.cn/D/6/6/3_baixf");
        user.setGender(0);
        user.setUserPassword("123456");
        user.setUserAccount("admin");
        user.setPhone("132122222");
        user.setEmail("122222@qq.com");
        user.setUserStatus(0);

        boolean result = userService.save(user);
        System.out.println(user.getId());
        Assertions.assertEquals(true,result);
    }

    @Test
    void userRegister() {
        long newID = userService.userRegister("abcde", "12345678", "12345678","2222");
        if (newID > 0){
            System.out.println("成功插入");
        }else {
            System.out.println("失败");
        }
    }
}