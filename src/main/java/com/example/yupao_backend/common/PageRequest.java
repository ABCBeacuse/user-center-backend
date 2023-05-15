package com.example.yupao_backend.common;


import lombok.Data;

import java.io.Serializable;

/**
 * 通用的分页请求类
 *
 * @author yupi
 */
@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 8023369120951915574L;

    /**
     * 页面大小
     */
    protected int pageSize = 10;

    /**
     * 当前是第几页
     */
    protected int pageNum = 1;
}
