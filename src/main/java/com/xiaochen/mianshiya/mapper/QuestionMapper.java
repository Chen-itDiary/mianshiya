package com.xiaochen.mianshiya.mapper;

import com.xiaochen.mianshiya.model.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * @author 陈亦让
 * @description 针对表【question(题目)】的数据库操作Mapper
 * @createDate 2025-09-23 10:45:59
 * @Entity generator.domain.Question
 */
public interface QuestionMapper extends BaseMapper<Question> {
    /**
     * 查询题目列表（包含已经删除的数据）
     *
     * @param fiveMinutesAgoDate
     * @return
     */
    @Select("select * from question where updateTime >= #{fiveMinutesAgoDate}")
    List<Question> listQuestionWithDelete(Date fiveMinutesAgoDate);
}




