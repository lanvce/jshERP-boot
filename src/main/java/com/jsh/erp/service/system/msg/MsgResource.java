package com.jsh.erp.service.system.msg;

import com.jsh.erp.service.common.ResourceInfo;

import java.lang.annotation.*;

/**
 * @author jishenghua qq752718920  2019-9-7 22:52:35
 */
@ResourceInfo(value = "msg")
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MsgResource {
}
