package com.epweike.controller.solr;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.epweike.controller.BaseController;
import com.epweike.model.PageModel;
import com.epweike.util.DateUtils;
import com.epweike.util.SolrUtils;
import com.epweike.util.StatUtils;

import net.sf.json.JSONObject;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FieldStatsInfo;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.common.params.StatsParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author wuxp
 */
@Controller
@RequestMapping("/finance")
public class FinanceController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(FinanceController.class);

	/**
	 * @Description:
	 * 
	 * @author 吴小平
	 * @version 创建时间：2015年8月27日 下午4:47:31
	 */
	public List<Map<String, Object>> getFinanceFacetListByTime(String aoData, String field, String core)
			throws Exception {

		// 开始时间
		String startString = getParamFromAodata(aoData, "start");
		Date start = DateUtils.parseDate(startString);
		// 结束时间
		String endString = getParamFromAodata(aoData, "end");
		Date end = DateUtils.parseDateTime(endString + " 23:59:59");
		// 任务类型
		String taskType = getParamFromAodata(aoData, "taskType");
		// 统计类型(日、月、年)
		String statType = getParamFromAodata(aoData, "statType");
		// 来源(web、iphone、Android等)
		String source = getParamFromAodata(aoData, "source");

		SolrQuery params = new SolrQuery("*:*").setFacet(true).addDateRangeFacet(field, start, end, statType)
				.setFacetLimit(1000);
		if (!source.equals("全部"))
			params.addFilterQuery("source:" + source);
		// 过滤任务类型
		switch (taskType) {
		case "单赏":
			params.addFilterQuery("model_id:1");
			break;
		case "多赏":
			params.addFilterQuery("model_id:2");
			break;
		case "计件":
			params.addFilterQuery("model_id:3");
			break;
		case "招标":
			params.addFilterQuery("model_id:4").addFilterQuery("task_type:{* TO 2}");
			break;
		case "雇佣":
			params.addFilterQuery("model_id:4").addFilterQuery("task_type:{* TO 2}")
					.addFilterQuery("task_cash_coverage:0");
			break;
		case "服务":
			params.addFilterQuery("model_id:4").addFilterQuery("task_type:2");
			break;
		case "直接雇佣":
			params.addFilterQuery("model_id:4").addFilterQuery("task_type:3");
		default:
			break;
		}
		// 日期根据统计类型截取
		int endIndex = 10;
		if (statType.contains("YEAR")) {
			endIndex = 4;
		} else if (statType.contains("MONTH")) {
			endIndex = 7;
		}

		QueryResponse response = SolrUtils.getSolrServer(core).query(params);
		// 获取区间统计列表
		@SuppressWarnings("rawtypes")
		List<RangeFacet> listFacet = response.getFacetRanges();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		for (RangeFacet<?, ?> rf : listFacet) {
			List<RangeFacet.Count> listCounts = rf.getCounts();
			for (RangeFacet.Count count : listCounts) {
				System.out.println("RangeFacet:" + count.getValue() + ":" + count.getCount());
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("date", count.getValue().substring(0, endIndex));// 日期截取只保留年月日形式
				map.put("count", count.getCount());
				list.add(map);
			}
		}
		return list;
	}

	/**
	 * @Description:接单统计列表（按时间）
	 *
	 * @author 吴小平
	 * @version 创建时间：2015年8月27日 下午5:29:08
	 */
	@RequestMapping(value = "date/get", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	public @ResponseBody String getFinanceFacetByDate(HttpServletRequest request) throws Exception {

		// 获取查询关键参数
		String aoData = request.getParameter("aoData");
		logger.info(aoData);
		// 解析查询关键参数
		PageModel<Map<String, Object>> pageModel = parsePageParamFromJson(aoData);
		// facet统计列表
		List<Map<String, Object>> list = getFinanceFacetListByTime(aoData, "fina_time_date", "finance");
		// 搜索结果数
		pageModel.setiTotalDisplayRecords(list.size());
		pageModel.setiTotalRecords(list.size());
		pageModel.setAaData(list);
		JSONObject json = JSONObject.fromObject(pageModel);
		logger.info("获取财务统计列表！！！" + json);

		return json.toString();
	}

	/**
	 * @Description:接单统计列表（按用户）
	 * 
	 * @author 吴小平
	 * @version 创建时间：2015年8月27日 下午5:29:08
	 */
	@RequestMapping(value = "user/get", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	public @ResponseBody String getFinanceFacetListByUser(HttpServletRequest request) throws Exception {

		// 获取查询关键参数
		String aoData = request.getParameter("aoData");
		logger.info("aoData:" + aoData);
		// 解析查询关键参数
		PageModel<Map<String, Object>> pageModel = parsePageParamFromJson(aoData);

		// 开始时间
		String startString = getParamFromAodata(aoData, "start");
		// Date start = DateUtils.parseDate(startString);
		// 结束时间
		String endString = getParamFromAodata(aoData, "end");
		// Date end = DateUtils.parseDateTime(endString + " 23:59:59");
		// 任务类型
		String taskType = getParamFromAodata(aoData, "taskType");
		// 来源(web、iphone、Android等)
		String source = getParamFromAodata(aoData, "source");
		// 用户名
		String username = getParamFromAodata(aoData, "username");
		// 店铺等级
		String shop_level = getParamFromAodata(aoData, "shop_level");
		// 财务类型
		String fina_action = getParamFromAodata(aoData, "fina_action");

		SolrQuery params = new SolrQuery("*:*");
		
		// vip开通时间
        String vip_start_start = getParamFromAodata(aoData, "vip_start_start");
        String vip_start_end = getParamFromAodata(aoData, "vip_start_end");
        if (!"".equals(vip_start_start) && !"".equals(vip_start_end)) {
            vip_start_start += "T00:00:00Z";
            vip_start_end += "T23:59:59Z";
            params.addFilterQuery("vip_start_time_date:[" + vip_start_start + " TO " + vip_start_end + "]");
        }

        // vip截止时间
        String vip_end_start = getParamFromAodata(aoData, "vip_end_start");
        String vip_end_end = getParamFromAodata(aoData, "vip_end_end");
        if (!"".equals(vip_end_start) && !"".equals(vip_end_end)) {
            vip_end_start += "T00:00:00Z";
            vip_end_end += "T23:59:59Z";
            params.addFilterQuery("vip_start_time_date:[" + vip_end_start + " TO " + vip_end_end + "]");
        }

		String filter = "";
		if (fina_action != null && fina_action.contains("task_bid")) {
			filter += "fina_action:task_bid OR ";
		}

		if (fina_action != null && fina_action.contains("task_mark")) {
			filter += "fina_action:task_mark OR ";
		}

		if (fina_action != null && fina_action.contains("task_lettory")) {
			filter += "fina_action:task_lettory OR ";
		}
		
		if (!"".equals(filter)) {
			filter = filter.substring(0, filter.length() - 3);
			params.addFilterQuery(filter);
		}
		System.out.println("fina_action====="+filter);
		// 过滤掉计件任务
		// params.addFilterQuery("NOT model_id:3");
		// 过滤任务类型
		filter = "";
		if (!"".equals(taskType)) {
			String[] types = taskType.split(",");
			for (int i = 0; i < types.length; i++) {
				// 过滤任务类型
				switch (types[i]) {
				case "单赏":
					filter += "(model_id:1) OR ";
					break;
				case "多赏":
					filter += "(model_id:2) OR ";
					break;
				case "计件":
					filter += "(model_id:3) OR ";
					break;
				case "招标":
					filter += "(model_id:4 AND task_type:{* TO 2}) OR ";
					break;
				case "雇佣":
					filter += "(model_id:4 AND task_type:{* TO 2} AND task_cash_coverage:0) OR ";
					break;
				case "服务":
					filter += "(model_id:4 AND task_type:2) OR ";
					break;
				case "直接雇佣":
					filter += "(model_id:4 AND task_type:3) OR ";
				default:
					break;
				}
			}
			filter = filter.substring(0, filter.length() - 3);
		}
		params.addFilterQuery(filter);
		params.addFilterQuery("fina_time_date:[" + startString + "T00:00:00Z TO " + endString + "T23:59:59Z]");
		if (!"".equals(username))
			params.addFilterQuery("username:" + username);
		params.setGetFieldStatistics(true);
		params.setParam(StatsParams.STATS_FIELD, "fina_cash");
		params.setParam(StatsParams.STATS_FACET, "username");

		if (!source.equals("全部"))
			params.addFilterQuery("source:" + source);
		if (shop_level.equals("全部VIP")) {
			params.addFilterQuery("shop_level:{1 TO *}");
		} else {
			if (!shop_level.equals("全部"))
				params.addFilterQuery("shop_level:" + shop_level);
		}

		// 查询统计财务报表
		QueryResponse response = SolrUtils.getSolrServer("finance").query(params);

		Map<String, Object> tmp1 = new HashMap<String, Object>();
		Map<String, FieldStatsInfo> stats = response.getFieldStatsInfo();
		FieldStatsInfo statsInfo = stats.get("fina_cash");
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();// 返回结果集
		if (stats != null && stats.size() >= 0) {
			// total
			DecimalFormat df = new DecimalFormat("######0.000");// 保留3位
			if (statsInfo != null && statsInfo.getCount() > 0) {
				tmp1.put("name", "汇总");
				tmp1.put("min", df.format(statsInfo.getMin()));
				tmp1.put("max", df.format(statsInfo.getMax()));
				tmp1.put("sum", df.format(statsInfo.getSum()));
				tmp1.put("count", statsInfo.getCount());
				tmp1.put("missing", statsInfo.getMissing());
				tmp1.put("mean", df.format(statsInfo.getMean()));
				tmp1.put("stddev", df.format(statsInfo.getStddev()));
				list.add(tmp1);
				System.out.println("tmp:" + tmp1.toString());
				// facets
				Map<String, List<FieldStatsInfo>> map = statsInfo.getFacets();
				List<FieldStatsInfo> statisList = map.get("username");
				if (statisList != null && statisList.size() > 0) {
					for (int i = 0, len = statisList.size(); i < len; i++) {
						// count大于0才统计
						if (statisList.get(i).getCount() > 0) {
							Map<String, Object> tmp2 = new HashMap<String, Object>();
							tmp2.put("name", statisList.get(i).getName());
							tmp2.put("min", df.format(statisList.get(i).getMin()));
							tmp2.put("max", df.format(statisList.get(i).getMax()));
							tmp2.put("sum", df.format(statisList.get(i).getSum()));
							tmp2.put("count", statisList.get(i).getCount());
							tmp2.put("missing", statisList.get(i).getMissing());
							tmp2.put("mean", df.format(statisList.get(i).getMean()));
							tmp2.put("stddev", df.format(statisList.get(i).getStddev()));
							list.add(tmp2);
						}
					}
				}
			}
			// 排序(按中标总额降序)
			Collections.sort(list, new Comparator<Map<String, Object>>() {
				public int compare(Map<String, Object> arg0, Map<String, Object> arg1) {
					return -(Double.valueOf((String) arg0.get("sum"))
							.compareTo(Double.valueOf(arg1.get("sum").toString())));
				}
			});
			System.out.println("resultList:" + list.toString());

			// 搜索结果数
			pageModel.setiTotalDisplayRecords(list.size());
			pageModel.setiTotalRecords(list.size());
			pageModel.setAaData(list);
		}
		JSONObject json = JSONObject.fromObject(pageModel);
		logger.info("获取财务统计列表！！！" + json);

		return json.toString();
	}

	@RequestMapping(value = { "stat/user" })
	public ModelAndView financeStatByUser() throws SolrServerException, IOException {
		// 返回视图
		ModelAndView mv = new ModelAndView("solr/finance/finance_user");
		mv.addObject("sourceList", getFacetList("finance", "source", 10));
		logger.info("进入接单统计(按威客)！！！");
		return mv;
	}

	/**
	 * @Description:交易次数统计
	 * 
	 * @author 吴小平
	 * @version 创建时间：2015年8月27日 下午5:29:08
	 */
	@RequestMapping(value = "times/get", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	public @ResponseBody String getFinanceTimes(HttpServletRequest request) throws Exception {

		// 获取查询关键参数
		String aoData = request.getParameter("aoData");
		logger.info("aoData:" + aoData);
		// 解析查询关键参数
		PageModel<Map<String, Object>> pageModel = parsePageParamFromJson(aoData);

		// 开始时间
		String startString = getParamFromAodata(aoData, "start");
		// Date start = DateUtils.parseDate(startString);
		// 结束时间
		String endString = getParamFromAodata(aoData, "end");
		// Date end = DateUtils.parseDateTime(endString + " 23:59:59");
		// 任务类型
		String taskType = getParamFromAodata(aoData, "taskType");
		// 来源(web、iphone、Android等)
		String source = getParamFromAodata(aoData, "source");
		// 用户名
		String username = getParamFromAodata(aoData, "username");
		// 店铺等级
		String shop_level = getParamFromAodata(aoData, "shop_level");
		// 交易类型
		String fina_action = getParamFromAodata(aoData, "fina_action");
		// 最小交易次数
		String minCount = getParamFromAodata(aoData, "minCount");

		SolrQuery params = new SolrQuery("*:*").setFacet(true).addFacetField("username")
				.setFacetLimit(Integer.MAX_VALUE).setFacetMinCount(Integer.parseInt(minCount));
		;
		// 过滤掉计件任务
		params.addFilterQuery("NOT model_id:3");
		// 过滤任务类型
		switch (taskType) {
		case "单赏":
			params.addFilterQuery("model_id:1");
			break;
		case "多赏":
			params.addFilterQuery("model_id:2");
			break;
		case "计件":
			params.clear();
			params.addFilterQuery("model_id:3");
			break;
		case "招标":
			params.addFilterQuery("model_id:4").addFilterQuery("task_type:{* TO 2}");
			break;
		case "雇佣":
			params.addFilterQuery("model_id:4").addFilterQuery("task_type:{* TO 2}")
					.addFilterQuery("task_cash_coverage:0");
			break;
		case "服务":
			params.addFilterQuery("model_id:4").addFilterQuery("task_type:2");
			break;
		case "直接雇佣":
			params.addFilterQuery("model_id:4").addFilterQuery("task_type:3");
		default:
			break;
		}
		params.addFilterQuery("fina_time_date:[" + startString + "T00:00:00Z TO " + endString + "T23:59:59Z]");
		if (!"".equals(username))
			params.addFilterQuery("username:" + username);
		if (!"".equals(fina_action))
			params.addFilterQuery("fina_action:" + fina_action);
		params.setGetFieldStatistics(true);
		params.setParam(StatsParams.STATS_FIELD, "fina_cash");
		params.setParam(StatsParams.STATS_FACET, "username");

		if (!source.equals("全部"))
			params.addFilterQuery("source:" + source);
		if (shop_level.equals("全部VIP")) {
			params.addFilterQuery("shop_level:{1 TO *}");
		} else {
			if (!shop_level.equals("全部"))
				params.addFilterQuery("shop_level:" + shop_level);
		}

		// 查询统计财务报表
		QueryResponse response = SolrUtils.getSolrServer("finance").query(params);

		List<Map<String, Object>> list = StatUtils.getFacetList(response.getFacetFields(), "");

		// 搜索结果数
		pageModel.setiTotalDisplayRecords(list.size());
		pageModel.setiTotalRecords(list.size());
		pageModel.setAaData(list);

		JSONObject json = JSONObject.fromObject(pageModel);

		return json.toString();
	}

	@RequestMapping(value = { "stat/times" })
	public ModelAndView financeStatTimes() throws SolrServerException, IOException {
		// 返回视图
		ModelAndView mv = new ModelAndView("solr/finance/finance_times");
		mv.addObject("sourceList", getFacetList("finance", "source", 10));
		return mv;
	}

}
