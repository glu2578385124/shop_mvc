package com.cn.mvc.shopmvc.beans;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ResultBeanTest {

    @Test
    public void salesKeepsLeadingZerosAndOrdersHoursNumerically() {
        Map<String, Double> hours = new LinkedHashMap<>();
        hours.put("10", 100.75);
        hours.put("2", 20.25);
        hours.put("0", 0.0);

        ResultBean result = new ResultBean();
        result.setSales(new ResultBean.Sales("0000012345", "0000000067", hours));
        JSONObject sales = JSON.parseObject(JSON.toJSONString(result)).getJSONObject("sales");

        assertEquals(Arrays.asList("0", "0", "0", "0", "0", "1", "2", "3", "4", "5"),
                JSON.parseArray(sales.getJSONArray("monthSale").toJSONString(), String.class));
        assertEquals(Arrays.asList("0", "0", "0", "0", "0", "0", "0", "0", "6", "7"),
                JSON.parseArray(sales.getJSONArray("daySale").toJSONString(), String.class));
        assertEquals(Arrays.asList("0", "2", "10"),
                JSON.parseArray(sales.getJSONArray("xAxis").toJSONString(), String.class));
        assertEquals(Arrays.asList(0.0, 20.25, 100.75),
                JSON.parseArray(sales.getJSONArray("data").toJSONString(), Double.class));
    }

    @Test
    public void weekOrderFinishPreservesInputOrderAndLongCounts() {
        Map<String, Long> orders = new LinkedHashMap<>();
        orders.put("Friday", 3000000000L);
        orders.put("Saturday", 12L);
        orders.put("Sunday", 0L);

        ResultBean result = new ResultBean();
        result.setWeekOrderFinish(new ResultBean.WeekOrderFinish(orders));
        JSONObject finish = JSON.parseObject(JSON.toJSONString(result)).getJSONObject("weekOrderFinish");

        assertEquals(Arrays.asList("Friday", "Saturday", "Sunday"),
                JSON.parseArray(finish.getJSONArray("xAxis").toJSONString(), String.class));
        assertEquals(Arrays.asList(3000000000L, 12L, 0L),
                JSON.parseArray(finish.getJSONArray("data").toJSONString(), Long.class));
    }

    @Test
    public void emptySourceMapsProduceEmptyChartArrays() {
        ResultBean.Sales sales = new ResultBean.Sales("0", "0", new LinkedHashMap<String, Double>());
        ResultBean.WeekOrderFinish finish = new ResultBean.WeekOrderFinish(new LinkedHashMap<String, Long>());

        assertEquals(0, sales.getxAxis().size());
        assertEquals(0, sales.getData().size());
        assertEquals(0, finish.getxAxis().size());
        assertEquals(0, finish.getData().size());
    }
}
