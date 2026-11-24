package com.cn.mvc.shopmvc.utils;

import com.cn.mvc.shopmvc.beans.AreaBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AreaUtil {

    private static List<AreaBean> list = new ArrayList<AreaBean>();
    private static Map<Long, AreaBean> areaMaps = new HashMap<Long, AreaBean>();

    public static void main(String[] args) {
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i).getName());
        }
    }

    static {
        //初始化省份
        list.add(new AreaBean(1,"110000","0",1,"北京市","39.929986","116.395645"));
        list.add(new AreaBean(2,"120000","0",1,"天津市","39.14393","117.210813"));
        list.add(new AreaBean(3,"130000","0",1,"河北省","38.61384","115.661434"));
        list.add(new AreaBean(4,"140000","0",1,"山西省","37.866566","112.515496"));
        list.add(new AreaBean(5,"150000","0",1,"内蒙古自治区","43.468238","114.415868"));
        list.add(new AreaBean(6,"210000","0",1,"辽宁省","41.6216","122.753592"));
        list.add(new AreaBean(7,"220000","0",1,"吉林省","43.678846","126.262876"));
        list.add(new AreaBean(8,"230000","0",1,"黑龙江省","47.356592","128.047414"));
        list.add(new AreaBean(9,"310000","0",1,"上海市","31.249162","121.487899"));
        list.add(new AreaBean(10,"320000","0",1,"江苏省","33.013797","119.368489"));
        list.add(new AreaBean(11,"330000","0",1,"浙江省","29.159494","119.957202"));
        list.add(new AreaBean(12,"340000","0",1,"安徽省","31.859252","117.216005"));
        list.add(new AreaBean(13,"350000","0",1,"福建省","26.050118","117.984943"));
        list.add(new AreaBean(14,"360000","0",1,"江西省","27.757258","115.676082"));
        list.add(new AreaBean(15,"370000","0",1,"山东省","36.09929","118.527663"));
        list.add(new AreaBean(16,"410000","0",1,"河南省","34.157184","113.486804"));
        list.add(new AreaBean(17,"420000","0",1,"湖北省","31.209316","112.410562"));
        list.add(new AreaBean(18,"430000","0",1,"湖南省","27.695864","111.720664"));
        list.add(new AreaBean(19,"440000","0",1,"广东省","23.408004","113.394818"));
        list.add(new AreaBean(20,"450000","0",1,"广西壮族自治区","23.552255","108.924274"));
        list.add(new AreaBean(21,"460000","0",1,"海南省","19.180501","109.733755"));
        list.add(new AreaBean(22,"500000","0",1,"重庆市","29.544606","106.530635"));
        list.add(new AreaBean(23,"510000","0",1,"四川省","0.367481","102.89916"));
        list.add(new AreaBean(24,"520000","0",1,"贵州省","26.902826","106.734996"));
        list.add(new AreaBean(25,"530000","0",1,"云南省","24.864213","101.592952"));
        list.add(new AreaBean(26,"540000","0",1,"西藏自治区","1.367315","89.137982"));
        list.add(new AreaBean(27,"610000","0",1,"陕西省","35.860026","109.503789"));
        list.add(new AreaBean(28,"620000","0",1,"甘肃省","38.103267","102.457625"));
        list.add(new AreaBean(29,"630000","0",1,"青海省","5.499761","96.202544"));
        list.add(new AreaBean(30,"640000","0",1,"宁夏回族自治区","37.321323","106.155481"));
        list.add(new AreaBean(31,"650000","0",1,"新疆维吾尔自治区","2.127001","85.614899"));
        list.add(new AreaBean(32,"810000","0",1,"香港特别行政区","22.293586","114.186124"));
        list.add(new AreaBean(33,"820000","0",1,"澳门特别行政区","22.204118","113.557519"));

        //初始化市区的
        list.forEach(bean -> {
            areaMaps.put(bean.getId(), bean);
        });
    }

    public static Map<Long, AreaBean> getAreaMaps() {
        return areaMaps;
    }

}