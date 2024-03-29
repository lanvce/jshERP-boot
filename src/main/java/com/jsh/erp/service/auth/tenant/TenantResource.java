package com.jsh.erp.service.auth.tenant;

import com.jsh.erp.service.common.ResourceInfo;

import java.lang.annotation.*;

/**
 * @author jishenghua qq752718920  2019-6-27 22:56:56
 */
@ResourceInfo(value = "tenant")
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantResource {
}
