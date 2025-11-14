package com.xiaochen.mianshiya.job.cycle;

import cn.hutool.core.collection.CollUtil;
import com.xiaochen.mianshiya.annotation.DistributedLock;
//import com.xiaochen.mianshiya.esdao.PostEsDao;
//import com.xiaochen.mianshiya.esdao.QuestionEsDao;
import com.xiaochen.mianshiya.mapper.PostMapper;
import com.xiaochen.mianshiya.mapper.QuestionMapper;
import com.xiaochen.mianshiya.model.dto.post.PostEsDTO;
import com.xiaochen.mianshiya.model.dto.question.QuestionEsDTO;
import com.xiaochen.mianshiya.model.entity.Post;
import com.xiaochen.mianshiya.model.entity.Question;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 增量同步题目到 es
 *
 * @author <a href="https://github.com/lixiaochen">程序员鱼皮</a>
 * @from <a href="https://xiaochen.icu">编程导航知识星球</a>
 */
// todo 取消注释开启任务
//@Component
@Slf4j
public class IncSyncQuestionToEs {

    @Resource
    private QuestionMapper questionMapper;
//
//    @Resource
//    private QuestionEsDao questionEsDao;

    /**
     * 每分钟执行一次
     */
    @DistributedLock(waitTime = 15, timeUnit = TimeUnit.SECONDS)
    @Scheduled(fixedRate = 60 * 1000)
    public void run() {
        // 查询近 5 分钟内的数据
        Date fiveMinutesAgoDate = new Date(new Date().getTime() - 5 * 60 * 1000L);
        List<Question> questionList = questionMapper.listQuestionWithDelete(fiveMinutesAgoDate);
        if (CollUtil.isEmpty(questionList)) {
            log.info("no inc question");
            return;
        }
        List<QuestionEsDTO> questionEsDTOList = questionList.stream()
                .map(QuestionEsDTO::objToDto)
                .collect(Collectors.toList());
        final int pageSize = 500;
        int total = questionEsDTOList.size();
        log.info("IncSyncQuestionToEs start, total {}", total);
        for (int i = 0; i < total; i += pageSize) {
            int end = Math.min(i + pageSize, total);
            log.info("sync from {} to {}", i, end);
//            questionEsDao.saveAll(questionEsDTOList.subList(i, end));
        }
        log.info("IncSyncQuestionToEs end, total {}", total);
    }
}
