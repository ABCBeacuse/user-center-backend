package com.example.yupao_backend.service;

import com.example.yupao_backend.module.domain.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

@SpringBootTest
public class RedisTest {

    @Resource
    // 可以直接引入，因为引入了 spring-boot-strater，引入之后会自动生成一个操作 redis 的对象。
    private RedisTemplate redisTemplate;

    @Test
    public void test(){
        // 获取一个操作 Redis String 字符串数据结构的集合
        ValueOperations valueOperations = redisTemplate.opsForValue();
        // 增
        valueOperations.set("yupiString", "dog");
        valueOperations.set("yupiInt", 1);
        valueOperations.set("yupiDouble", 2.0);
        User user = new User();
        user.setId(1L);
        user.setUsername("yupi");
        valueOperations.set("yupiUser",user);
        // 查
        Object yupi = valueOperations.get("yupiString");
        Assertions.assertTrue("dog".equals((String) yupi));
        yupi = valueOperations.get("yupiInt");
        // yupi 直接这样强转可能出现空指针异常
        Assertions.assertTrue(1 == (Integer) yupi);
        yupi = valueOperations.get("yupiDouble");
        Assertions.assertTrue(2.0 == (Double) yupi);
        yupi = valueOperations.get("yupiUser");
        System.out.println(yupi);
        // 改就是重复 set
        // 删除
        redisTemplate.delete("yupiUser");
    }
}
