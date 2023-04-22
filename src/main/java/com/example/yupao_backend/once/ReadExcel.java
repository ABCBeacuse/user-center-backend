package com.example.yupao_backend.once;

import com.alibaba.excel.EasyExcel;

import java.util.List;
import java.util.Map;


public class ReadExcel {

    /**
     * 读取数据
     */
    public static void main(String[] args) {
        String fileName = "D:\\文件\\伙伴匹配系统\\练习\\back\\yupao_backend\\src\\main\\resources\\testExcel.xlsx";
        readExcelByListener(fileName);
    }

    /**
     * 监听器读取
     * @param fileName
     */
    public static void readExcelByListener(String fileName) {
        EasyExcel.read(fileName, XingQiuUserInfo.class, new TableListener()).sheet().doRead();
    }

    /**
     * 同步读取
     * @param fileName
     */
    public static void synchronousRead(String fileName) {
        // 这里 需要指定读用哪个class去读，然后读取第一个sheet 同步读取会自动finish
        List<XingQiuUserInfo> list = EasyExcel.read(fileName).head(XingQiuUserInfo.class).sheet().doReadSync();
        for (XingQiuUserInfo data : list) {
        }
        // 这里 也可以不指定class，返回一个list，然后读取第一个sheet 同步读取会自动finish
        List<Map<Integer, String>> listMap = EasyExcel.read(fileName).sheet().doReadSync();
        for (Map<Integer, String> data : listMap) {
        }
    }
}
