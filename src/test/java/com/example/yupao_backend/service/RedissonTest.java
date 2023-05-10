package com.example.yupao_backend.service;

import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class RedissonTest {

    @Resource
    private RedissonClient redissonClient;

    @Test
    void redissonTest(){
        // list, 存储在本地 JVM 中
        List<String> list = new ArrayList<>();
        list.add("yupi");
        list.get(0);
        list.remove(0);

        // 存储在 redis 中
        RList<String> rList = redissonClient.getList("test-list");
        rList.add("yupi");
        System.out.println("rList:" + rList.get(0));
        rList.remove(0);

        // map
        Map<String, Object> map = new HashMap<>();
        map.put("yupi", 10);
        map.get("yupi");

        RMap<Object, Object> rMap = redissonClient.getMap("test-map");
        rMap.put("yupi", 10);
        // set

        // stack
    }
}
