package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 套餐业务实现
 */
@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDTO
     */
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        System.out.println(setmeal);
        //向套餐表插入数据
        setmealMapper.insert(setmeal);

        //获取生成的套餐id
        Long setmealId = setmeal.getId();

        List<SetmealDish> setmealDishList = setmealDTO.getSetmealDishes();
        setmealDishList.forEach(setmealDish -> {//设置每个SetmealDish的setmealId
            setmealDish.setSetmealId(setmealId);
        });

        //保存套餐和菜品的关联关系
        for (SetmealDish setmealDish : setmealDishList)
            setmealDishMapper.insert(setmealDish);
    }

    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        int pageNum = setmealPageQueryDTO.getPage();
        int pageSize = setmealPageQueryDTO.getPageSize();

        PageHelper.startPage(pageNum, pageSize);
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        ids.forEach(id -> {
            if(setmealMapper.selectById(id).getStatus() == StatusConstant.ENABLE)//判断当前套餐状态，在售则不能删除
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
        });

        ids.forEach(setmealId -> {
            setmealMapper.deleteById(setmealId);//删除套餐表中的数据
            setmealDishMapper.delete(new LambdaQueryWrapper<SetmealDish>().eq(SetmealDish::getSetmealId, setmealId));//删除套餐菜品关系表中的套餐和菜品关联数据
        });
    }

    /**
     * 根据id查询套餐和套餐菜品关系
     *
     * @param id
     * @return
     */
    public SetmealVO getByIdWithDish(Long id) {
        Setmeal setmeal = setmealMapper.selectById(id);
        List<SetmealDish> setmealDishList = setmealDishMapper.selectList(new LambdaQueryWrapper<SetmealDish>().eq(SetmealDish::getSetmealId, id));//查询含有该套餐的套餐菜品数据

        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishList);

        return setmealVO;
    }

    /**
     * 修改套餐
     *
     * @param setmealDTO
     */
    @Transactional
    public void update(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmealMapper.updateById(setmeal);//修改套餐数据

        Long setmealId = setmealDTO.getId();//获取套餐id
        setmealDishMapper.delete(new LambdaQueryWrapper<SetmealDish>().eq(SetmealDish::getSetmealId, setmealId));//删除套餐菜品关系表中的套餐和菜品关联数据
        List<SetmealDish> setmealDishList = setmealDTO.getSetmealDishes();//获取套餐菜品关系
        setmealDishList.forEach(setmealDish -> {//重新设置新的套餐菜品关系
            setmealDish.setSetmealId(setmealId);
        });

        //保存套餐和菜品的新关系
        for (SetmealDish setmealDish : setmealDishList)
            setmealDishMapper.insert(setmealDish);
    }

    /**
     * 套餐起售停售
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        if(status == StatusConstant.ENABLE){//起售套餐时，若套餐内有停售菜品，则抛出异常提示"无法启售"
            List<Dish> dishList = dishMapper.getBySetmealId(id);//获取含有该套餐的菜品,并按在售状态升序排序(只需检测第一个是否停售则可)
            if(dishList.get(0).getStatus() == StatusConstant.DISABLE)//若第一个菜品为停售
                throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);//则抛出异常
        }

        setmealMapper.updateById(Setmeal.builder().status(status).id(id).build());//套餐状态更改
    }
}
