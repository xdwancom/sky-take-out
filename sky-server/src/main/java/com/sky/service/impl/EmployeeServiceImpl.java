package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());//md5加密
        Employee employee = employeeMapper.selectOne(new QueryWrapper<Employee>().eq("username",username));//根据用户名查询数据库

        if (employee == null)
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);//账号不存在
        if (!password.equals(employee.getPassword()))
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);//密码错误
        if (employee.getStatus() == StatusConstant.DISABLE)
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);//账号被锁定

        return employee;
    }

    /**
     * 新增员工
     *
     * @param employeeDTO
     */
    public void save(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);//对象属性拷贝

        employee.setStatus(StatusConstant.ENABLE)//设置账号状态，默认1，1正常，0锁定
            .setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()))//设置密码，默认密码123456
            .setCreateTime(LocalDateTime.now())//设置当前记录的创建时间和修改时间
            .setUpdateTime(LocalDateTime.now())
            .setCreateUser(BaseContext.getCurrentId())//设置当前员工创建人id和修改人id
            .setUpdateUser(BaseContext.getCurrentId());

        BaseContext.removeCurrentId();//清理ThreadLocal
        employeeMapper.insert(employee);
    }

    /**
     * 分页查询
     *
     * @param employeePageQueryDTO
     * @return
     */
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        // select * from employee limit 0,10
        //开始分页查询
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());

        return null;
    }
}
