package com.imct.alphaclass.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.imct.alphaclass.bean.Anchor;
import com.imct.alphaclass.bean.Course;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.common.Constants;
import com.imct.alphaclass.dao.AnchorDAO;
import com.imct.alphaclass.dao.CourseDAO;
import com.imct.alphaclass.dao.UserDAO;
import com.imct.alphaclass.exception.ServiceException;
import com.imct.alphaclass.utils.MapUtils;

@Service
public class AnchorService {
    @Resource
    private AnchorDAO dao;
    @Resource
    private CourseDAO coursedao;
    @Resource
    private UserDAO userdao;

    /** 查询课程下全部锚点（pos/euler 嵌套、cid 移除、id 转字符串） */
    public List<Map<String, Object>> getAllAnchorsByCourse(String ownername, String coursename) {
        Course course = requireCourse(ownername, coursename);
        return dao.getAllByCid(course.getId()).stream()
                .map(AnchorService::decorateAnchor)
                .collect(Collectors.toList());
    }

    /** 新增锚点：pos/euler 扁平字段入库，返回组装后的锚点对象 */
    public Map<String, Object> addAnchorByCourse(String ownername, String coursename, Map<String, Object> params) {
        Course course = requireCourse(ownername, coursename);
        Anchor anchor = new Anchor();
        anchor.setCid(course.getId());
        anchor.setName(params.get("name").toString());
        Map<String, Object> pos = (Map<String, Object>)params.get("pos");
        anchor.setPos_x(MapUtils.parseFloat(pos, "pos_x"));
        anchor.setPos_y(MapUtils.parseFloat(pos, "pos_y"));
        anchor.setPos_z(MapUtils.parseFloat(pos, "pos_z"));
        Map<String, Object> euler = (Map<String, Object>)params.get("euler");
        anchor.setEuler_x(MapUtils.parseFloat(euler, "euler_x"));
        anchor.setEuler_y(MapUtils.parseFloat(euler, "euler_y"));
        anchor.setEuler_z(MapUtils.parseFloat(euler, "euler_z"));
        dao.addAnchor(anchor);
        
        anchor = dao.getAnchorById(anchor.getId());
        return decorateAnchor(MapUtils.toMap(anchor));
    }

    /** 删除锚点：仅当锚点属于该课程时删除，返回是否删除成功 */
    public boolean deleteAnchorById(String ownername, String coursename, int anchorid) {
        Anchor anchor = dao.getAnchorById(anchorid);
        Course course = requireCourse(ownername, coursename);
        if (anchor != null && anchor.getCid() == course.getId()) {
            dao.deleteAnchorById(anchorid);
            return dao.getAnchorById(anchorid) == null;
        }
        return false;
    }

    /** 修改锚点：仅当锚点属于该课程时生效；未传 pos/euler 时沿用旧值 */
    public Map<String, Object> modifyAnchorById(String ownername, String coursename, int anchorid, Map<String, Object> params) {
        Anchor old_anchor = dao.getAnchorById(anchorid);
        Course course = requireCourse(ownername, coursename);
        if (old_anchor!=null&&old_anchor.getCid()==course.getId()) {
            Anchor anchor = new Anchor();
            anchor.setCid(course.getId());
            anchor.setName(params.get("name")==null?old_anchor.getName():params.get("name").toString());
            if (params.get("pos")!=null) {
                Map<String, Object> pos = (Map<String, Object>)params.get("pos");
                anchor.setPos_x(MapUtils.parseFloat(pos, "pos_x"));
                anchor.setPos_y(MapUtils.parseFloat(pos, "pos_y"));
                anchor.setPos_z(MapUtils.parseFloat(pos, "pos_z"));
            }else{
                anchor.setPos_x(old_anchor.getPos_x());
                anchor.setPos_y(old_anchor.getPos_y());
                anchor.setPos_z(old_anchor.getPos_z());
            }
            if (params.get("euler") != null) {
                
                Map<String, Object> euler = (Map<String, Object>)params.get("euler");
                anchor.setEuler_x(MapUtils.parseFloat(euler, "euler_x"));
                anchor.setEuler_y(MapUtils.parseFloat(euler, "euler_y"));
                anchor.setEuler_z(MapUtils.parseFloat(euler, "euler_z"));
            }else{
                anchor.setEuler_x(old_anchor.getEuler_x());
                anchor.setEuler_y(old_anchor.getEuler_y());
                anchor.setEuler_z(old_anchor.getEuler_z());
            }
            dao.updateAnchorById(anchor.getName(),anchor.getPos_x(),anchor.getPos_y(),anchor.getPos_z(),
                anchor.getEuler_x(),anchor.getEuler_y(),anchor.getEuler_z(), anchorid);
            
            anchor = dao.getAnchorById(anchorid);
            return decorateAnchor(MapUtils.toMap(anchor));
        }else{
            return null;
        }
    }

    /** user/course 任一不存在时抛 404（替代链式 NPE） */
    private Course requireCourse(String ownername, String coursename) {
        User user = userdao.getByUsername(ownername);
        if (user == null) {
            throw new ServiceException(Constants.CODE_404, "用户不存在");
        }
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        if (course == null) {
            throw new ServiceException(Constants.CODE_404, "课程不存在");
        }
        return course;
    }

    /** 复制并装饰单条锚点行（pos/euler 收拢嵌套、cid 移除、id 转字符串） */
    private static Map<String, Object> decorateAnchor(Map<String, Object> ac) {
        Map<String, Object> result = new HashMap<String, Object>(ac);
        result.remove("cid");
        result.put("pos", MapUtils.nestVec(result, "pos", "pos"));
        result.put("euler", MapUtils.nestVec(result, "euler", "euler"));
        result.put("id", result.get("id").toString());
        return result;
    }
}
