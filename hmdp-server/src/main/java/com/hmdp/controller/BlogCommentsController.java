package com.hmdp.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.PageResult;
import com.hmdp.dto.Result;
import com.hmdp.service.IBlogCommentsService;
import com.hmdp.service.IBlogService;
import com.hmdp.service.impl.BlogServiceImpl;
import com.hmdp.vo.BlogCommentsVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/blog-comments")
public class BlogCommentsController {

	@Resource
	private IBlogCommentsService blogCommentsService;

	@PostMapping
	public Result addComment(@RequestParam("comment") String comment,@RequestParam("blogId") Long blogId){
		return blogCommentsService.addComment(comment,blogId);
	}

	@GetMapping("/{id}")
	public Result getComments(
			@PathVariable("id") Long blogId,
			@RequestParam(value = "page", defaultValue = "1") Integer page,
			@RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {

		Page<BlogCommentsVO> pageResult = blogCommentsService.getComments(blogId, page, pageSize);
		return Result.ok(pageResult);
	}
	@GetMapping("/self")
	public Result getSelfComments(

			@RequestParam(value = "page", defaultValue = "1") Integer page,
			@RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize) {

		Page<BlogCommentsVO> pageResult = blogCommentsService.getSelfComments(page, pageSize);
		return Result.ok(pageResult);
	}
	@DeleteMapping("/{id}")
	public Result deleteCommentById(@PathVariable("id") Long id){
		return blogCommentsService.deleteCommentById(id);
	}

}
