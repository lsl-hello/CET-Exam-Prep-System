package com.cet6.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cet6.common.Constants;
import com.cet6.entity.UserMessage;
import com.cet6.entity.UserStudyPlan;
import com.cet6.entity.UserWordRecord;
import com.cet6.entity.UserQuestionRecord;
import com.cet6.mapper.UserMessageMapper;
import com.cet6.mapper.UserStudyPlanMapper;
import com.cet6.mapper.UserWordRecordMapper;
import com.cet6.mapper.UserQuestionRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StudyRemindTask {

    private final UserStudyPlanMapper planMapper;
    private final UserMessageMapper messageMapper;
    private final UserWordRecordMapper wordRecordMapper;
    private final UserQuestionRecordMapper questionRecordMapper;

    /** 每分钟检查一次，匹配 remind_time 发送提醒 */
    @Scheduled(cron = "0 * * * * ?")
    public void checkAndRemind() {
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        DayOfWeek day = LocalDate.now().getDayOfWeek();
        boolean isWorkday = day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;

        List<UserStudyPlan> plans = planMapper.selectList(new LambdaQueryWrapper<UserStudyPlan>()
                .eq(UserStudyPlan::getStatus, 1)
                .eq(UserStudyPlan::getRemindTime, now));

        for (UserStudyPlan plan : plans) {
            if (plan.getCycleType() == 2 && !isWorkday) {
                continue;
            }
            if (isPlanCompleted(plan)) {
                continue;
            }
            long exists = messageMapper.selectCount(new LambdaQueryWrapper<UserMessage>()
                    .eq(UserMessage::getUserId, plan.getUserId())
                    .eq(UserMessage::getMsgType, 1)
                    .likeRight(UserMessage::getTitle, plan.getPlanName())
                    .ge(UserMessage::getCreateTime, LocalDate.now().atStartOfDay()));
            if (exists > 0) {
                continue;
            }
            UserMessage msg = new UserMessage();
            msg.setUserId(plan.getUserId());
            msg.setMsgType(1);
            msg.setTitle(plan.getPlanName() + " - 今日任务提醒");
            if (plan.getPlanType() == Constants.PLAN_WORD) {
                int target = plan.getDailyTargetNum() != null ? plan.getDailyTargetNum() : 0;
                msg.setContent("你今日的「" + plan.getPlanName() + "」还未完成，目标 "
                        + target + " 个单词，快来背单词吧！");
            } else {
                msg.setContent("你今日的「" + plan.getPlanName() + "」还未完成，快来刷题吧！");
            }
            msg.setIsRead(0);
            messageMapper.insert(msg);
            log.info("发送学习提醒给用户 {}", plan.getUserId());
        }
    }

    private boolean isPlanCompleted(UserStudyPlan plan) {
        LocalDate today = LocalDate.now();
        if (plan.getPlanType() == Constants.PLAN_WORD) {
            long count = wordRecordMapper.selectCount(new LambdaQueryWrapper<UserWordRecord>()
                    .eq(UserWordRecord::getUserId, plan.getUserId())
                    .eq(UserWordRecord::getReviewDate, today));
            int target = plan.getDailyTargetNum() != null && plan.getDailyTargetNum() > 0
                    ? plan.getDailyTargetNum() : Integer.MAX_VALUE;
            return count >= target;
        } else {
            // 刷题计划：今日有作答记录即视为开始；dailyTargetNum 为空时只要没记录就提醒
            long count = questionRecordMapper.selectCount(new LambdaQueryWrapper<UserQuestionRecord>()
                    .eq(UserQuestionRecord::getUserId, plan.getUserId())
                    .ge(UserQuestionRecord::getAnswerTime, today.atStartOfDay())
                    .lt(UserQuestionRecord::getAnswerTime, today.plusDays(1).atStartOfDay()));
            int target = plan.getDailyTargetNum() != null && plan.getDailyTargetNum() > 0
                    ? plan.getDailyTargetNum() : 1;
            return count >= target;
        }
    }
}
