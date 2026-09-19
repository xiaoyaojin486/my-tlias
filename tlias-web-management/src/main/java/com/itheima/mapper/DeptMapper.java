package com.itheima.mapper;

import com.itheima.pojo.Dept;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper // 标识当前接口是一个Mybatis的Mapper接口 ---> 实现类对象 --> IOC容器
public interface DeptMapper {

    /**
     * 查询所有部门数据
     */
    //方式一: 手动封装
    //@Results({
    //    @Result(column = "create_time", property = "createTime"),
    //    @Result(column = "update_time", property = "updateTime")
    //})
    //@Select("SELECT id, name, create_time, update_time FROM dept ORDER BY update_time DESC")

    //方式二: 起别名
    //@Select("SELECT id, name, create_time createTime, update_time updateTime FROM dept ORDER BY update_time DESC")

    @Select("SELECT id, name, create_time, update_time FROM dept ORDER BY update_time DESC")
    List<Dept> list();

    /**
     * 根据id删除部门
     */
    @Delete("DELETE FROM dept WHERE id = #{id}")
    void deleteById(Integer id);

    /**
     * 插入部门数据
     */
    @Insert("INSERT INTO dept(name, create_time, update_time) VALUES(#{name}, #{createTime}, #{updateTime})")
    void insert(Dept dept);

    /**
     * 根据id查询部门详情
     */
    @Select("SELECT id, name, create_time, update_time FROM dept WHERE id = #{id}")
    Dept getById(Integer id);

    /**
     * 根据ID更新部门数据
     */
    @Update("UPDATE dept SET name = #{name}, update_time = #{updateTime} WHERE id = #{id}")
    void update(Dept dept);
}
