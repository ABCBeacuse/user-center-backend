package com.example.yupao_backend.once;

import com.alibaba.excel.EasyExcel;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 导入星球用户数据到数据库
 */
public class ImportXingQiuUser {
    public static void main(String[] args) {
        String fileName = "D:\\文件\\伙伴匹配系统\\练习\\back\\yupao_backend\\src\\main\\resources\\testExcel.xlsx";
        List<XingQiuUserInfo> userInfoList = EasyExcel.read(fileName).head(XingQiuUserInfo.class).sheet().doReadSync();
        System.out.println("总数" + userInfoList.size());
        /**
         * 根据 XingQiuUserInfo 的 username 属性来进行分组，XingQiuUserInfo::getUsername，:: 表示取这个类的属性
         */
        Map<String, List<XingQiuUserInfo>> listMap = userInfoList.stream().filter( userInfo -> StringUtils.isNoneEmpty(userInfo.getUsername())).collect(Collectors.groupingBy(XingQiuUserInfo::getUsername));
        System.out.println("不重复昵称" + listMap.keySet().size());

        /**
         * 遍历 Map
         */
        for (Map.Entry<String, List<XingQiuUserInfo>> stringListEntry : listMap.entrySet()){
            if(stringListEntry.getValue().size() >= 2) {
                System.out.println("username = " + stringListEntry.getKey());
            }
        }
    }
}
