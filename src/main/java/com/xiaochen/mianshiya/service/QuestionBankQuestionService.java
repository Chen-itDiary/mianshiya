package com.xiaochen.mianshiya.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xiaochen.mianshiya.model.dto.questionbankquestion.QuestionBankQuestionQueryRequest;
import com.xiaochen.mianshiya.model.entity.QuestionBankQuestion;
import com.xiaochen.mianshiya.model.entity.User;
import com.xiaochen.mianshiya.model.vo.QuestionBankQuestionVO;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 题库题目关联服务
 *
 * @author <a href="https://github.com/lixiaochen">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
public interface QuestionBankQuestionService extends IService<QuestionBankQuestion> {

    /**
     * 校验数据
     *
     * @param questionBankQuestion
     * @param add                  对创建的数据进行校验
     */
    void validQuestionBankQuestion(QuestionBankQuestion questionBankQuestion, boolean add);

    /**
     * 获取查询条件
     *
     * @param questionBankQuestionQueryRequest
     * @return
     */
    QueryWrapper<QuestionBankQuestion> getQueryWrapper(QuestionBankQuestionQueryRequest questionBankQuestionQueryRequest);

    /**
     * 获取题库题目关联封装
     *
     * @param questionBankQuestion
     * @param request
     * @return
     */
    QuestionBankQuestionVO getQuestionBankQuestionVO(QuestionBankQuestion questionBankQuestion, HttpServletRequest request);

    /**
     * 分页获取题库题目关联封装
     *
     * @param questionBankQuestionPage
     * @param request
     * @return
     */
    Page<QuestionBankQuestionVO> getQuestionBankQuestionVOPage(Page<QuestionBankQuestion> questionBankQuestionPage, HttpServletRequest request);

    /**
     * 批量添加题目到题库
     *
     * @param questionIdLIst
     * @param questionBankId
     * @param loginUser
     */

    void batchAddQuestionsToBank(List<Long> questionIdLIst, Long questionBankId, User loginUser);

    /**
     * 批量添加题目到题库（事务）
     *
     * @param questionBankQuestions
     */
    @Transactional(rollbackFor = Exception.class)
    void batchAddQuestionsToBankInner(List<QuestionBankQuestion> questionBankQuestions);

    /**
     * 批量移除题目从题库
     *
     * @param questionIdLIst
     * @param questionBankId
     */
    Integer batchRemoveQuestionsFromBank(List<Long> questionIdLIst, Long questionBankId);

    /**
     * 批量删除题目从题库（事务）
     *
     * @param questionBankQuestions
     */
    @Transactional(rollbackFor = Exception.class)
    Integer batchRemoveQuestionsToBankInner(List<Long> questionBankQuestions, Long questionBankId);
}
