package com.imct.alphaclass.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.imct.alphaclass.bean.GenModelResult;
import com.imct.alphaclass.bean.GenVideoResult;
import com.imct.alphaclass.bean.ServiceUsage;
import com.imct.alphaclass.bean.TextToImageResult;

@Mapper
public interface ServiceDAO {

    /** 当前用户的文生图历史：join service_usage 过滤归属，SQL 层完成过滤与排序 */
    @Select("select r.* from text_to_image_result r join service_usage u on r.usage_id = u.id where u.user_id = #{userId} and r.is_deleted = 0 order by r.created_at desc")
    public List<Map<String, Object>> getHistoryByUserId(int userId);
    
    @Insert("Insert into service_usage (user_id,service_id,created_at,is_successful) values(#{user_id},#{service_id},#{created_at},#{is_successful})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void addUsage(ServiceUsage usage);

    @Insert("Insert into text_to_image_result (usage_id,prompt,url,thumbnail_url,size,created_at,is_deleted) values(#{usage_id},#{prompt},#{url},#{thumbnail_url},#{size},#{created_at},#{is_deleted})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void addResult(TextToImageResult result);

    @Insert("Insert into video_generate_result (request_id,user_id,type,prompt,url,thumbnail_url,size,task_status,created_at,is_deleted) values(#{request_id},#{user_id},#{type},#{prompt},#{url},#{thumbnail_url},#{size},#{task_status},#{created_at},#{is_deleted})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void addVideoResult(GenVideoResult result);

    @Insert("Insert into model_generate_result (job_id,request_id,user_id,type,prompt,prompt_image_url,url,thumbnail_url,size,task_status,created_at,is_deleted,polygon_count) values(#{job_id},#{request_id},#{user_id},#{type},#{prompt},#{prompt_image_url},#{url},#{thumbnail_url},#{size},#{task_status},#{created_at},#{is_deleted},#{polygon_count})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void addModelResult(GenModelResult result);

    @Select("select * from video_generate_result")
    public List<GenVideoResult> getAllVideoResults();

    @Select("select * from model_generate_result")
    public List<GenModelResult> getAllModelResults();

    // @Delete("delete from anchor where id = #{id}")
    // public boolean deleteAnchorById(int id);

    // @Update("update anchor set name = #{name}, pos_x = #{pos_x}, pos_y = #{pos_y},pos_z = #{pos_z},euler_x = #{euler_x},euler_y = #{euler_y},euler_z = #{euler_z} where id = #{id}")
    // public int updateAnchorById(String name,float pos_x,float pos_y,float pos_z,float euler_x,float euler_y,float euler_z, int id);

    @Update("update video_generate_result set url = #{url}, task_status= #{status},thumbnail_url = #{thumbnailUrl} where request_id = #{requestId} and user_id = #{userId}")
    public boolean updateVideoResultById(String status,String url, String thumbnailUrl, String requestId, int userId);

    @Update("update model_generate_result set url = #{url}, task_status= #{status},thumbnail_url = #{thumbnailUrl},polygon_count = #{polygon_count},size = #{size} where request_id = #{requestId} and user_id = #{userId}")
    public boolean updateModelResultById(String status,String url, String thumbnailUrl, int polygon_count, int size, String requestId, int userId);

    @Update("update video_generate_result set is_deleted=1 where id = #{id} and user_id = #{userId}")
    public void deleteVideoResultById(int id, int userId);

    @Update("update model_generate_result set is_deleted=1 where id = #{id} and user_id = #{userId}")
    public void deleteModelResultById(int id, int userId);

    @Update("update text_to_image_result set is_deleted=1 where id = #{id} and usage_id in (select id from service_usage where user_id = #{userId})")
    public void deleteTextToImageResultById(int id, int userId);
}
