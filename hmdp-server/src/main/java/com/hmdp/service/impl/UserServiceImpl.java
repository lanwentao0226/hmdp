package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.PageResult;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.*;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IFollowService;
import com.hmdp.service.ISignService;
import com.hmdp.service.IUserInfoService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.UserHolder;
import com.hmdp.vo.BlogCommentsVO;
import com.hmdp.vo.FansVO;
import com.hmdp.vo.SignVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Resource
	private IFollowService followService;

	@Resource
	private IUserService userService;

	@Resource
	private IUserInfoService userInfoService;

	@Resource
	private ISignService signService;



	@Override
	public Result sendCode(String phone, HttpSession session) {
		if(RegexUtils.isPhoneInvalid(phone)){
			return Result.fail("手机号格式错误！");
		}
		String code = RandomUtil.randomNumbers(6);

		stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY+phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

		log.debug("发送短信验证码成功，验证码：{}",code);
		return Result.ok(code);
	}

	@Override
	public Result login(LoginFormDTO loginForm, HttpSession session) {
		String phone = loginForm.getPhone();
		if(RegexUtils.isPhoneInvalid(phone)){
			return Result.fail("手机号格式错误！");
		}
		String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
		String code = loginForm.getCode();
		if(cacheCode==null || !cacheCode.equals(code)){
			return Result.fail("验证码错误");
		}

		User user = query().eq("phone", phone).one();

		if(user == null){

			user=createUserWithPhone(phone);
		}
		String icon = user.getIcon();
		if ( icon == null || icon.trim().isEmpty()) {
			user.setIcon("/imgs/icons/default-icon.png");
		}
		String token= UUID.randomUUID().toString(true);
		String tokenKey=LOGIN_USER_KEY+token;

		UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
		userDTO.setLoginToken(tokenKey);

		Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
				CopyOptions.create()
						.setIgnoreNullValue(true)
						.setFieldValueEditor((fieldName, fieldValue) ->
								fieldValue != null ? fieldValue.toString() : null
						));


		stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);
		stringRedisTemplate.expire(tokenKey,LOGIN_USER_TTL,TimeUnit.MINUTES);
		System.out.println(token);
		return Result.ok(token);
	}

	@Override
	@Transactional
	public Result sign() {

		Long userId = UserHolder.getUser().getId();
		LocalDateTime now = LocalDateTime.now();

		String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
		String key = USER_SIGN_KEY + userId +keySuffix;

		int dayOfMonth = now.getDayOfMonth();
		stringRedisTemplate.opsForValue().setBit(key,dayOfMonth-1,true);

		int	count = countCoiled();
		UserInfo userInfo = userInfoService.query().eq("user_id",userId).one();
		if(userInfo==null){
			return Result.fail("签到失败");
		}
		boolean isSuccess = userInfoService.update().eq("user_id",userId).set("credits", count + userInfo.getCredits()).update();
		if(!isSuccess){
			return Result.fail("签到失败");
		}
		Sign sign = new Sign();
		sign.setUserId(userId);
		sign.setYear(LocalDateTime.now().getYear()); // 当前年份
		sign.setMonth(LocalDateTime.now().getMonthValue()); // 当前月份
		sign.setDate(java.sql.Date.valueOf(LocalDateTime.now().toLocalDate())); // 当前日期
		sign.setIsBackup(0); // 是否
		boolean save = signService.save(sign);
		if(!save){
			return Result.fail("签到失败");
		}

		return Result.ok(count);
	}

	public int countCoiled(){
		Long userId = UserHolder.getUser().getId();
		LocalDateTime now = LocalDateTime.now();

		String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
		String key = USER_SIGN_KEY + userId +keySuffix;

		int dayOfMonth = now.getDayOfMonth();
		List<Long> result = stringRedisTemplate.opsForValue().bitField(
				key,
				BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
						.valueAt(0)
		);
		if(result == null || result.isEmpty()){
			return 0;
		}
		Long num = result.get(0);
		if(num == null || num == 0){
			return 0;
		}

		int count =0;
		while (true){
			if((num & 1)==0){
				break;
			}else{
				count++;
			}
			num >>>= 1;
		}
		return count;
	}

	@Override
	public Result signCount() {
		int count = countCoiled();
		return Result.ok(count);
	}

	@Override
	public Result logout() {
		String tokenKey=UserHolder.getUser().getLoginToken();
		Boolean isSuccess = stringRedisTemplate.delete(tokenKey);
		if(isSuccess){
			UserHolder.removeUser();
			return Result.ok("退出登录成功");
		}
		else{
			return Result.fail("退出登录失败");
		}
	}

	@Override
	public Page<FansVO> getFans(Integer page, Integer pageSize) {
		Long userId = UserHolder.getUser().getId();
		if (userId == null || page < 1 || pageSize < 1) {
			throw new IllegalArgumentException("参数非法");
		}
		Page<Follow> pageParam = new Page<>(page, pageSize);

		// 2. 构建查询条件（查询指定用户的博客，按创建时间降序）
		LambdaQueryWrapper<Follow> queryWrapper = new LambdaQueryWrapper<Follow>()
				.eq(Follow::getFollowUserId, userId)
				.orderByDesc(Follow::getCreateTime);


		// 3. 执行分页查询（MyBatis-Plus 自动处理分页 SQL）
		Page<Follow> followPage = followService.page(pageParam, queryWrapper);

		List<FansVO> fansVOs = followPage.getRecords().stream()
				.map(this::convertToVO)
				.collect(Collectors.toList());
		// 5. 构建VO分页结果（保留原始分页信息）
		Page<FansVO> voPage = new Page<>();
		voPage.setCurrent(followPage.getCurrent());
		voPage.setSize(followPage.getSize());
		voPage.setTotal(followPage.getTotal());
		voPage.setPages(followPage.getPages());
		voPage.setRecords(fansVOs);

		return voPage;

	}

	private FansVO convertToVO(Follow follow) {
		Long userId = follow.getUserId();
		User user = userService.query().eq("id", userId).one();
		FansVO fansVO = new FansVO();

		fansVO.setId(userId);
		fansVO.setNickName(user.getNickName());
		fansVO.setIcon(user.getIcon());
		fansVO.setIsFollow(false);
		Boolean isSuccess = stringRedisTemplate.opsForSet().isMember("follows:" + follow.getFollowUserId(), follow.getUserId().toString());
		if(isSuccess){
			fansVO.setIsFollow(true);
		}
		return fansVO;
	}

	@Override
	public Long countFans() {
		Long userId = UserHolder.getUser().getId();
		if (userId == null) {
			throw new IllegalArgumentException("用户未登录");
		}
		String key = "fans:" + userId;
		Long size = stringRedisTemplate.opsForSet().size(key);
		return size;
	}

	@Override
	public Result me() {
		UserDTO user = UserHolder.getUser();
		if(user==null){
			return Result.fail("用户未登录");
		}
		String key=user.getLoginToken();
		Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
		UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), true);
		return Result.ok(userDTO);
	}


	private int signSum() {
		Long userId = UserHolder.getUser().getId();
		LocalDateTime now = LocalDateTime.now();

// 构建当前月份的签到key
		String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
		String key = USER_SIGN_KEY + userId + keySuffix;

// 获取当前月份的总天数
		int dayOfMonth = now.getDayOfMonth();
		int length = now.getMonth().length(now.toLocalDate().isLeapYear());

// 获取本月所有签到记录
		List<Long> result = stringRedisTemplate.opsForValue().bitField(
				key,
				BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(length)).valueAt(0)
		);

