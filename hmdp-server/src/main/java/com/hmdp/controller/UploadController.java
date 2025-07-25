package com.hmdp.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.Result;
import com.hmdp.utils.AliOssUtil;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("upload")
public class UploadController {

    @Resource
    private AliOssUtil aliOssUtil;

    @PostMapping("blog")
    public Result uploadImage(@RequestParam("file") MultipartFile image) {
        try {
            // 获取原始文件名称
            String originalFilename = image.getOriginalFilename();
            // 生成新文件名
//            String fileName = createNewFileName(originalFilename);
            String filename= UUID.randomUUID()+originalFilename.substring(originalFilename.lastIndexOf("."));
            // 保存文件
//             image.transferTo(new File(SystemConstants.IMAGE_UPLOAD_DIR, fileName));
            String url = aliOssUtil.upload(filename, image.getInputStream());

            // 返回结果
            log.debug("文件上传成功，{}", url);
            return Result.ok(url);
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @GetMapping("/blog/delete")
    public Result deleteBlogImg(@RequestParam("name") String filename) {
        File file = new File(SystemConstants.IMAGE_UPLOAD_DIR, filename);
        if (file.isDirectory()) {
            return Result.fail("错误的文件名称");
        }
        FileUtil.del(file);
        return Result.ok();
    }

//    private String createNewFileName(String originalFilename) {
//        // 获取后缀
//        String suffix = StrUtil.subAfter(originalFilename, ".", true);
//        // 生成目录
//        String name = UUID.randomUUID().toString();
//        int hash = name.hashCode();
//        int d1 = hash & 0xF;
//        int d2 = (hash >> 4) & 0xF;
//        // 判断目录是否存在
//        File dir = new File(SystemConstants.IMAGE_UPLOAD_DIR, StrUtil.format("/blogs/{}/{}", d1, d2));
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }
//        // 生成文件名
//        return StrUtil.format("/blogs/{}/{}/{}.{}", d1, d2, name, suffix);
//    }
}
