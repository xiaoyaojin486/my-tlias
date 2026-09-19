package com.itheima.service;

import com.itheima.excel.model.StudentModel;
import com.itheima.pojo.ClazzCountOption;
import com.itheima.pojo.PageResult;
import com.itheima.pojo.Student;
import java.util.List;
import java.util.Map;

public interface StudentService {

    /**
     * 添加学生信息
     */
    void save(Student student);

    /**
     * 条件分页查询
     */
    PageResult page(String name, Integer degree, Integer clazzId, Integer page, Integer pageSize);

    /**
     * 根据ID查询学生信息
     */
    Student getInfo(Integer id);

    /**
     * 修改学生信息
     */
    void update(Student student);

    /**
     * 删除学生信息
     */
    void delete(List<Integer> ids);

    /**
     * 违纪处理
     */
    void violationHandle(Integer id, Integer score);

    /**
     * 统计学历人数
     */
    List<Map> getStudentDegreeData();

    /**
     * 统计班级人数
     */
    ClazzCountOption getStudentCountData();

    /**
     * 导入学生信息
     */
    void importStudents(Integer clazzId, List<StudentModel> studentModels);

    /**
     * 获取要导出的学生信息
     */
    List<StudentModel> getExportStudents(Integer clazzId, String name, Integer degree);
}
