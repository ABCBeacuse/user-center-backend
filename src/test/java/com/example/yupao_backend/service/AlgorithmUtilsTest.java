package com.example.yupao_backend.service;

import com.example.yupao_backend.utils.AlgorithmUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

/**
 * 算法工具类测试类
 *
 * @author yupi
 */
public class AlgorithmUtilsTest {

    @Test
    void Test() {
        String str1 = "鱼皮是狗";
        String str2 = "鱼皮不是狗";
        String str3 = "鱼皮是鱼不是狗";
        int result1 = AlgorithmUtils.minDistance(str1, str2);
        int result2 = AlgorithmUtils.minDistance(str1, str3);

        System.out.println(result1);
        System.out.println(result2);
    }

    @Test
    void Test2() {
        List<String> tagList1 = Arrays.asList("Java", "大一", "男");
        List<String> tagList2 = Arrays.asList("Java", "大二", "女");
        List<String> tagList3 = Arrays.asList("Python", "大三", "女");
        int result1 = AlgorithmUtils.minDistance(tagList1, tagList2);
        int result2 = AlgorithmUtils.minDistance(tagList1, tagList3);

        System.out.println(result1);
        System.out.println(result2);
    }
}
