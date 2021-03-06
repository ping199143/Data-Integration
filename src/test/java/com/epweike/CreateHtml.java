/**
 * Copyright 2010-2015 epweike.com.
 * @Description: 
 * @author 吴小平
 * @date Sep 16, 2015 8:50:49 AM 
 * 
 */
package com.epweike;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class CreateHtml {
	public static void main(String[] args) {
		try {
			// 创建一个合适的Configration对象
			Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);
			configuration.setDirectoryForTemplateLoading(new File(
					"F:\\workspace\\Eclipse\\Data Integration\\src\\main\\webapp\\WEB-INF\\template"));
			configuration.setDefaultEncoding("UTF-8"); // 这个一定要设置，不然在生成的页面中 会乱码
			// 获取或创建一个模版。
			Template template = configuration.getTemplate("home.ftl");
			Map<String, Object> paramMap = new HashMap<String, Object>();
			paramMap.put("description", "我正在学习使用Freemarker生成静态文件！");

			List<String> nameList = new ArrayList<String>();
			nameList.add("陈靖仇");
			nameList.add("玉儿");
			nameList.add("宇文拓");
			paramMap.put("nameList", nameList);

			Map<String, Object> weaponMap = new HashMap<String, Object>();
			weaponMap.put("first", "轩辕剑");
			weaponMap.put("second", "崆峒印");
			weaponMap.put("third", "女娲石");
			weaponMap.put("fourth", "神农鼎");
			weaponMap.put("fifth", "伏羲琴");
			weaponMap.put("sixth", "昆仑镜");
			weaponMap.put("seventh", null);
			paramMap.put("weaponMap", weaponMap);

			Writer writer = new OutputStreamWriter(new FileOutputStream(
					"success.html"), "UTF-8");
			template.process(paramMap, writer);

			System.out.println("恭喜，生成成功~~");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TemplateException e) {
			e.printStackTrace();
		}

	}
}