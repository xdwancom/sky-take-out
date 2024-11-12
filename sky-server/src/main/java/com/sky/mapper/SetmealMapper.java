package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.pagehelper.Page;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Employee;
import com.sky.entity.Setmeal;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.poi.ss.formula.functions.T;

@Mapper
public interface SetmealMapper extends BaseMapper<Setmeal> {
    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    Page<SetmealVO> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);
}
