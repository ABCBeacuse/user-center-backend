package com.example.yupao_backend.exception;


import com.example.yupao_backend.common.ErrorCode;

/**
 * 自定义异常处理类
 *
 * @author yupi
 */
public class BussinessException extends RuntimeException {

    /**
     * 继承 RuntimeException 运行时异常，运行时异常可以不在代码中显式的捕获，即这种异常我们不需要 throws 或者 try catch 来捕获它
     * 因为 RuntimeException 中的构造函数的参数不全，我们需要继承，来再次封装为全局异常处理类
     */

    private final int code;

    /**
     * description 比 message 更加详细的信息
     */
    private final String description;

    public BussinessException(String message, int code, String description) {
        super(message);
        this.code = code;
        this.description = description;
    }

    public BussinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = errorCode.getDescription();
    }

    /**
     *
     * @param errorCode
     * @param description 也可以自定义异常
     */
    public BussinessException(ErrorCode errorCode, String description) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = description;
    }

    /**
     * 不需要对异常里面的值进行变化，所有只需要 getter
     * @return
     */
    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
