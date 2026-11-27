package com.cn.mvc.shopmvc.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.cn.mvc.shopmvc.beans.WeekBean;
import com.cn.mvc.shopmvc.service.DashboardService;
import com.cn.mvc.shopmvc.utils.DateUtil;
import com.cn.mvc.shopmvc.utils.DruidHelper;
import com.cn.mvc.shopmvc.utils.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

/**
 * 实时数仓指标业务
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private Logger logger = LoggerFactory.getLogger(getClass());

    RedisUtil client = RedisUtil.build("hadoop-13", 6379);

    // 游客指标
    private static final String QUOTA_VISITOR = "quota_visitor";
    // 转化率指标
    private static final String QUOTA_CONVERT = "quota_convert";

    public DashboardServiceImpl() {
        System.out.println("DashboardServiceImpl构造方法被调用");
    }

    static LinkedHashMap<String, Long> alignDailyValues(List<WeekBean> days, Map<String, Long> rows) {
        LinkedHashMap<String, Long> result = new LinkedHashMap<>();
        for (WeekBean day : days) {
            result.put(day.getWeek(), rows.getOrDefault(day.getDay(), 0L));
        }
        return result;
    }

    //PV、UV、IP指标
    public List<Map<String, String>> dau() {
        List<Map<String, String>> userChartDataes = new ArrayList<Map<String, String>>();
        String visitorJsonStr = null;
        try {
            //visitorJsonStr = client.get(QUOTA_VISITOR);
            // 获取今日的PV、UV、IP
            //SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd");
            //String currentDate = simpleDateFormat.format(new Date());
            String currentDate = "20191016";

            String uv = client.hGet("shop:uv", currentDate).toString();
            userChartDataes.add(new HashMap<String, String>() {{
                put("name", "UV");
                put("value", uv);
            }});

            String ip = client.hGet("shop:ip", currentDate).toString();
            userChartDataes.add(new HashMap<String, String>() {{
                put("name", "ip");
                put("value", ip);
            }});

            String pv = client.hGet("shop:pv", currentDate).toString();
            userChartDataes.add(new HashMap<String, String>() {{
                put("name", "PV");
                put("value", pv);
            }});

            visitorJsonStr = "{\"pv\": " + pv + ", \"uv\": " + uv + ", \"ip\": " + ip + "}";
            System.out.println("visitorJsonStr========" + visitorJsonStr);
            logger.info("visitorJsonStr========" + visitorJsonStr);
        } catch (Exception e) {
            logger.info("==== Redis connection field! 访客{}指标数据获取失败 ====", QUOTA_VISITOR);
            logger.error(null, e);
        }

        return userChartDataes;
    }

    //漏斗转化
    public List<Map<String, String>> convert() {
        List<Map<String, String>> rateChartDataes = new ArrayList<Map<String, String>>();
        String visitorJsonStr = null;
        try {
            visitorJsonStr = client.get(QUOTA_CONVERT);
        } catch (Exception e) {
            logger.error("==== Redis connection field! 转化率{}指标数据获取失败 ====", QUOTA_CONVERT);
        }
        if (!StringUtils.isEmpty(visitorJsonStr)) {
            // bn = browseNumber
            // cn = cartNumber
            // on = orderNumber
            // pn = payNumber
            if (visitorJsonStr.contains("bn")) {
                visitorJsonStr = visitorJsonStr.replace("bn", "浏览");
            }
            if (visitorJsonStr.contains("cn")) {
                visitorJsonStr = visitorJsonStr.replace("cn", "加购物车");
            }
            if (visitorJsonStr.contains("on")) {
                visitorJsonStr = visitorJsonStr.replace("on", "下单");
            }
            if (visitorJsonStr.contains("pn")) {
                visitorJsonStr = visitorJsonStr.replace("pn", "付款");
            }

            JSONObject visitorJson = JSONObject.parseObject(visitorJsonStr);
            if (visitorJson instanceof Map) {
                visitorJson.forEach((k, v) -> rateChartDataes.add(new HashMap<String, String>() {{
                    put("name", k);
                    put("value", ((v instanceof Integer ? (int) v : 0)) + "");
                }}));
            }
        } else {
            rateChartDataes.add(new HashMap<String, String>() {{
                put("name", "浏览");
                put("value", "10000");
            }});
            rateChartDataes.add(new HashMap<String, String>() {{
                put("name", "加购物车");
                put("value", "2000");
            }});
            rateChartDataes.add(new HashMap<String, String>() {{
                put("name", "下单");
                put("value", "1200");
            }});
            rateChartDataes.add(new HashMap<String, String>() {{
                put("name", "付款");
                put("value", "1000");
            }});
        }
        return rateChartDataes;
    }

    public Map<String, Double> hourSale() {
        //初始化24小时的数据
        Map<String, Double> result = new HashMap<String, Double>() {{
            put("0", 0d); put("1", 0d); put("2", 0d); put("3", 0d); put("4", 0d); put("5", 0d); put("6", 0d); put("7", 0d);
            put("8", 0d); put("9", 0d); put("10", 0d); put("11", 0d); put("12", 0d); put("13", 0d); put("14", 0d); put("15", 0d);
            put("16", 0d); put("17", 0d); put("18", 0d); put("19", 0d); put("20", 0d); put("21", 0d); put("22", 0d); put("23", 0d);
        }};

        String sql = "SELECT EXTRACT(HOUR FROM \"__time\") AS \"curHour\",SUM(\"totalMoney\") AS \"curHourSales\" FROM \"dws_od\" WHERE \"__time\" >= CURRENT_DATE AND \"__time\" < CURRENT_DATE + INTERVAL '1' DAY GROUP BY EXTRACT(HOUR FROM \"__time\")";

        // 实例化Druid JDBC连接
        DruidHelper helper = new DruidHelper();
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = helper.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                result.put(rs.getInt(1) + "", rs.getDouble(2));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            helper.close(connection, st, rs);
        }
        return result;
    }

    public String daySale() {
        long sale = 0;
        String sql = "SELECT SUM(\"totalMoney\") AS \"curMonthSales\" FROM \"dws_od\" WHERE \"__time\" >= CURRENT_TIMESTAMP - INTERVAL '1' DAY";
        // 实例化Druid JDBC连接
        DruidHelper helper = new DruidHelper();
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = helper.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                sale = rs.getLong(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            helper.close(connection, st, rs);
        }
        return amountFormat(sale);
    }

    public Map<String, Object> weekSale() {
        // 近6天，含今天，共7天
        int twNum = 6;
        // 本周近一周
        LinkedList<WeekBean> twDay = new LinkedList<WeekBean>();
        for (int i = twNum; i >= 0; i--) {
            String day = DateUtil.latelyNday(i);
            String week = DateUtil.dayForWeek(day);
            twDay.add(new WeekBean(day, week, 0L, i));
        }
        // 上周
        int lwNum = 7;
        // 上周的近一周
        LinkedList<WeekBean> lwDay = new LinkedList<WeekBean>();
        for (int i = twNum; i >= 0; i--) {
            String day = DateUtil.latelyNday(7, i);
            String week = DateUtil.dayForWeek(day);
            lwDay.add(new WeekBean(day, week, 0L, i));
        }
        // 按时间从过去到现在进行排序（本周）
        twDay.stream().sorted((o1, o2) -> o2.getSort().compareTo(o1.getSort()));
        // 按时间从过去到现在进行排序（上周）
        lwDay.stream().sorted((o1, o2) -> o2.getSort().compareTo(o1.getSort()));


        Map<String, Object> result = new HashMap<String, Object>();
        // 上周 last week SQL
        String lwSql = "SELECT SUBSTR(CAST(\"__time\" AS VARCHAR),1,10) AS d,sum(\"totalMoney\") \n" +
                "FROM \"dws_od\" \n" +
                "where \"__time\" \n" +
                "BETWEEN ((CURRENT_TIMESTAMP - INTERVAL '7' DAY)  - INTERVAL '7' DAY) \n" +
                "AND (CURRENT_TIMESTAMP - INTERVAL '7' DAY) \n" +
                "GROUP BY 1 \n" +
                "ORDER BY 1 ASC";
        // 本周 this week SQL
        //String twSql = "SELECT SUBSTR(CAST(\"__time\" AS VARCHAR),1,10) AS d,sum(\"totalMoney\") FROM \"dws_od\" where \"__time\" >= CURRENT_TIMESTAMP - INTERVAL '6' DAY GROUP BY SUBSTR(CAST(\"__time\" AS VARCHAR),1,10) ORDER BY SUBSTR(CAST(\"__time\" AS VARCHAR),1,10) ASC";

        String twSql = "SELECT SUBSTR(CAST(\"__time\" AS VARCHAR),1,10) AS d,sum(\"totalMoney\") \n" +
                "FROM \"dws_od\" \n" +
                "where \"__time\" >= CURRENT_TIMESTAMP - INTERVAL '6' DAY \n" +
                "GROUP BY 1 \n" +
                "ORDER BY 1 ASC";
        // 实例化Druid JDBC连接
        DruidHelper helper = new DruidHelper();
        Map<String, Long> lastWeekRows = new HashMap<>();
        Map<String, Long> thisWeekRows = new HashMap<>();

        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = helper.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(lwSql);
            while (rs.next()) {
                lastWeekRows.put(rs.getString(1), rs.getLong(2));
            }
            rs.close();
            rs = st.executeQuery(twSql);
            while (rs.next()) {
                thisWeekRows.put(rs.getString(1), rs.getLong(2));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                helper.close(connection, st, rs);
            }

        }
        LinkedHashMap<String, Long> lastWeek = alignDailyValues(lwDay, lastWeekRows);
        LinkedHashMap<String, Long> thisWeek = alignDailyValues(twDay, thisWeekRows);
        result.put("lw", new ArrayList<>(lastWeek.values()));
        result.put("lwd", new ArrayList<>(lastWeek.keySet()));
        result.put("tw", new ArrayList<>(thisWeek.values()));
        result.put("twd", new ArrayList<>(thisWeek.keySet()));
        return result;
    }

    public String monthSale() {
        long sale = 0;
        String sql = "SELECT SUM(\"totalMoney\") AS \"curMonthSales\" FROM \"dws_od\" WHERE \"__time\" >= CURRENT_TIMESTAMP - INTERVAL '1' MONTH";
        // 实例化Druid JDBC连接
        DruidHelper helper = new DruidHelper();
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = helper.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                sale = rs.getLong(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            helper.close(connection, st, rs);
        }
        return amountFormat(sale);
    }

    //日销售数
    public long dayOrderNum() {
        long result = 0;
        String sql = "SELECT SUM(\"count\") FROM \"dws_od\" WHERE \"__time\" >= CURRENT_TIMESTAMP - INTERVAL '1' DAY";
        // 实例化Druid JDBC连接
        DruidHelper helper = new DruidHelper();
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = helper.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                result = rs.getLong(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                helper.close(connection, st, rs);
            }
        }
        return result;
    }

    //周销售数
    public long weekOrderNum() {
        long result = 0;
        String sql = "SELECT SUM(\"count\") FROM \"dws_od\" WHERE \"__time\" >= CURRENT_TIMESTAMP - INTERVAL '7' DAY";
        // 实例化Druid JDBC连接
        DruidHelper helper = new DruidHelper();
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = helper.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                result = rs.getLong(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                helper.close(connection, st, rs);
            }
        }
        return result;
    }

    //月销售数
    public long monthOrderNum() {
        long result = 0;
        String sql = "SELECT SUM(\"count\") FROM \"dws_od\" WHERE \"__time\" >= CURRENT_TIMESTAMP - INTERVAL '30' DAY";
        // 实例化Druid JDBC连接
        DruidHelper helper = new DruidHelper();
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = helper.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                result = rs.getLong(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                helper.close(connection, st, rs);
            }
        }
        return result;
    }


    public Map<Integer, Long> areaOrderNum() {
        Map<Integer, Long> result = new HashMap<Integer, Long>();
        String sql = "SELECT areaId,SUM(\"count\") FROM \"dws_od\" WHERE \"__time\" >= CURRENT_TIMESTAMP - INTERVAL '1' DAY GROUP BY \"areaId\"";
        // 实例化Druid JDBC连接
        DruidHelper helper = new DruidHelper();
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = helper.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                result.put(rs.getInt(1), rs.getLong(2));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            helper.close(connection, st, rs);
        }
        return result;
    }


    public Map<Integer, Long> areaOrderState() {
        Map<Integer, Long> result = new HashMap<Integer, Long>();
        String sql = "SELECT areaId, SUM(\"count\") as odn  FROM \"dws_od\" WHERE \"__time\" >= CURRENT_TIMESTAMP - INTERVAL '1' DAY GROUP BY \"areaId\" ORDER BY odn DESC limit 8";
        // 实例化Druid JDBC连接
        DruidHelper helper = new DruidHelper();
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = helper.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                result.put(rs.getInt(1), rs.getLong(2));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            helper.close(connection, st, rs);
        }
        return result;
    }


    //周订单趋势
    public Map<String, Long> weekOrderFinish() {
        // 近6天，含今天，共7天
        int twNum = 6;
        // 本周近一周
        LinkedList<WeekBean> twDay = new LinkedList<WeekBean>();
        for (int i = twNum; i >= 0; i--) {
            String day = DateUtil.latelyNday(i);
            String week = DateUtil.dayForWeek(day);
            twDay.add(new WeekBean(day, week, 0L, i));
        }
        // 按时间从过去到现在进行排序（本周）
        twDay.stream().sorted((o1, o2) -> o2.getSort().compareTo(o1.getSort()));

        Map<String, Long> rows = new HashMap<>();
        String sql = "SELECT SUBSTR(CAST(\"__time\" AS VARCHAR),1,10) AS d,sum(\"count\") FROM \"dws_od\" where \"__time\" >= CURRENT_TIMESTAMP - INTERVAL '7' DAY GROUP BY SUBSTR(CAST(\"__time\" AS VARCHAR),1,10)";
        // 实例化Druid JDBC连接
        DruidHelper helper = new DruidHelper();
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = helper.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                rows.put(rs.getString(1), rs.getLong(2));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            helper.close(connection, st, rs);
        }
        return alignDailyValues(twDay, rows);
    }


    public Map<Integer, Double> top4AreaSale() {
        Map<Integer, Double> result = new LinkedHashMap<Integer, Double>();
        String sql = "SELECT areaId, SUM(\"totalMoney\") as ta FROM \"dws_od\" WHERE \"__time\" >= CURRENT_TIMESTAMP - INTERVAL '1' DAY GROUP BY \"areaId\" ORDER BY ta DESC limit 4";
        // 实例化Druid JDBC连接
        DruidHelper helper = new DruidHelper();
        Connection connection = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            connection = helper.getConnection();
            st = connection.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                result.put(rs.getInt(1), rs.getDouble(2));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            helper.close(connection, st, rs);
        }
        return result;
    }

    /**
     * 当amount的长度小于10，则进行补0
     * @return
     */
    public String amountFormat(Long amount) {
        String zero = "0".intern();
        String temp = null;
        int length = 10;
        String amountStr = amount.toString();
        int amountLength = amountStr.length();
        if (amountLength < length) {
            int diff = length - amountLength;
            for (int i = 0; i < diff; i++) {
                if (i == 0) {
                    temp = zero;
                } else {
                    temp = temp + zero;
                }
            }
            amountStr = temp + amountStr;
        }
        return amountStr;
    }

    public static void main(String[] args) {
        DashboardServiceImpl service = new DashboardServiceImpl();
        System.out.println(service.amountFormat(1000L));
        // UV、PV、访客数
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
        // 月总销售额
        String monthSale = service.monthSale();
        // 日总销售额
        String daySale = service.daySale();
        // 今日24小时销售额
        Map<String, Double> hourSale = service.hourSale();
        // Top8区域订单的订单数
        Map<Integer, Long> areaOrderState = service.areaOrderState();
        // 本周一到本周日每天的订单数
        Map<String, Long> weekOrderFinish = service.weekOrderFinish();
        // Top4地区销售排行
        Map<Integer, Double> top4AreaSale = service.top4AreaSale();

        System.out.println("UV、PV、访客数：" + dau);
        System.out.println("转化率：" + convert);
        System.out.println("周销售环比分析：" + weekSale);
        System.out.println("日订单数：" + dayOrderNum);
        System.out.println("周订单数：" + weekOrderNum);
        System.out.println("月订单数：" + monthOrderNum);
        System.out.println("所有区域订单数：" + areaOrderNum);
        System.out.println("月总销售额：" + monthSale);
        System.out.println("日总销售额：" + daySale);
        System.out.println("今日24小时销售额：" + hourSale);
        System.out.println("Top8区域订单的订单数：" + areaOrderState);
        System.out.println("本周一到本周日每天的订单数：" + weekOrderFinish);
        System.out.println("Top4地区销售排行：" + top4AreaSale);
    }

}
