package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.pagehelper.Page;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;


@Mapper
public interface DishMapper extends BaseMapper<Dish> {
    /**
     * 菜品分页查询+左连接填充
     *
     * @param dishPageQueryDTO
     * @return
     */
    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 根据套餐id查询菜品,并根据在售状态升序排序
     * @param setmealId
     * @return
     */
    @Select("select a.* from dish a left join setmeal_dish b on a.id = b.dish_id where b.setmeal_id = #{setmealId} order by a.status asc")
    List<Dish> getBySetmealId(Long setmealId);
}
