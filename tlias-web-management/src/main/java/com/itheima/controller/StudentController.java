package com.itheima.controller;

import com.alibaba.excel.EasyExcel;
import com.itheima.excel.model.StudentDataListener;
import com.itheima.excel.model.StudentModel;
import com.itheima.pojo.PageResult;
import com.itheima.pojo.Result;
import com.itheima.pojo.Student;
import com.itheima.service.StudentService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    /**
     * 添加学生
     */
    @PostMapping
    public Result save(@RequestBody Student student){
        studentService.save(student);
        return Result.success();
    }

    /**
     * 条件分页查询
     */
    @GetMapping
    public Result page(String name ,
                       Integer degree,
                       Integer clazzId,
                       @RequestParam(defaultValue = "1") Integer page ,
                       @RequestParam(defaultValue = "10") Integer pageSize){
        PageResult pageResult = studentService.page(name,degree,clazzId,page,pageSize);
        return Result.success(pageResult);
    }

    /**
     * 根据ID查询学生信息
     */
    @GetMapping("/{id}")
    public Result getInfo(@PathVariable Integer id){
        Student student = studentService.getInfo(id);
        return Result.success(student);
    }

    /**
     * 修改学生信息
     */
    @PutMapping
    public Result update(@RequestBody Student student){
        studentService.update(student);
        return Result.success();
    }

    /**
     * 删除学生信息
     */
    @DeleteMapping("/{ids}")
    public Result delete(@PathVariable List<Integer> ids){
        studentService.delete(ids);
        return Result.success();
    }

    /**
     * 违纪处理
     */
    @PutMapping("/violation/{id}/{score}")
    public Result violationHandle(@PathVariable Integer id , @PathVariable Integer score){
        studentService.violationHandle(id, score);
        return Result.success();
    }


    /**
     * 批量导入学员信息
     */
    @PostMapping("/import/{clazzId}")
    public Result importStudents(@PathVariable Integer clazzId, MultipartFile file) throws Exception {
        log.info("批量导入学员信息, clazzId:{}, file:{}", clazzId, file.getOriginalFilename());
        // 读取Excel数据
        EasyExcel.read(file.getInputStream(), StudentModel.class, new StudentDataListener(studentService, clazzId))
                .sheet()
                .doRead();
        return Result.success();
    }

    /**
     * 批量导出学员信息 - 下载
     */
    @GetMapping("/export")
    public void exportStudents(HttpServletResponse response, Integer clazzId, String name, Integer degree) throws Exception {
        log.info("批量导出学员信息, clazzId:{}, name:{}, degree:{}", clazzId, name, degree);
        //1. 获取符合条件的学员数据列表
        List<StudentModel> sList = studentService.getExportStudents(clazzId, name, degree);

        //2. 基于EasyExcel导出Excel文件
        EasyExcel.write(response.getOutputStream(), StudentModel.class)
                .sheet("学员信息列表")
                .doWrite(sList);
    }

}
