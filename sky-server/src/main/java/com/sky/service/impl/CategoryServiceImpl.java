package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类业务层
 */
@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 新增分类
     * @param categoryDTO
     */
    public void save(CategoryDTO categoryDTO) {
        Category category = Category.builder()
                .status(StatusConstant.DISABLE)
                .build();
        BeanUtils.copyProperties(categoryDTO, category);
        categoryMapper.insert(category);
    }

    /**
     * mp分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        IPage<Category> page = new Page<>(categoryPageQueryDTO.getPage(), categoryPageQueryDTO.getPageSize());//创建分页对象
        QueryWrapper<Category> queryWrapper = new QueryWrapper<Category>()
                .like(categoryPageQueryDTO.getName() != null, "name", categoryPageQueryDTO.getName())//添加模糊查询条件
                .eq(categoryPageQueryDTO.getType() != null, "type", categoryPageQueryDTO.getType());//分类筛选
        IPage<Category> pageresult = categoryMapper.selectPage(page, queryWrapper);
        return new PageResult(pageresult.getTotal(), pageresult.getRecords());
    }

    /**
     * 根据id删除分类
     * @param id
     */
    public void deleteById(Long id) {
        if(dishMapper.selectCount(new QueryWrapper<Dish>().eq("category_id", id)) > 0)//查询当前分类是否关联了菜品，有则抛出业务异常
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);//当前分类下有菜品，不能删除
        if(setmealMapper.selectCount(new QueryWrapper<Setmeal>().eq("category_id", id)) > 0)//查询当前分类是否关联了套餐，有则抛出业务异常
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);//当前分类下有菜品，不能删除
        categoryMapper.deleteById(id);
    }

    /**
     * 修改分类
     * @param categoryDTO
     */
    public void update(CategoryDTO categoryDTO) {
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO,category);
        categoryMapper.updateById(category);
    }

    /**
     * 启用、禁用分类
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        categoryMapper.updateById(Category.builder().status(status).id(id).build());
    }

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    public List<Category> list(Integer type) {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Category::getStatus, 1)
                    .eq(type != null,Category::getType, type)
                    .orderByAsc(Category::getSort)
                    .orderByDesc(Category::getCreateTime);
        return categoryMapper.selectList(queryWrapper);
    }
}
