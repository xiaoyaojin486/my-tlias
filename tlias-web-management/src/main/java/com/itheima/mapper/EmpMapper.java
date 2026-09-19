package com.itheima.mapper;

import com.itheima.pojo.Emp;
import com.itheima.pojo.EmpQueryParam;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 操作员工信息的Mapper
 */
@Mapper
public interface EmpMapper {
    /**
     * 查询员工信息
     */
    //@Select("select emp.*, dept.name as dept_name from emp left join dept on emp.dept_id = dept.id")
    //public List<Emp> list(String name, Integer gender, LocalDate begin, LocalDate end);

    public List<Emp> list(EmpQueryParam empQueryParam);

    /**
     * 保存员工信息 - insert
     * 主键返回 --> 获取到插入这条数据的主键.
     */
    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("insert into emp(username, password, name, gender, image, job, entry_date, phone, salary, dept_id, create_time, update_time) " +
            "values (#{username}, #{password}, #{name}, #{gender}, #{image}, #{job}, #{entryDate}, #{phone}, #{salary}, #{deptId}, #{createTime}, #{updateTime})")
    void insert(Emp emp);

    /**
     * 批量删除员工信息
     */
    void deleteByIds(List<Integer> ids);

    /**
     * 根据id查询员工信息(基本信息, 工作经历信息)
     */
    Emp getById(Integer id);

    /**
     * 根据ID更新员工的基本信息
     */
    void update(Emp emp);

    /**
     * 统计员工职位人数
     */
    @MapKey("pos")
    List<Map> getEmpJobData();

    /**
     * 统计员工性别
     */
    @MapKey("name")
    List<Map<String, Object>> getEmpGenderData();

    /**
     * 根据部门id查询员工人数
     */
    @Select("select count(*) from emp where dept_id = #{deptId}")
    Integer countByDeptId(Integer deptId);

    /**
     * 查询所有的图像访问路径
     */
    @Select("select distinct image from emp where image != '' and image is not null")
    List<String> listFiles();

    /**
     * 根据用户名查询员工信息(用于登录)
     * <p>
     * 注意: 不能再写成 "where username = ? and password = ?"。
     * BCrypt 每次加密都会生成新的随机盐, 同一个密码两次加密得到的密文完全不同,
     * 所以密码无法作为 SQL 查询条件, 只能先按用户名把密文取出来, 再在 Java 里用
     * PasswordEncoder.matches(明文, 密文) 比对。
     */
    @Select("select * from emp where username = #{username}")
    Emp getByUsername(String username);
}