// 处理无签到记录的情况
		if (result == null || result.isEmpty() || result.get(0) == null) {
			return 0;
		}

		Long signData = result.get(0);
		int count = 0;

// 统计签到次数
		for (int i = 0; i < dayOfMonth; i++) {
			if ((signData & (1L << i)) != 0) {
				count++;
			}
		}

		return count;
	}
	private Boolean isSign(){
		Long userId = UserHolder.getUser().getId();
		LocalDateTime now = LocalDateTime.now();

		String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
		String key = USER_SIGN_KEY + userId +keySuffix;

		int dayOfMonth = now.getDayOfMonth();
		List<Long> result = stringRedisTemplate.opsForValue().bitField(
				key,
				BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(1))
						.valueAt(dayOfMonth-1)
		);
		if(result == null || result.isEmpty()){
			return false;
		}
		Long num = result.get(0);
		if(num == null || num == 0){
			return false;
		}
		return true;
	}


	@Override
	public Result getSignDetails() {
		SignVO signVO = new SignVO();
		signVO.setContinuousSign(countCoiled());
		signVO.setCumulativeSign(signSum());

		Long userId = UserHolder.getUser().getId();
		System.out.println(userId);
		UserInfo userInfo = userInfoService.query().eq("user_id", userId).one();
		if(userInfo==null){
			userInfo = new UserInfo();
			userInfo.setCredits(0);
			userInfo.setUserId(userId);
			userInfo.setUpdateTime(LocalDateTime.now());
			userInfo.setCreateTime(LocalDateTime.now());
			boolean save = userInfoService.save(userInfo);
			if(!save){
				return Result.fail("获取积分失败");
			}
		}
		QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
		wrapper.gt("credits", userInfo.getCredits());
		long count = userInfoService.count(wrapper);

		signVO.setCredits(userInfo.getCredits());
		signVO.setTop(count+1);
		signVO.setIsSign(isSign());


		return Result.ok(signVO);

	}

	@Override
	public List<Integer> getCheckedInDates(LocalDate date) {
		Long userId = UserHolder.getUser().getId();

		String keySuffix = date.format(DateTimeFormatter.ofPattern(":yyyyMM"));
		String key = USER_SIGN_KEY + userId + keySuffix;

		int length = date.getMonth().length(date.isLeapYear());

		List<Long> result = stringRedisTemplate.opsForValue().bitField(
				key,
				BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(length)).valueAt(0)
		);

		List<Integer> list = new ArrayList<>(Collections.nCopies(length, 0));
		if (result == null || result.isEmpty() || result.get(0) == null) {
			return list;
		}

		Long signData = result.get(0);
		int dayIndex;

		for (int i = 0; i < length; i++) {
			dayIndex = length - 1 - i; // 月底到月初的索引
			if ((signData & (1L << i)) != 0) {
				list.set(dayIndex, 1); // 设置对应日期为已签到
			}
		}

		return list;
	}

	@Transactional
	public User createUserWithPhone(String phone) {
		User user = new User();
		user.setPhone(phone);
		user.setNickName(USER_NICK_NAME_PREFIX+RandomUtil.randomString(10));
		save(user);

		UserInfo userInfo=new UserInfo();
		userInfo.setUserId(user.getId());
		userInfo.setCreateTime(LocalDateTime.now());
		userInfo.setUpdateTime(LocalDateTime.now());
		userInfoService.save(userInfo);

		return user;
	}
}
