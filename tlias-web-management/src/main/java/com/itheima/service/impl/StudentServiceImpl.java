package com.itheima.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.excel.model.StudentModel;
import com.itheima.mapper.StudentMapper;
import com.itheima.pojo.ClazzCountOption;
import com.itheima.pojo.PageResult;
import com.itheima.pojo.Student;
import com.itheima.service.StudentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class StudentServiceImpl implements StudentService {

    @Autowired
    private StudentMapper studentMapper;

    @Override
    public void save(Student student) {
        student.setCreateTime(LocalDateTime.now());
        student.setUpdateTime(LocalDateTime.now());
        studentMapper.insert(student);
    }


    @Override
    public PageResult page(String name, Integer degree, Integer clazzId, Integer page, Integer pageSize) {
        PageHelper.startPage(page, pageSize);

        List<Student> studentList = studentMapper.list(name,degree,clazzId);
        Page<Student> p = (Page<Student>) studentList;

        return new PageResult(p.getTotal(), p.getResult());
    }


    @Override
    public Student getInfo(Integer id) {
        return studentMapper.getById(id);
    }


    @Override
    public void update(Student student) {
        student.setUpdateTime(LocalDateTime.now());
        studentMapper.update(student);
    }


    @Override
    public void delete(List<Integer> ids) {
        studentMapper.delete(ids);
    }


    @Override
    public void violationHandle(Integer id, Integer score) {
        studentMapper.updateViolation(id, score);
    }


    @Override
    public List<Map> getStudentDegreeData() {
        return studentMapper.countStudentDegreeData();
    }

    @Override
    public ClazzCountOption getStudentCountData() {
        List<Map<String, Object>> countList = studentMapper.getStudentCount();
        if(!CollectionUtils.isEmpty(countList)){
            List<Object> clazzList = countList.stream().map(map -> {
                return map.get("cname");
            }).toList();

            List<Object> dataList = countList.stream().map(map -> {
                return map.get("scount");
            }).toList();

            return new ClazzCountOption(clazzList, dataList);
        }
        return null;
    }

    @Override
    public void importStudents(Integer clazzId, List<StudentModel> studentModels) {
        if(!CollectionUtils.isEmpty(studentModels)){
            //1. 对上传上来的学员信息进行转换 List<Student>
            List<Student> studentList = studentModels.stream().map(sm -> {
                Student s = new Student();
                //赋值
                BeanUtil.copyProperties(sm, s);
                s.setGender(sm.getGenderStr().equals("男")?1:2);
                s.setIsCollege(sm.getIsCollegeStr().equals("是")?1:0);
                String degreeStr = sm.getDegreeStr();
                switch (degreeStr) {
                    case "初中" -> s.setDegree(1);
                    case "高中" -> s.setDegree(2);
                    case "大专" -> s.setDegree(3);
                    case "本科" -> s.setDegree(4);
                    case "硕士" -> s.setDegree(5);
                    case "博士" -> s.setDegree(6);
                }
                s.setClazzId(clazzId);
                s.setCreateTime(LocalDateTime.now());
                s.setUpdateTime(LocalDateTime.now());
                return s;
            }).toList();

            //2. 批量插入学员信息
            studentMapper.insertBatch(studentList);
        }
    }


    @Override
    public List<StudentModel> getExportStudents(Integer clazzId, String name, Integer degree) {
        //1. 调用mapper查询学员信息
        List<Student> studentList = studentMapper.list(name, degree, clazzId);
        //2. 转换数据
        if(!CollectionUtils.isEmpty(studentList)){
            List<StudentModel> studentModelList = studentList.stream().map(s -> {
                StudentModel sm = new StudentModel();
                BeanUtil.copyProperties(s, sm);
                sm.setGenderStr(s.getGender() == 1 ? "男" : "女");
                sm.setIsCollegeStr(s.getIsCollege() == 1 ? "是" : "否");
                switch (s.getDegree()) {
                    case 1 -> sm.setDegreeStr("初中");
                    case 2 -> sm.setDegreeStr("高中");
                    case 3 -> sm.setDegreeStr("大专");
                    case 4 -> sm.setDegreeStr("本科");
                    case 5 -> sm.setDegreeStr("硕士");
                    case 6 -> sm.setDegreeStr("博士");
                    default -> sm.setDegreeStr("");
                }
                return sm;
            }).toList();
            return studentModelList;
        }
        return new ArrayList<>();
    }
}
