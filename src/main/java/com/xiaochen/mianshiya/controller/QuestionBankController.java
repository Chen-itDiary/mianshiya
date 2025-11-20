package com.xiaochen.mianshiya.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.Tracer;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaochen.mianshiya.annotation.AuthCheck;
import com.xiaochen.mianshiya.annotation.HotKeyCheck;
import com.xiaochen.mianshiya.common.BaseResponse;
import com.xiaochen.mianshiya.common.DeleteRequest;
import com.xiaochen.mianshiya.common.ErrorCode;
import com.xiaochen.mianshiya.common.ResultUtils;
import com.xiaochen.mianshiya.constant.UserConstant;
import com.xiaochen.mianshiya.exception.BusinessException;
import com.xiaochen.mianshiya.exception.ThrowUtils;
import com.xiaochen.mianshiya.model.dto.question.QuestionQueryRequest;
import com.xiaochen.mianshiya.model.dto.questionbank.QuestionBankAddRequest;
import com.xiaochen.mianshiya.model.dto.questionbank.QuestionBankEditRequest;
import com.xiaochen.mianshiya.model.dto.questionbank.QuestionBankQueryRequest;
import com.xiaochen.mianshiya.model.dto.questionbank.QuestionBankUpdateRequest;
import com.xiaochen.mianshiya.model.entity.Question;
import com.xiaochen.mianshiya.model.entity.QuestionBank;
import com.xiaochen.mianshiya.model.entity.User;
import com.xiaochen.mianshiya.model.vo.QuestionBankVO;
import com.xiaochen.mianshiya.service.QuestionBankService;
import com.xiaochen.mianshiya.service.QuestionService;
import com.xiaochen.mianshiya.service.UserService;
import com.xiaochen.sentinelHandle.SentinelHandle;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 题库接口
 *
 * @author <a href="https://github.com/lixiaochen">程序员鱼皮</a>
 * @from <a href="https://www.code-nav.cn">编程导航学习圈</a>
 */
@RestController
@RequestMapping("/questionBank")
@Slf4j
public class QuestionBankController {

    @Resource
    private QuestionBankService questionBankService;

    @Resource
    private UserService userService;

