package com.hmdp.controller;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.PageResult;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.entity.UserInfo;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import com.hmdp.vo.BlogCommentsVO;
import com.hmdp.vo.FansVO;
import com.hmdp.vo.UserInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        //  发送短信验证码并保存验证码
        return userService.sendCode(phone,session);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        //  实现登录功能
        return userService.login(loginForm,session);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(){
        return userService.logout();
    }

    @GetMapping("/me")
    public Result me(){
        // 获取当前登录的用户并返回
        return userService.me();
    }
    @GetMapping("/fans")
    public Result getFans(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {

        Page<FansVO> pageResult = userService.getFans(page, pageSize);
        return Result.ok(pageResult);
    }
    @GetMapping("/fans/count")
    public Result countFans() {
        Long count=userService.countFans();
        return Result.ok(count);
    }

    @GetMapping("/userInfo")
    public Result getUserInfo(){
        return userInfoService.getUserInfo();
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }
    // UserController 根据id查询用户

    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId){
        // 查询详情
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 返回
        return Result.ok(userDTO);
    }
    @PutMapping("/updateInfo")
    public Result updateInfo(@RequestBody UserInfoVO userInfoVO){
        return userInfoService.updateInfo(userInfoVO);
    }

    /**
     * 用户签到功能
     * @return
     */
    @PostMapping("/sign")
    public Result sign(){
        return userService.sign();
    }
    @GetMapping("/signDetails")
    public Result getSignDetails(){
        return userService.getSignDetails();
    }
    @GetMapping("/getCheckedInDates")
    public Result getCheckedInDates(@RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date){
        List<Integer> list=userService.getCheckedInDates(date);
        return Result.ok(list);
    }

    /**
     * 统计本月连续签到
     * @return
     */
    @GetMapping("/sign/countCoiled")
    public Result signCount(){
        return userService.signCount();
    }



}
