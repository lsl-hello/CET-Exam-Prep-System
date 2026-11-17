package com.cet6.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cet6.common.Result;
import com.cet6.entity.SysNotice;
import com.cet6.mapper.SysNoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final SysNoticeMapper noticeMapper;

    @GetMapping("/list")
    public Result<List<SysNotice>> list() {
        return Result.success(noticeMapper.selectList(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getStatus, 1)
                .orderByDesc(SysNotice::getSort)));
    }

    @GetMapping("/{id}")
    public Result<SysNotice> detail(@PathVariable Long id) {
        return Result.success(noticeMapper.selectById(id));
    }
}