    @Resource
    private QuestionService questionService;
    // region 增删改查
    @Resource
    private RedissonClient redisClient;
    /**
     * 创建题库
     *
     * @param questionBankAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addQuestionBank(@RequestBody QuestionBankAddRequest questionBankAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(questionBankAddRequest == null, ErrorCode.PARAMS_ERROR);
        // todo 在此处将实体类和 DTO 进行转换
        QuestionBank questionBank = new QuestionBank();
        BeanUtils.copyProperties(questionBankAddRequest, questionBank);
        // 数据校验
        questionBankService.validQuestionBank(questionBank, true);
        // todo 填充默认值
        User loginUser = userService.getLoginUser(request);
        questionBank.setUserId(loginUser.getId());
        // 写入数据库
        boolean result = questionBankService.save(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newQuestionBankId = questionBank.getId();
        return ResultUtils.success(newQuestionBankId);
    }

    /**
     * 删除题库
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteQuestionBank(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        QuestionBank oldQuestionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldQuestionBank.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionBankService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新题库（仅管理员可用）
     *
     * @param questionBankUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateQuestionBank(@RequestBody QuestionBankUpdateRequest questionBankUpdateRequest) {
        if (questionBankUpdateRequest == null || questionBankUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        QuestionBank questionBank = new QuestionBank();
        BeanUtils.copyProperties(questionBankUpdateRequest, questionBank);
        // 数据校验
        questionBankService.validQuestionBank(questionBank, false);
        // 判断是否存在
        long id = questionBankUpdateRequest.getId();
        QuestionBank oldQuestionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = questionBankService.updateById(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取题库（封装类）
     *
     * @param questionBankQueryRequest
     * @return
     */
    @HotKeyCheck(hotKey = "'bank_detail_' + #questionBankQueryRequest.id")
    @GetMapping("/get/vo")
    public BaseResponse<QuestionBankVO> getQuestionBankVOById(QuestionBankQueryRequest questionBankQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(questionBankQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = questionBankQueryRequest.getId();
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        // 生成 Key
//        String key = "bank_detail_" + id   ;
//        RBucket<QuestionBankVO> bankQuestionbucket = redisClient.getBucket(key);
//        // 如果是热 Key
//        if(JdHotKeyStore.isHotKey(key)){  // 计数
//            QuestionBankVO questionBankVO = bankQuestionbucket.get();
//            if(questionBankVO != null){
//                return ResultUtils.success(questionBankVO);
//            }
//            Object cachedQuestionBankVO = JdHotKeyStore.get(key);
//            if(cachedQuestionBankVO != null){
//                return ResultUtils.success((QuestionBankVO) cachedQuestionBankVO);
//            }
//        }
        // 查询数据库
        QuestionBank questionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(questionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        QuestionBankVO questionBankVO = questionBankService.getQuestionBankVO(questionBank, request);
        //根据是否有需要，关联查询题目列表
        if (questionBankQueryRequest.getNeedQueryQuestionList()) {
            QuestionQueryRequest questionQueryRequest = new QuestionQueryRequest();
            questionQueryRequest.setQuestionBankId(id);
            Page<Question> questionPage = questionService.listQuestionByPage(questionQueryRequest);
            questionBankVO.setQuestionPage(questionPage);
        }
//        if(JdHotKeyStore.isHotKey(key)){
//            bankQuestionbucket.set(questionBankVO, 60, TimeUnit.SECONDS);
//        }
        // 设置本地缓存
//        JdHotKeyStore.smartSet(key, questionBankVO);
        return ResultUtils.success(questionBankVO);
    }

    /**
     * 分页获取题库列表（仅管理员可用）
     *
     * @param questionBankQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<QuestionBank>> listQuestionBankByPage(@RequestBody QuestionBankQueryRequest questionBankQueryRequest) {
        long current = questionBankQueryRequest.getCurrent();
        long size = questionBankQueryRequest.getPageSize();
        // 查询数据库
        Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current, size),
                questionBankService.getQueryWrapper(questionBankQueryRequest));
        return ResultUtils.success(questionBankPage);
    }

    /**
     * 分页获取题库列表（封装类）
     *
     * @param questionBankQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    @SentinelResource(value = "listQuestionBankVOByPage",
            blockHandler = "handleBlockException",
            blockHandlerClass = SentinelHandle.class,
            fallback = "handleFallback",
            fallbackClass = SentinelHandle.class
    )
    public BaseResponse<Page<QuestionBankVO>> listQuestionBankVOByPage(
            @RequestBody QuestionBankQueryRequest questionBankQueryRequest,
            HttpServletRequest request) {
        long current = questionBankQueryRequest.getCurrent();
        long size = questionBankQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current, size),
                questionBankService.getQueryWrapper(questionBankQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionBankService.getQuestionBankVOPage(questionBankPage, request));
    }

    /**
     * 分页获取题库列表（封装类） --限流 ip 版
     *
     * @param questionBankQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo/sentinel")
    public BaseResponse<Page<QuestionBankVO>> listQuestionBankVOByPageSentinel(
            @RequestBody QuestionBankQueryRequest questionBankQueryRequest,
            HttpServletRequest request) {
        long current = questionBankQueryRequest.getCurrent();
        long size = questionBankQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        String remoteAddr = request.getRemoteAddr();
        Entry entry = null;
        try{
            entry = SphU.entry("listQuestionBankVOByPage", EntryType.IN, 1, remoteAddr);
            // 查询数据库
            Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current, size),
                    questionBankService.getQueryWrapper(questionBankQueryRequest));
            // 获取封装类
            return ResultUtils.success(questionBankService.getQuestionBankVOPage(questionBankPage, request));
        }catch (Throwable ex){
            // 业务异常
            if(!BlockException.isBlockException(ex)){
                Tracer.trace(ex);
                return ResultUtils.error(ErrorCode.SYSTEM_ERROR,"这是业务异常...");

            }
            // 降级操作
            if(ex instanceof DegradeException) {
                return SentinelHandle.handleFallback(questionBankQueryRequest, request, ex);
            }
            // 限流操作
            return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统访问频繁，请稍后再试...");
        }finally {
            if(entry != null){
                entry.exit(1, remoteAddr);
            }
        }

    }

    /**
     * 分页获取当前登录用户创建的题库列表
     *
     * @param questionBankQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<QuestionBankVO>> listMyQuestionBankVOByPage(@RequestBody QuestionBankQueryRequest questionBankQueryRequest,
                                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(questionBankQueryRequest == null, ErrorCode.PARAMS_ERROR);
        // 补充查询条件，只查询当前登录用户的数据
        User loginUser = userService.getLoginUser(request);
        questionBankQueryRequest.setUserId(loginUser.getId());
        long current = questionBankQueryRequest.getCurrent();
        long size = questionBankQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<QuestionBank> questionBankPage = questionBankService.page(new Page<>(current, size),
                questionBankService.getQueryWrapper(questionBankQueryRequest));
        // 获取封装类
        return ResultUtils.success(questionBankService.getQuestionBankVOPage(questionBankPage, request));
    }

    /**
     * 编辑题库（给用户使用）
     *
     * @param questionBankEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> editQuestionBank(@RequestBody QuestionBankEditRequest questionBankEditRequest, HttpServletRequest request) {
        if (questionBankEditRequest == null || questionBankEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // todo 在此处将实体类和 DTO 进行转换
        QuestionBank questionBank = new QuestionBank();
        BeanUtils.copyProperties(questionBankEditRequest, questionBank);
        // 数据校验
        questionBankService.validQuestionBank(questionBank, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = questionBankEditRequest.getId();
        QuestionBank oldQuestionBank = questionBankService.getById(id);
        ThrowUtils.throwIf(oldQuestionBank == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldQuestionBank.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = questionBankService.updateById(questionBank);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    // endregion
}
