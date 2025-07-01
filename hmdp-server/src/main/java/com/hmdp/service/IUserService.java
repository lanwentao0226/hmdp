package com.hmdp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.PageResult;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.vo.FansVO;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IUserService extends IService<User> {

	Result sendCode(String phone, HttpSession session);

	Result login(LoginFormDTO loginForm, HttpSession session);

	Result sign();

	Result signCount();

	Result logout();

	Page<FansVO> getFans(Integer page, Integer pageSize);

	Long countFans();

	Result me();

	Result getSignDetails();

	List<Integer> getCheckedInDates(LocalDate date);
}
