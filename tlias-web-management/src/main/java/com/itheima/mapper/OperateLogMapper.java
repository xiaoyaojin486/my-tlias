package com.itheima.mapper;

import com.itheima.pojo.OperateLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OperateLogMapper {

    //插入日志数据
    @Insert("insert into operate_log (operate_emp_id, operate_time, class_name, method_name, method_params, return_value, cost_time) " +
            "values (#{operateEmpId}, #{operateTime}, #{className}, #{methodName}, #{methodParams}, #{returnValue}, #{costTime});")
    public void insert(OperateLog log);

    /**
     * 分页查询操作日志
     * operate_log 表只存操作人id, 不存姓名, 所以关联 emp 表把姓名查出来
     * 用 left join: 操作人被删除后日志仍需保留, 此时 operateEmpName 为 null
     * 按操作时间倒序, 时间相同时用 id 兜底, 保证分页结果顺序稳定
     */
    @Select("select ol.*, e.name as operate_emp_name " +
            "from operate_log ol left join emp e on e.id = ol.operate_emp_id " +
            "order by ol.operate_time desc, ol.id desc")
    List<OperateLog> selectPage();

}
