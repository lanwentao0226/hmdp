package com.hmdp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> {
	private int currentPage;    // 当前页码
	private int pageSize;       // 每页大小
	private long totalRecords;  // 总记录数
	private List<T> records;    // 当前页数据
}
