package com.itheima.service;

import com.itheima.pojo.OperateLog;
import com.itheima.pojo.PageResult;

public interface OperateLogService {

    /**
     * 分页查询操作日志
     */
    PageResult<OperateLog> page(Integer page, Integer pageSize);
}
