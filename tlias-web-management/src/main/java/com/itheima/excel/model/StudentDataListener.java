package com.itheima.excel.model;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.itheima.service.StudentService;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;

/**
 * EasyExcel中类,是不能被Spring管理的.
 */
@Slf4j
public class StudentDataListener implements ReadListener<StudentModel> {
    private List<StudentModel> studentModels = new ArrayList<>();

    private Integer clazzId;
    private StudentService studentService;
    public StudentDataListener(StudentService studentService, Integer clazzId) {
        this.studentService = studentService;
        this.clazzId = clazzId;
    }
    //每读取一行数据, 会调用一次invoke方法
    @Override
    public void invoke(StudentModel studentModel, AnalysisContext analysisContext) {
        log.info("每读取一行数据: {}", studentModel);
        studentModels.add(studentModel);
    }
    //全部读取完成后, 会调用一次doAfterAllAnalysed方法
    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        studentService.importStudents(clazzId, studentModels);//将解析后的数据插入数据库
    }
}
