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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EmpServiceImpl implements EmpService {

    /** 新增员工时的初始密码 —— 与原数据库 emp.password 列的默认值保持一致 */
    private static final String INIT_PASSWORD = "123456";

    /**
     * 一个固定的合法 BCrypt 密文, 用于"用户名不存在"时的空跑比对, 见 login 方法注释。
     * 它对应的明文是一串随机字符, 不可能是任何真实密码, 因此永远不会比对成功。
     */
    private static final String DUMMY_PASSWORD = "$2a$10$n010KaBnalNmyIxokWzWZe914ueOePaCuoeC5LO229W8/cmbL9XrC";

    @Autowired
    private EmpMapper empMapper;
    @Autowired
    private EmpExprMapper empExprMapper;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private PasswordEncoder passwordEncoder;

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

        //密码加密存储: 统一使用初始密码, 不采纳客户端传来的 password
        //(原来靠数据库列的 default '123456' 兜底, 现在默认值已从表结构移除, 必须在这里显式设置)
        emp.setPassword(passwordEncoder.encode(INIT_PASSWORD));

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

        //密码加密后才能入库, 否则会把明文直接写进数据库。
        //当前前端没有"修改密码"入口, 不会传 password, 这里是防御性处理:
        //  传了 -> 加密后更新; 没传 -> 置 null, 配合 EmpMapper.xml 里的 <if> 跳过 password 列, 保持原密码不变
        String rawPassword = emp.getPassword();
        emp.setPassword(StringUtils.hasText(rawPassword) ? passwordEncoder.encode(rawPassword) : null);

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
        //1. 参数校验: 用户名或密码为空直接登录失败
        //(密码为 null 时 BCrypt 的 matches 会抛 IllegalArgumentException, 变成 500, 这里先挡掉)
        if(!StringUtils.hasText(emp.getUsername()) || !StringUtils.hasText(emp.getPassword())){
            return null;
        }

        //2. 根据用户名查询员工信息
        //   密码是 BCrypt 密文, 同一个密码每次加密结果都不同, 无法再作为 SQL 查询条件
        Emp e = empMapper.getByUsername(emp.getUsername());

        //3. 用户不存在时, 也空跑一次 BCrypt 比对再返回失败
        //   目的是让"用户名不存在"和"密码错误"的响应耗时基本一致;
        //   否则攻击者能靠响应时间差枚举出系统里存在哪些用户名
        if(e == null){
            passwordEncoder.matches(emp.getPassword(), DUMMY_PASSWORD);
            return null;
        }

        //4. 比对明文密码与库中密文(全程没有解密, BCrypt 是不可逆的)
        //   顺带一提: 如果数据库里还是旧的明文密码, 这里会比对失败并打一行
        //   "Encoded password does not look like BCrypt" 的告警 —— 即漏跑迁移脚本会立刻暴露, 不会静默放行
        if(!passwordEncoder.matches(emp.getPassword(), e.getPassword())){
            return null;
        }

        //5. 登录成功, 生成jwt令牌
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("id", e.getId());
        dataMap.put("username", e.getUsername());
        String jwt = jwtUtils.generateJwt(dataMap);//生成jwt令牌 - 存储id,username

        return new LoginInfo(e.getId(), e.getUsername(), e.getName(), jwt);
    }
}
