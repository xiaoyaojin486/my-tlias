package com.itheima.mapper;

import com.itheima.pojo.EmpExpr;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 员工工作经历Mapper
 */
@Mapper
public interface EmpExprMapper {
    /**
     * 批量保存员工工作经历信息
     */
    void insertBatch(List<EmpExpr> exprList);

    /**
     * 根据员工的id批量删除工作经历信息
     */
    void deleteByEmpIds(List<Integer> ids);
}
