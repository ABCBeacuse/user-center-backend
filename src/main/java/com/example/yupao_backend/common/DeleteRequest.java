package com.example.yupao_backend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用的删除请求类
 */
@Data
public class DeleteRequest implements Serializable {

    private static final long serialVersionUID = -6138168551986804338L;

    /**
     * 删除的信息 id
     */
    private long id;
}
