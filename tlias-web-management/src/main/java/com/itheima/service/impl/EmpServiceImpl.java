package com.itheima.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.mapper.EmpExprMapper;
import com.itheima.mapper.EmpMapper;
import com.itheima.pojo.*;
import com.itheima.service.EmpLogService;
import com.itheima.service.EmpService;
import com.itheima.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmpServiceImpl implements EmpService {

    @Autowired
    private EmpMapper empMapper;
    @Autowired
    private EmpExprMapper empExprMapper;

    @Override
    public PageResult<Emp> page(EmpQueryParam empQueryParam) {
        //1. 设置分页参数
        PageHelper.startPage(empQueryParam.getPage(), empQueryParam.getPageSize());

        //2. 执行查询
        List<Emp> empList = empMapper.list(empQueryParam);
        Page<Emp> p = (Page<Emp>) empList;

        //3. 封装分页结果
        return new PageResult<>(p.getTotal(), p.getResult());
    }

    @Autowired
    private EmpLogService empLogService;

    @Transactional(rollbackFor = Exception.class) //开启事务管理 - rollbackFor: 抛出什么异常回滚事务
    @Override
    public void save(Emp emp) throws Exception {
        //1. 保存员工的基本信息
        emp.setCreateTime(LocalDateTime.now());
        emp.setUpdateTime(LocalDateTime.now());
        empMapper.insert(emp);
        log.info("保存员工信息, {}", emp);

        //2. 保存员工的工作经历信息 --> 属于上面刚添加的员工的
        List<EmpExpr> exprList = emp.getExprList();
        if(exprList != null && !exprList.isEmpty()){ //有工作经历
            //遍历集合，设置员工id
            exprList.forEach(expr -> {
                expr.setEmpId(emp.getId());
            });
            empExprMapper.insertBatch(exprList);
        }
    }

    @Override
    public List<Emp> list() {
        return empMapper.list(null);
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(List<Integer> ids) {
        //1. 删除员工的基本信息
        empMapper.deleteByIds(ids);

        //2. 删除员工的工作经历信息
        empExprMapper.deleteByEmpIds(ids);
    }

    @Override
    public Emp getById(Integer id) {
        return empMapper.getById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void update(Emp emp) {
        //1. 根据ID更新员工基本信息
        emp.setUpdateTime(LocalDateTime.now());
        empMapper.update(emp);

        //2. 更新员工的工作经历信息(先删除, 再添加)
        //2.1 根据员工的id删除员工的工作经历
        empExprMapper.deleteByEmpIds(Arrays.asList(emp.getId()));
        //2.2 再添加员工的工作经历 (批量)
        List<EmpExpr> exprList = emp.getExprList();
        if(exprList != null && !exprList.isEmpty()){ //有工作经历
            //遍历集合，设置员工id
            exprList.forEach(expr -> {
                expr.setEmpId(emp.getId());
            });
            empExprMapper.insertBatch(exprList);
        }
    }

    @Override
    public JobOption getEmpJobData() {
        // [{pos=教研主管, num=1} {pos=讲师, num=14}  {pos=学工主管, num=1} ...]
        List<Map> jobDataList = empMapper.getEmpJobData();

        // 组装数据 JobOption
        List jobList = jobDataList.stream().map(map -> {
            return map.get("pos");
        }).toList();

        List dataList = jobDataList.stream().map(map -> {
            return map.get("num");
        }).toList();
        return new JobOption(jobList, dataList);
    }


    @Override
    public List<Map<String, Object>> getEmpGenderData() {
        List<Map<String, Object>> mapList = empMapper.getEmpGenderData();
        return mapList;
    }

    @Override
    public LoginInfo login(Emp emp) {
        //1. 查询数据库, 根据用户名和密码查询员工信息
        Emp e = empMapper.getByUsernameAndPassword(emp);

        //2. 判断员工是否存在, 如果存在, 则说明登录成功; 封装返回结果
        if(e != null){
            //生成jwt令牌
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("id", e.getId());
            dataMap.put("username", e.getUsername());
            String jwt = JwtUtils.generateJwt(dataMap);//生成jwt令牌 - 存储id,username

            return new LoginInfo(e.getId(), e.getUsername(), e.getName(), jwt);
        }
        return null;
    }
}
