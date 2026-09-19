package com.itheima.controller;

import com.itheima.anno.LogOperation;
import com.itheima.pojo.Emp;
import com.itheima.pojo.EmpQueryParam;
import com.itheima.pojo.PageResult;
import com.itheima.pojo.Result;
import com.itheima.service.EmpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * 员工管理的Controller
 */
@Slf4j
@RequestMapping("/emps")
@RestController
public class EmpController {

    @Autowired
    private EmpService empService;

    /**
     * 条件分页查询
     */
    @GetMapping
    public Result page(EmpQueryParam empQueryParam){
        log.info("分页查询, {}", empQueryParam);
        PageResult<Emp> pageResult = empService.page(empQueryParam);
        return Result.success(pageResult);
    }

    /**
     * 添加员工
     */
    @LogOperation
    @PostMapping
    public Result save(@RequestBody Emp emp) throws Exception {
        log.info("添加员工, {}", emp);
        empService.save(emp);
        return Result.success();
    }

    /**
     * 查询所有的员工
     */
    @GetMapping("/list")
    public Result list(){
        log.info("查询所有的员工数据");
        List<Emp> empList = empService.list();
        return Result.success(empList);
    }

    /**
     * 批量删除员工 /emps?ids=1,2,3
     */
    /*@DeleteMapping
    public Result delete(Integer[] ids){
        log.info("批量删除员工, {}", Arrays.toString(ids));
        return Result.success();
    }*/

    /**
     * 批量删除员工 /emps?ids=1,2,3
     */
    @LogOperation
    @DeleteMapping
    public Result delete(@RequestParam List<Integer> ids){
        log.info("批量删除员工, {}", ids);
        empService.delete(ids);
        return Result.success();
    }


    /**
     * 根据ID查询员工信息
     */
    @GetMapping("/{id}")
    public Result getById(@PathVariable Integer id){
        log.info("根据ID查询员工信息, id: {}", id);
        Emp emp = empService.getById(id);
        return Result.success(emp);
    }

    /**
     * 修改员工信息
     */
    @LogOperation
    @PutMapping
    public Result update(@RequestBody Emp emp){
        log.info("修改员工信息, {}", emp);
        empService.update(emp);
        return Result.success();
    }
}
