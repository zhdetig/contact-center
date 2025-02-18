package org.zhongweixian.api.quartz;

import com.google.common.collect.Lists;
import io.minio.MinioClient;
import io.minio.RemoveObjectsArgs;
import io.minio.messages.DeleteObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.cti.cc.constant.Constant;
import org.cti.cc.entity.CallDevice;
import org.cti.cc.mapper.AgentMapper;
import org.cti.cc.mapper.PushLogMapper;
import org.cti.cc.po.CompanyInfo;
import org.cti.cc.util.DateTimeUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.zhongweixian.api.service.CallLogService;
import org.zhongweixian.api.service.CompanyService;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by caoliang on 2021/9/5
 */
@Component
public class TaskJobOfDay implements Job {
    private Logger logger = LoggerFactory.getLogger(TaskJobOfDay.class);

    /**
     * 当前表的话单保留当天
     */
    @Value("${call.retain:0}")
    private Integer callRetain;

    /**
     * 话单推送保留5天
     */
    @Value("${push.log.retain:5}")
    private Integer pushLogRetain;

    @Value("${platform.v9.key:pykqu7qfhcs5gz87}")
    private String key;

    @Autowired
    private CallLogService callLogService;

    @Autowired
    private PushLogMapper pushLogMapper;

    @Autowired
    private AgentMapper agentMapper;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DiscoveryClient discoveryClient;

    /**
     * 每天定时任务，凌晨延时5分钟执行
     */
    public final static String CRON = "0 5 0 * * ? *";
    public final static String NAME = "TaskJobOfDay";

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Long start = DateTimeUtil.getBeforDay();
        Long end = DateTimeUtil.getNowDay();
        logger.info("Day job start: {} - end:{}", DateFormatUtils.format(start, DateTimeUtil.YYYYMMDD_HHMMSS), DateFormatUtils.format(end, DateTimeUtil.YYYYMMDD_HHMMSS));


        //清理话单数据
        deleteCallLog();

        //删除推送失败的数据
        deletePushLog();

        //删除录音
        deleteMinioRecord();
    }


    public void deleteCallLog() {
        //获取前N天的时间
        Long time = DateTimeUtil.getBeforeDay(callRetain);
        int resule = callLogService.cleaDayOfData(time);
        logger.info("delete {} call of logs", resule);
    }

    /**
     * 删除推送的历史数据
     */
    private void deletePushLog() {
        Long time = DateTimeUtil.getBeforeDay(pushLogRetain);
        int result = pushLogMapper.deletePushLog(time / 1000);
        logger.info("delete {} day of {} size cc_push_log", pushLogRetain, result);
    }

    /**
     * 删除呼叫的录音,默认保留最后一个月的数据
     */
    private void deleteMinioRecord() {
        List<CompanyInfo> companies = companyService.getCompanyList(null);
        Map<String, Object> params = new HashMap<>();
        for (CompanyInfo companyInfo : companies) {
            //查询n天前的话单，
            Long time = DateTimeUtil.getBeforeMonth(companyInfo.getRecordStorage());
            params.put("companyId", companyInfo.getId());
            params.put("start", time - 24 * 3600 * 1000);
            params.put("end", time);
            params.put("monthTime", DateTimeUtil.getMonth(time));

            List<CallDevice> callDevices = callLogService.callDeviceList(params);
            while (!CollectionUtils.isEmpty(callDevices)) {
                //循环批量删除录音
                List<List<CallDevice>> newList = Lists.partition(callDevices, 100);
                for (List<CallDevice> list : newList) {
                    List<DeleteObject> objects = new LinkedList<>();
                    list.forEach(callDevice -> {
                        if (StringUtils.isNotBlank(callDevice.getRecord())) {
                            objects.add(new DeleteObject(callDevice.getRecord()));
                        }
                    });
                    minioClient.removeObjects(RemoveObjectsArgs.builder().bucket(Constant.RRCORD_BUCKET).objects(objects).build());
                }
                params.put("id", callDevices.get(callDevices.size() - 1).getId());
                callDevices = callLogService.callDeviceList(params);
            }
        }
        logger.info("delete minio record");
    }
}
