//package com.xiaochen.mianshiya.esdao;
//
//import com.xiaochen.mianshiya.model.dto.post.PostEsDTO;
//import com.xiaochen.mianshiya.model.dto.question.QuestionEsDTO;
//import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
//
//import java.util.List;
//
///**
// * 題目 ES 操作
// *
// * @author <a href="https://github.com/lixiaochen">程序员鱼皮</a>
// * @from <a href="https://xiaochen.icu">编程导航知识星球</a>
// */
//public interface QuestionEsDao extends ElasticsearchRepository<QuestionEsDTO, Long> {
//
//    List<QuestionEsDTO> findByUserId(Long userId);
//}