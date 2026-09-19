package com.itheima.service;

import com.itheima.pojo.*;

import java.util.List;
import java.util.Map;


public interface EmpService {
    /**
     * 分页查询
     */
    PageResult<Emp> page(EmpQueryParam empQueryParam);

    /**
     * 保存员工信息
     */
    void save(Emp emp) throws Exception;

    /**
     * 查询所有的员工数据
     */
    List<Emp> list();

    /**
     * 批量删除员工
     */
    void delete(List<Integer> ids);

    /**
     * 根据id查询员工信息
     */
    Emp getById(Integer id);

    /**
     * 更新员工信息
     */
    void update(Emp emp);

    /**
     * 统计员工职位人数
     */
    JobOption getEmpJobData();

    /**
     * 统计员工性别
     */
    List<Map<String, Object>> getEmpGenderData();

    /**
     * 登录
     */
    LoginInfo login(Emp emp);
}
