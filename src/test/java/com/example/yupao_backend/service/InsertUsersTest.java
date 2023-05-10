package com.example.yupao_backend.service;

import com.example.yupao_backend.module.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@SpringBootTest
public class InsertUsersTest {

    @Resource
    private UserService userService;

    // param_1. 核心线程数量(始终默认同时运行多少线程) , param_2. 最大同时运行多少个线程, param_3. 线程的存活时间, param_4. 时间单位(10分钟), param_5. 任务队列(长度为 10000，可以往里面放 10000 个任务),
    // param_6. 什么情况下会超过 60 这个默认线程数，任务队列满了，肯定需要更多的人来干活，这个时候就会增加更多的线程。就会大于60，慢慢增加到1000
    // 如果同时运行的线程已经加到1000了，但是任务还是很多，还是忙不过来，任务队列还是满的。
    // 可以指定 param_6. 任务策略，即现在干不完活了，现在要怎么处理这些任务。 默认策略为 拒绝，会抛异常。
    private ExecutorService executorService = new ThreadPoolExecutor(60, 1000, 10000, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));
    /**
     * StopWatch 为 spring 自带的一个工具类，用来记录程序执行时间
     */
    @Test
    public void doInsertUsers(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 1000;
        List<User> userList = new ArrayList<>();
        for (int i = 0; i < INSERT_NUM; i++) {
            User user = new User();
            user.setUsername("假鱼皮");
            user.setAvatarUrl("https://thirdwx.qlogo.cn/mmopen/vi_32/KMPSJp38ibxXer96QwxNBtbFmXOWzOiaWpWKYdG9jJib04UVRVPIs1AroHEXf0GuZTAnp0AUNkfiagaYF2fl9eNVRw/132");
            user.setGender(0);
            user.setUserPassword("12345678");
            user.setUserAccount("fakeYupi00" + i);
            user.setPhone("123");
            user.setEmail("123@qq.com");
            user.setUserStatus(0);
            user.setTags("[]");
            user.setUserRole(0);
            user.setPlanetCode("123412");

            userList.add(user);
        }
        // 每个 SqlSession 插入 100 条数据
        userService.saveBatch(userList, 100);
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }

    @Test
    public void doConcurrencyInsertUsers(){
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final int INSERT_NUM = 100000;
        int j = 0;
        // 定义任务数组
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            List<User> userList = new ArrayList<>();
            while (true){
                User user = new User();
                user.setUsername("假鱼皮");
                user.setAvatarUrl("https://thirdwx.qlogo.cn/mmopen/vi_32/KMPSJp38ibxXer96QwxNBtbFmXOWzOiaWpWKYdG9jJib04UVRVPIs1AroHEXf0GuZTAnp0AUNkfiagaYF2fl9eNVRw/132");
                user.setGender(0);
                user.setUserPassword("12345678");
                user.setUserAccount("fakeYupi00e" + j);
                user.setPhone("123");
                user.setEmail("123@qq.com");
                user.setUserStatus(0);
                user.setTags("[]");
                user.setUserRole(0);
                user.setPlanetCode("123412");
                j++;

                userList.add(user);
                if( j % 15000 == 0){
                    break;
                }
            }
            // 新建一个异步任务, 第二个参数可以指定一个线程池, 如果不指定的话默认就是 Java 自带的 focujoinpool（并发度有限）,没有返回值就是 Void
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                System.out.println("TheardName: " + Thread.currentThread().getName());
                userService.saveBatch(userList, 10000);
            }, executorService);
            futureList.add(future);
        }
        // 拿到了十个异步任务, 如果不添加 join(), 还是异步的。程序走完了, 可能还没有添加完毕. 加上 join 后, 会等添加完毕之后再执行下一行代码。
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        stopWatch.stop();
        System.out.println(stopWatch.getTotalTimeMillis());
    }
}
