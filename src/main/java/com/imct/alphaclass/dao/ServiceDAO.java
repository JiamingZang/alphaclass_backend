package com.imct.alphaclass.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.imct.alphaclass.controller.ServiceController.GenModelResult;
import com.imct.alphaclass.controller.ServiceController.GenVideoResult;
import com.imct.alphaclass.controller.ServiceController.ServiceResult;
import com.imct.alphaclass.controller.ServiceController.ServiceUsage;

@Mapper
public interface ServiceDAO {

    @Select("select * from text_to_image_result")
    public List<Map<String, Object>> getAllResults();
    
    @Select("select * from service_usage where id = #{id}")
    public Map<String, Object> getUsageById(int id);
    
    @Insert("Insert into service_usage (user_id,service_id,created_at,is_successful) values(#{user_id},#{service_id},#{created_at},#{is_successful})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void addUsage(ServiceUsage usage);

    @Insert("Insert into text_to_image_result (usage_id,prompt,url,thumbnail_url,size,created_at,is_deleted) values(#{usage_id},#{prompt},#{url},#{thumbnail_url},#{size},#{created_at},#{is_deleted})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void addResult(ServiceResult result);

    @Insert("Insert into video_generate_result (request_id,user_id,type,prompt,url,thumbnail_url,size,task_status,created_at,is_deleted) values(#{request_id},#{user_id},#{type},#{prompt},#{url},#{thumbnail_url},#{size},#{task_status},#{created_at},#{is_deleted})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void addVideoResult(GenVideoResult result);

    @Insert("Insert into model_generate_result (job_id,request_id,user_id,type,prompt,prompt_image_url,url,thumbnail_url,size,task_status,created_at,is_deleted,polygon_count) values(#{job_id},#{request_id},#{user_id},#{type},#{prompt},#{prompt_image_url},#{url},#{thumbnail_url},#{size},#{task_status},#{created_at},#{is_deleted},#{polygon_count})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    public void addModelResult(GenModelResult result);

    @Select("select * from video_generate_result")
    public List<Map<String, Object>> getAllVideoResults();

    @Select("select * from model_generate_result")
    public List<Map<String, Object>> getAllModelResults();

    // @Delete("delete from anchor where id = #{id}")
    // public boolean deleteAnchorById(int id);

    // @Update("update anchor set name = #{name}, pos_x = #{pos_x}, pos_y = #{pos_y},pos_z = #{pos_z},euler_x = #{euler_x},euler_y = #{euler_y},euler_z = #{euler_z} where id = #{id}")
    // public int updateAnchorById(String name,float pos_x,float pos_y,float pos_z,float euler_x,float euler_y,float euler_z, int id);

    @Update("update video_generate_result set url = #{url}, task_status= #{status},thumbnail_url = #{thumbnailUrl} where request_id = #{requestId}")
    public boolean updateVideoResultById(String status,String url, String thumbnailUrl, String requestId);

    @Update("update model_generate_result set url = #{url}, task_status= #{status},thumbnail_url = #{thumbnailUrl},polygon_count = #{polygon_count},size = #{size} where request_id = #{requestId}")
    public boolean updateModelResultById(String status,String url, String thumbnailUrl, int polygon_count, int size, String requestId);

    @Update("update video_generate_result set is_deleted=1 where id = #{id}")
    public void deleteVideoResultById(int id);

    @Update("update model_generate_result set is_deleted=1 where id = #{id}")
    public void deleteModelResultById(int id);

    @Update("update text_to_image_result set is_deleted=1 where id = #{id}")
    public void deleteTextToImageResultById(int id);
}
