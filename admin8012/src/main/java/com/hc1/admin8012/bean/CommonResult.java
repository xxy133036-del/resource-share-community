package com.hc1.admin8012.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @Author xwj
 * @Description
 * @Date 2026-06-08 15:24:58
 * @Version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CommonResult<T> {
    private int code;
    private String message;
    private T data;

    public static <T> CommonResult<T> success(T data)
    {
        return new CommonResult<>(200,"操作成功",data);
    }
    public static <T> CommonResult<T> error()
    {
        return new CommonResult<>(500,"操作失败",null);
    }
    public static <T> CommonResult<T> error(String message)
    {
        return new CommonResult<>(500,message,null);
    }
    public static <T> CommonResult<T> error(int code,String message)
    {
        return new CommonResult<>(code,message,null);
    }


}
