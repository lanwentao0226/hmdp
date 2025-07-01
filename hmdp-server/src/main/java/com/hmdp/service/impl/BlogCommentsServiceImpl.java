package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.hmdp.dto.BlogCommentsDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.BlogComments;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogCommentsMapper;
import com.hmdp.service.IBlogCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IBlogService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.UserHolder;
import com.hmdp.vo.BlogCommentsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.awt.print.Pageable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;


import static com.hmdp.utils.RedisConstants.BLOG_COMMENTS_KEY;



/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Resource
	private IBlogService blogService;

	@Resource(name = "taskExecutor") // 使用Spring管理的线程池
	private ExecutorService taskExecutor;

	@Resource
	private IUserService userService;

	@Override
	@Transactional
	public Result addComment(String comment, Long blogId) {
		// 构建评论对象
		BlogCommentsDTO blogCommentsDTO = new BlogCommentsDTO();
		blogCommentsDTO.setBlogId(blogId);
		blogCommentsDTO.setContent(comment);
		blogCommentsDTO.setUserId(UserHolder.getUser().getId());
		blogCommentsDTO.setCreateTime(LocalDateTime.now());

		BlogComments blogComments = BeanUtil.copyProperties(blogCommentsDTO, BlogComments.class);
		boolean isSuccess = save(blogComments);
		boolean update = blogService.update().eq("id", blogId).setSql("comments=comments+1").update();

		if (isSuccess && update) {
			return Result.ok();
		}

		return Result.fail("添加评论失败！");
	}

	@Override
	public Page<BlogCommentsVO> getComments(Long blogId, Integer page, Integer pageSize) {
		// 参数校验
		if (blogId == null || page < 1 || pageSize < 1) {
			throw new IllegalArgumentException("博客ID、页码或每页大小参数非法");
		}
		Page<BlogComments> pageParam = new Page<>(page, pageSize);

		// 2. 构建查询条件（查询指定用户的博客，按创建时间降序）
		LambdaQueryWrapper<BlogComments> queryWrapper = new LambdaQueryWrapper<BlogComments>()
				.eq(BlogComments::getBlogId, blogId)
				.orderByDesc(BlogComments::getCreateTime);

		// 3. 执行分页查询（MyBatis-Plus 自动处理分页 SQL）
		Page<BlogComments> commentPage = this.page(pageParam, queryWrapper);

		List<BlogCommentsVO> commentVOs = commentPage.getRecords().stream()
				.map(this::convertToVO1)
				.collect(Collectors.toList());
		// 5. 构建VO分页结果（保留原始分页信息）
		Page<BlogCommentsVO> voPage = new Page<>();
		voPage.setCurrent(commentPage.getCurrent());
		voPage.setSize(commentPage.getSize());
		voPage.setTotal(commentPage.getTotal());
		voPage.setPages(commentPage.getPages());
		voPage.setRecords(commentVOs);

		return voPage;

	}

	private BlogCommentsVO convertToVO1(BlogComments comment) {
		BlogCommentsVO vo = new BlogCommentsVO();
		BeanUtils.copyProperties(comment, vo);

		User user = userService.query().eq("id", comment.getUserId()).one();
		vo.setIcon(user.getIcon());
		vo.setName(user.getNickName());
		return vo;
	}


	@Override
	public Page<BlogCommentsVO> getSelfComments(Integer page, Integer pageSize) {
		Long userId = UserHolder.getUser().getId();
		Page<BlogComments> pageParam = new Page<>(page, pageSize);

		// 2. 构建查询条件（查询指定用户的博客，按创建时间降序）
		LambdaQueryWrapper<BlogComments> queryWrapper = new LambdaQueryWrapper<BlogComments>()
				.eq(BlogComments::getUserId, userId)
				.orderByDesc(BlogComments::getCreateTime);

		// 3. 执行分页查询（MyBatis-Plus 自动处理分页 SQL）
		Page<BlogComments> commentPage = this.page(pageParam, queryWrapper);

		List<BlogCommentsVO> commentVOs = commentPage.getRecords().stream()
				.map(this::convertToVO)
				.collect(Collectors.toList());

		// 5. 构建VO分页结果（保留原始分页信息）
		Page<BlogCommentsVO> voPage = new Page<>();
		voPage.setCurrent(commentPage.getCurrent());
		voPage.setSize(commentPage.getSize());
		voPage.setTotal(commentPage.getTotal());
		voPage.setPages(commentPage.getPages());
		voPage.setRecords(commentVOs);

		return voPage;
	}

	@Override
	@Transactional
	public Result deleteCommentById(Long id) {
		BlogComments blogComments = getById(id);
		if(blogComments==null){
			return Result.fail("评论不存在");
		}

		UserDTO user = UserHolder.getUser();
		if (!Objects.equals(blogComments.getUserId(), user.getId())) {
			return Result.fail("无权限删除此博客");
		}

		boolean isSuccess = removeById(id);
		boolean update = blogService.update().eq("id", blogComments.getBlogId()).setSql("comments=comments-1").update();
		if(isSuccess && update){
			return Result.ok();

		}
		return Result.fail("删除评论失败");
	}

	private BlogCommentsVO convertToVO(BlogComments comment) {
		BlogCommentsVO vo = new BlogCommentsVO();
		BeanUtils.copyProperties(comment, vo);
		return vo;
	}


}
