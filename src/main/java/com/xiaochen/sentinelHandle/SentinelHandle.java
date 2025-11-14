package com.xiaochen.sentinelHandle;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.xiaochen.mianshiya.common.BaseResponse;
import com.xiaochen.mianshiya.common.ErrorCode;
import com.xiaochen.mianshiya.common.ResultUtils;
import com.xiaochen.mianshiya.model.dto.questionbank.QuestionBankQueryRequest;
import com.xiaochen.mianshiya.model.vo.QuestionBankVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;

public class SentinelHandle {
    /**
     * listQuestionBankVOByPage 降级操作：直接返回本地数据(处理业务异常)
     */
    public static BaseResponse<Page<QuestionBankVO>> handleFallback(@RequestBody QuestionBankQueryRequest questionBankQueryRequest,
                                                             HttpServletRequest request, Throwable ex) {
        // 可以返回本地数据或空数据
        return ResultUtils.success(null);
    }

    /**
     * listQuestionBankVOByPage 流控操作
     * 限流：提示“系统压力过大，请耐心等待”(处理限流＋熔断之后的降级操作)
     */
    public static BaseResponse<Page<QuestionBankVO>> handleBlockException(@RequestBody QuestionBankQueryRequest questionBankQueryRequest,
                                                                          HttpServletRequest request, BlockException ex) {
        // 降级操作
        if(ex instanceof DegradeException){
            return handleFallback(questionBankQueryRequest, request, ex);
        }
        // 限流操作
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统压力过大，请耐心等待");
    }

}
