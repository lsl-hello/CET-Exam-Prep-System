package com.cet6.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cet6.common.Result;
import com.cet6.entity.SysNotice;
import com.cet6.mapper.SysNoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicController {

    private final SysNoticeMapper noticeMapper;

    @GetMapping("/home")
    public Result<Map<String, Object>> home() {
        Map<String, Object> data = new HashMap<>();
        data.put("title", "大学英语四六级刷题背单词系统");
        data.put("intro", "面向四六级考生的在线刷题与背单词平台，涵盖听力、阅读、翻译、写作四大题型，"
                + "支持艾宾浩斯记忆曲线复习、错题自动收录、薄弱知识点分析与每日学习计划提醒。");
        data.put("examInfo", Map.of(
                "name", "大学英语四六级考试（CET-4/CET-6）",
                "frequency", "每年6月、12月",
                "duration", "四级125分钟、六级130分钟",
                "sections", List.of("听力", "阅读", "翻译", "写作")
        ));
        List<SysNotice> notices = noticeMapper.selectList(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getStatus, 1)
                .orderByDesc(SysNotice::getSort)
                .last("LIMIT 5"));
        data.put("notices", notices);
        return Result.success(data);
    }
}
