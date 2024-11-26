package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.*;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    /**
     * 新增菜品和对应的口味
     *
     * @param dishDTO
     */
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {

        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        //向菜品表插入1条数据
        dishMapper.insert(dish);

        //获取insert语句生成的主键值
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (!flavors.isEmpty()) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            //向口味表插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * PageHelper菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);//后绪步骤实现
        return new PageResult(page.getTotal(), page.getResult());
    }
//    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {//mp分页查询
//        IPage<Dish> page = new Page<>(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());//创建分页对象
//        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<Dish>()
//                .like(dishPageQueryDTO.getName() != null, Dish::getName, dishPageQueryDTO.getName()) // 添加模糊查询条件
//                .eq(dishPageQueryDTO.getCategoryId() != null, Dish::getCategoryId, dishPageQueryDTO.getCategoryId()) // 分类id筛选
//                .eq(dishPageQueryDTO.getStatus() != null, Dish::getStatus, dishPageQueryDTO.getStatus()); // 状态筛选
//        IPage<Dish> pageresult = dishMapper.selectPage(page, lambdaQueryWrapper);
//        return new PageResult(pageresult.getTotal(), pageresult.getRecords());
//    }

    /**
     * 菜品批量删除
     *
     * @param ids
     */
    @Transactional//事务
    public void deleteBatch(List<Long> ids) {

        for (Long id : ids)//判断当前菜品列表是否有在售菜品
            if (dishMapper.selectById(id).getStatus() == StatusConstant.ENABLE)
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);//当前菜品处于起售中，不能删除

        List<SetmealDish> setmealDishList = setmealDishMapper.selectList(new LambdaQueryWrapper<SetmealDish>().in(SetmealDish::getDishId, ids));
        if (setmealDishList != null && setmealDishList.size() > 0)//判断当前菜品列表是否有被套餐关联
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);//当前菜品被套餐关联了，不能删除

        for (Long id : ids) {
            dishMapper.deleteById(id);//删除菜品表中的菜品
            dishFlavorMapper.delete(new LambdaQueryWrapper<DishFlavor>().eq(DishFlavor::getDishId, id));//删除菜品口味表关联的口味
        }
    }

    /**
     * 根据id查询菜品和对应的口味数据
     *
     * @param id
     * @return
     */
    public DishVO getByIdWithFlavor(Long id) {
        //根据id查询菜品数据
        Dish dish = dishMapper.selectById(id);
        //根据dish的id(对应口味表中dish_id)查询菜品对应口味数据
        List<DishFlavor> dishFlavors = dishFlavorMapper.selectList(new LambdaQueryWrapper<DishFlavor>().eq(DishFlavor::getDishId, id));

        DishVO dishVO = DishVO.builder().flavors(dishFlavors).build();//添加口味数据
        BeanUtils.copyProperties(dish, dishVO);//将查询到的数据复制到VO

        return dishVO;
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    public List<Dish> list(Long categoryId) {
        return dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(categoryId!=null,Dish::getCategoryId, categoryId)
                .eq(Dish::getStatus, StatusConstant.ENABLE));
    }

    /**
     * 根据id修改菜品和对应的口味数据
     *
     * @param dishDTO
     */
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.updateById(dish);//根据菜品id修改菜品数据
        dishFlavorMapper.delete(new LambdaQueryWrapper<DishFlavor>().eq(DishFlavor::getDishId, dishDTO.getId()));//删除菜品口味表关联的口味

        //重新插入口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());
            });
            dishFlavorMapper.insertBatch(flavors);//向口味表插入n条数据
        }
    }

    /**
     * 菜品起售停售
     *
     * @param status
     * @param id
     */
    @Transactional
    public void startOrStop(Integer status, Long id) {
        dishMapper.updateById(Dish.builder().status(status).id(id).build());//菜品状态更改
//        dishMapper.update(null, new LambdaUpdateWrapper<Dish>()
//                .eq(Dish::getId, id)
//                .set(Dish::getStatus, status));
        if (status == StatusConstant.DISABLE) {//如果是停售操作，包含该菜品的套餐也要停售
            List<SetmealDish> setmealDishList = setmealDishMapper.selectList(new LambdaQueryWrapper<SetmealDish>().in(SetmealDish::getDishId, id));//查询含有该菜品的的套餐数据
            if (!setmealDishList.isEmpty())
                for (SetmealDish setmealDish : setmealDishList)
                    setmealMapper.updateById(Setmeal.builder().status(status).id(setmealDish.getSetmealId()).build());//套餐状态更改
        }
    }



    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .like(dish.getName() != null, Dish::getName, dish.getName())
                .eq(dish.getCategoryId()!=null,Dish::getCategoryId, dish.getCategoryId())
                .eq(dish.getStatus()!=null,Dish::getStatus, dish.getStatus()));

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);
            //根据dish的id(对应口味表中dish_id)查询菜品对应口味数据
            List<DishFlavor> dishFlavors = dishFlavorMapper.selectList(new LambdaQueryWrapper<DishFlavor>().eq(DishFlavor::getDishId, d.getId()));

            dishVO.setFlavors(dishFlavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }
}