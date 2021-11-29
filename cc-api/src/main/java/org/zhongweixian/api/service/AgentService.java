package org.zhongweixian.api.service;

import com.github.pagehelper.PageInfo;
import org.cti.cc.entity.Agent;
import org.cti.cc.po.AgentInfo;
import org.cti.cc.po.AgentSipPo;
import org.cti.cc.vo.AgentVo;
import org.cti.cc.vo.AgentBindSkill;
import org.cti.cc.vo.AgentSipVo;
import org.cti.cc.vo.BatchAddAgentVo;
import org.zhongweixian.api.vo.excel.AgentImportExcel;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;


public interface AgentService extends BaseService<Agent> {


    /**
     * 添加修改坐席
     *
     * @param agent
     * @return
     */
    int saveOrUpdate(AgentVo agent);

    /**
     * 批量添加坐席
     *
     * @param addAgentVo
     * @return
     */
    Integer batchAddAgent(BatchAddAgentVo addAgentVo);

    /**
     * 获取坐席详情
     *
     * @param companyId
     * @param id
     * @return
     */
    AgentInfo getAgentInfo(Long companyId, Long id);

    /**
     * 技能中添加坐席
     *
     * @param companyId
     * @param skillId   技能ID
     * @param ids       坐席ID
     * @param rank      技能等级
     * @return
     */
    int agentBindSkill(Long companyId, Long skillId, List<Long> ids, Integer rank);

    /**
     * 删除坐席
     *
     * @param companyId
     * @param id
     * @return
     */
    int deleteAgent(Long companyId, Long id);

    /**
     * sip列表
     *
     * @param params
     * @return
     */
    PageInfo<AgentSipPo> agentSipList(Map<String, Object> params);

    /**
     * 添加sip号码
     *
     * @param agentSipVo
     * @return
     */
    int saveOrUpdateAgentSip(AgentSipVo agentSipVo);

    /**
     * 删除sip
     *
     * @param companyId
     * @param id
     * @return
     */
    int deleteSip(Long companyId, Long id);


    /**
     * 坐席导出
     *
     * @param response
     * @param params
     */
    void agentExport(HttpServletResponse response, Map<String, Object> params) throws IOException;

    /**
     * 批量上传坐席
     *
     * @param agentList
     * @param companyId
     * @return
     */
    int agentImport(List<AgentImportExcel> agentList, Long companyId);

    /**
     * 坐席绑定技能
     *
     * @param agentBindSkill
     * @return
     */
    int agentBindSkill(AgentBindSkill agentBindSkill);
}
