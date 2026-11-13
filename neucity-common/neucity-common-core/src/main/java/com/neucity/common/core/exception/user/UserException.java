package com.neucity.common.core.exception.user;

import com.neucity.common.core.exception.base.BaseException;

/**
 * 用户信息异常类
 * 
 * @author shupf
 */
public class UserException extends BaseException
{
    private static final long serialVersionUID = 1L;

    public UserException(String code, Object[] args)
    {
        super("user", code, args, null);
    }
}
