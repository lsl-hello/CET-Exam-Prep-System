package com.cet6.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cet6.entity.WrongWord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WrongWordMapper extends BaseMapper<WrongWord> {

    /**
     * 幂等加入错题本：用 INSERT ... ON DUPLICATE KEY UPDATE 一条 SQL 搞定。
     * - 记录不存在 → INSERT 新增（is_deleted=0）
     * - 记录已存在（含历史软删除的 is_deleted=1）→ UPDATE reason + 恢复 is_deleted=0
     * 这样能绕过 MyBatis-Plus 逻辑删除导致 selectOne 查不到、INSERT 撞唯一索引的问题。
     */
    @Insert("INSERT INTO wrong_word (user_id, word_id, reason, is_deleted, create_time, update_time) " +
            "VALUES (#{userId}, #{wordId}, #{reason}, 0, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE reason = VALUES(reason), is_deleted = 0, update_time = NOW()")
    int upsertWrongWord(@Param("userId") Long userId, @Param("wordId") Long wordId, @Param("reason") String reason);

    /**
     * 物理删除（绕过 MyBatis-PIus 逻辑删除，真 DELETE）。
     * 错题本取消星标时应真删除，避免软删除记录残留导致下次再加入时唯一索引冲突。
     */
    @Delete("DELETE FROM wrong_word WHERE user_id = #{userId} AND word_id = #{wordId}")
    int physicalDeleteWrongWord(@Param("userId") Long userId, @Param("wordId") Long wordId);
}
