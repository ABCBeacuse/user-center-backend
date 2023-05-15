package com.example.yupao_backend.module.enums;

/**
 * 队伍状态枚举类
 *
 * @author yupi
 */
public enum TeamStatusEnums {

    PUBLIC(0,"公开"),
    PRIVATE(1,"私有"),
    SECRET(2,"加密");

    public static TeamStatusEnums getEnumByValue(int value) {
        if(value < 0) {
            return null;
        }
        for(TeamStatusEnums teamStatusEnums : TeamStatusEnums.values()) {
            if(teamStatusEnums.getValue() == value) {
                return teamStatusEnums;
            }
        }
        return null;
    }

    private int value;

    private String name;

    TeamStatusEnums(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }
}
