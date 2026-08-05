package com.imct.alphaclass.service;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.imct.alphaclass.bean.Anchor;
import com.imct.alphaclass.bean.Course;
import com.imct.alphaclass.bean.User;
import com.imct.alphaclass.dao.AnchorDAO;
import com.imct.alphaclass.dao.CourseDAO;
import com.imct.alphaclass.dao.UserDAO;
import com.imct.alphaclass.utils.MapUtils;

@Service
public class AnchorService {
    @Resource
    private AnchorDAO dao;
    @Resource
    private CourseDAO coursedao;
    @Resource
    private UserDAO userdao;

    public List<Map<String, Object>> getAllAnchorsByCourse(String ownername,String coursename){
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        List<Map<String, Object>> all_anchorresult = dao.getAllByCid(course.getId());
        for (Map<String,Object> ac : all_anchorresult) {
            ac.remove("cid");
            ac.put("pos", MapUtils.nestVec(ac, "pos", "pos"));
            ac.put("euler", MapUtils.nestVec(ac, "euler", "euler"));
            ac.put("id", ac.get("id").toString());
        }
        return all_anchorresult;
    } 

    public Map<String, Object> addAnchorByCourse(String ownername, String coursename, Map<String, Object> params){
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        Anchor anchor = new Anchor();
        anchor.setCid(course.getId());anchor.setName(params.get("name").toString());
        Map<String, Object> pos = (Map<String, Object>)params.get("pos");
        anchor.setPos_x(Float.parseFloat("".equals(pos.get("pos_x").toString())?"0.0":pos.get("pos_x").toString()));
        anchor.setPos_y(Float.parseFloat("".equals(pos.get("pos_y").toString())?"0.0":pos.get("pos_y").toString()));
        anchor.setPos_z(Float.parseFloat("".equals(pos.get("pos_z").toString())?"0.0":pos.get("pos_z").toString()));
        Map<String, Object> euler = (Map<String, Object>)params.get("euler");
        anchor.setEuler_x(Float.parseFloat("".equals(euler.get("euler_x").toString())?"0.0f":euler.get("euler_x").toString()));
        anchor.setEuler_y(Float.parseFloat("".equals(euler.get("euler_y").toString())?"0.0f":euler.get("euler_y").toString()));
        anchor.setEuler_z(Float.parseFloat("".equals(euler.get("euler_z").toString())?"0.0f":euler.get("euler_z").toString()));
        dao.addAnchor(anchor);
        
        anchor = dao.getAnchorById(anchor.getId());
        Map<String, Object> ac = MapUtils.toMap(anchor);
        ac.remove("cid");
        ac.put("pos", MapUtils.nestVec(ac, "pos", "pos"));
        ac.put("euler", MapUtils.nestVec(ac, "euler", "euler"));
        ac.put("id", ac.get("id").toString());
        return ac;
    }

    public boolean deleteAnchorById(String ownername, String coursename, int anchorid){
        Anchor anchor = dao.getAnchorById(anchorid);
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        if (anchor!=null&&anchor.getCid()==course.getId()) {
            dao.deleteAnchorById(anchorid);
            if (dao.getAnchorById(anchorid)==null) {
                return true;
            }else{
                return false;
            }
        }else{
            return false;
        }
    }

    public Map<String, Object> modifyAnchorById(String ownername, String coursename, int anchorid,Map<String, Object> params) {
        Anchor old_anchor = dao.getAnchorById(anchorid);
        User user = userdao.getByUsername(ownername);
        Course course = coursedao.getCourseByUidAndName(user.getId(), coursename);
        if (old_anchor!=null&&old_anchor.getCid()==course.getId()) {
            Anchor anchor = new Anchor();
            anchor.setCid(course.getId());
            anchor.setName(params.get("name")==null?old_anchor.getName():params.get("name").toString());
            if (params.get("pos")!=null) {
                Map<String, Object> pos = (Map<String, Object>)params.get("pos");
                anchor.setPos_x(Float.parseFloat("".equals(pos.get("pos_x").toString())?"0.0":pos.get("pos_x").toString()));
                anchor.setPos_y(Float.parseFloat("".equals(pos.get("pos_y").toString())?"0.0":pos.get("pos_y").toString()));
                anchor.setPos_z(Float.parseFloat("".equals(pos.get("pos_z").toString())?"0.0":pos.get("pos_z").toString()));
            }else{
                anchor.setPos_x(old_anchor.getPos_x());
                anchor.setPos_y(old_anchor.getPos_y());
                anchor.setPos_z(old_anchor.getPos_z());
            }
            if (params.get("euler") != null) {
                
                Map<String, Object> euler = (Map<String, Object>)params.get("euler");
                anchor.setEuler_x(Float.parseFloat("".equals(euler.get("euler_x").toString())?"0.0f":euler.get("euler_x").toString()));
                anchor.setEuler_y(Float.parseFloat("".equals(euler.get("euler_y").toString())?"0.0f":euler.get("euler_y").toString()));
                anchor.setEuler_z(Float.parseFloat("".equals(euler.get("euler_z").toString())?"0.0f":euler.get("euler_z").toString()));
            }else{
                anchor.setEuler_x(old_anchor.getEuler_x());
                anchor.setEuler_y(old_anchor.getEuler_y());
                anchor.setEuler_z(old_anchor.getEuler_z());
            }
            dao.updateAnchorById(anchor.getName(),anchor.getPos_x(),anchor.getPos_y(),anchor.getPos_z(),
                anchor.getEuler_x(),anchor.getEuler_y(),anchor.getEuler_z(), anchorid);
            
            anchor = dao.getAnchorById(anchorid);
            Map<String, Object> ac = MapUtils.toMap(anchor);
            ac.remove("cid");
            ac.put("pos", MapUtils.nestVec(ac, "pos", "pos"));
            ac.put("euler", MapUtils.nestVec(ac, "euler", "euler"));
            ac.put("id", ac.get("id").toString());
            return ac;
        }else{
            return null;
        }
    }
}
