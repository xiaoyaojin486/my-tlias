package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class Emp {
    private Integer id; //ID,主键
    private String username; //用户名

    /**
     * 密码(数据库中存 BCrypt 密文)
     * WRITE_ONLY: 只允许反序列化(接收登录请求传来的明文密码), 序列化时一律不输出
     *             —— 保证 /emps 列表、/emps/{id} 等接口不会把密码(明文或密文)返回给前端
     * ToString.Exclude: Lombok 生成的 toString 不含密码
     *             —— 避免日志和操作日志(method_params)里出现密码
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    private String password; //密码
    private String name; //姓名
    private Integer gender; //性别, 1:男, 2:女
    private String phone; //手机号
    private Integer job; //职位, 1:班主任,2:讲师,3:学工主管,4:教研主管,5:咨询师
    private Integer salary; //薪资
    private String image; //头像
    private LocalDate entryDate; //入职日期
    private Integer deptId; //关联的部门ID
    private LocalDateTime createTime; //创建时间
    private LocalDateTime updateTime; //修改时间

    //封装部门名称数
    private String deptName; //部门名称
    //封装员工的工作经历集合
    private List<EmpExpr> exprList;
}
