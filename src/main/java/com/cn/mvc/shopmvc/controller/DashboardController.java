package com.cn.mvc.shopmvc.controller;


import com.cn.mvc.shopmvc.beans.AreaBean;
import com.cn.mvc.shopmvc.beans.ResultBean;
import com.cn.mvc.shopmvc.service.DashboardService;
import com.cn.mvc.shopmvc.utils.AreaUtil;
import com.cn.mvc.shopmvc.utils.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private Logger logger = LoggerFactory.getLogger(getClass());


    @Resource
    private DashboardService service;

    /**
     *
     * @param pw
     */
    @ResponseBody
    @RequestMapping("/list")
    @SuppressWarnings("serial")
    public void list(PrintWriter pw) {

        //获取地区枚举
        Map<Long, AreaBean> areaMaps = AreaUtil.getAreaMaps();
        ResultBean resultBean = new ResultBean();
        // UV、PV、IP数
        List<Map<String, String>> dau = service.dau();
        // 转化率
        List<Map<String, String>> convert = service.convert();
        // 周销售环比分析
        Map<String, Object> weekSale = service.weekSale();
        // 日订单数
        long dayOrderNum = service.dayOrderNum();
        // 周订单数
        long weekOrderNum = service.weekOrderNum();
        // 月订单数
        long monthOrderNum = service.monthOrderNum();
        // 所有区域订单数
        Map<Integer, Long> areaOrderNum = service.areaOrderNum();
        // 本周一到本周日每天的订单数
        Map<String, Long> weekOrderFinish = service.weekOrderFinish();
        // Top8区域订单的订单数
        Map<Integer, Long> areaOrderState = service.areaOrderState();

        // 今日24小时销售额
        Map<String, Double> hourSale = service.hourSale();
        // 日总销售额
        String daySale = service.daySale();
        // 月总销售额
        String monthSale = service.monthSale();

//     // Top4地区销售排行
        Map<Integer, Double> top4AreaSale = service.top4AreaSale();

        // 封装UV、PV、访客数指标
        resultBean.setVisitor(dau);
        // 封装转化率指标
        resultBean.setConvert(convert);
        // 封装周销售环比分析指标
        resultBean.setWeekSale(new ResultBean.WeekSale((List<String>) weekSale.get("twd"), (List<Integer>) weekSale.get("tw"), (List<Integer>) weekSale.get("lw")));
        // 封装订单数（日、周、月订单数）指标
        ArrayList<ArrayList<HashMap<String, Object>>> data = new ArrayList<ArrayList<HashMap<String, Object>>>();
        areaOrderNum.forEach((k, v) -> {
            data.add(new ArrayList<HashMap<String, Object>>() {{
                add(new HashMap<String, Object>() {{
                    AreaBean areaBean = areaMaps.get(k.longValue());
                    if (null != areaBean) {
                        put("name", areaBean.getName());
                        put("value", v.intValue());
                    }
                }});
            }});
        });
        List<ArrayList<ResultBean.ChinaMap>> chinaMap = new ArrayList<ArrayList<ResultBean.ChinaMap>>();
        //封装Echarts地图结果数据
        areaOrderNum.forEach((k, v) -> {
            AreaBean areaBean = areaMaps.get(k.longValue());
            if (null != areaBean) {
                ArrayList<ResultBean.ChinaMap> arr = new ArrayList<ResultBean.ChinaMap>();
                arr.add(new ResultBean.ChinaMap() {{
                    setName(areaBean.getName());
                    setValue(v.intValue());
                }});
                chinaMap.add(arr);
            }
        });
        resultBean.setChinaMap(chinaMap);
        resultBean.setOrderNum(new ResultBean.OrderNum(monthOrderNum, weekOrderNum, dayOrderNum, data));
        // 封装销售额（月销售、日销售、24小时销售）指标

        resultBean.setSales(new ResultBean.Sales(monthSale, daySale, hourSale));
        // 封装本周一到本周日每天的订单数
        resultBean.setTop8orderNum(new ArrayList<Map<String, Object>>() {{
            areaOrderState.forEach((k, v) -> {
                AreaBean areaBean = areaMaps.get(k.longValue());
                if (null != areaBean) {
                    add(new HashMap<String, Object>() {{
                        put("name", areaBean.getName());
                        put("value", v.intValue());
                    }});
                }
            });
        }});
        resultBean.setWeekOrderFinish(new ResultBean.WeekOrderFinish(weekOrderFinish));

        // 封装Top4地区销售排行
        List<String> areaArr = new ArrayList<String>();
        List<Double> valueArr = new ArrayList<Double>();
        top4AreaSale.forEach((k, v) -> {
            AreaBean areaBean = areaMaps.get(k.longValue());
            if (null != areaBean) {
                areaArr.add(areaBean.getName());
                valueArr.add(v);
            }
        });
        resultBean.setTop4sale(new ResultBean.Top4sale(areaArr, valueArr));
        // 返回数据到前端页面
        String dashboardJson = JsonUtil.obj2Json(resultBean);
        logger.info("==== result json is: {} ====", dashboardJson);
        System.out.println(dashboardJson);
        pw.write(dashboardJson);
    }


}
