package com.example.user_center.common;

/**
 * 错误码
 *
 * @author yupi
 */
public enum ErrorCode {

    /**
     * 400 是 Http 的状态码，是由于明显的客户端错误 bad request 意思是 "错误的请求"
     * 前端提交数据的字段名称或者是字段类型和后台的实体类不一致，导致无法封装
     * 前端提交的到后台的数据应该是 json 字符串类型，而前端没有将对象转化为字符串类型
     * 加上 00 是业务码，400 表示是客户端发送的请求错误，00 是更加具体的状态码
     * 401 错误代表用户没有访问权限，需要进行身份认证
     */
    SUCCESS(200,"ok",""),
    PARAMS_ERROR(40000, "请求参数错误",""),
    NULL_ERROR(40001,"请求数据为空",""),
    NOT_LOGIN(40100,"未登录",""),
    NOT_AUTH(40101,"无权限",""),
    SYSTEM_ERROR(50000,"系统内部错误","")
    ;

    private final int code;

    /**
     * 状态码信息
     */
    private final String message;

    /**
     * 状态码描述
     */
    private final String description;

    ErrorCode(int code, String message, String description) {
        this.code = code;
        this.message = message;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }
}
