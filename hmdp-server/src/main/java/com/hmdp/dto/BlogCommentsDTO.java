package com.hmdp.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlogCommentsDTO {

	private Long userId;

	private Long blogId;

	private String content;

	private LocalDateTime createTime;
}
