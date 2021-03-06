package com.epweike.controller.solr;

import com.epweike.controller.BaseController;
import com.epweike.model.PageModel;
import com.epweike.model.RetModel;
import com.epweike.model.solr.Talent;
import com.epweike.util.DateUtils;
import com.epweike.util.QueryUtils;
import com.epweike.util.SolrUtils;
import com.epweike.util.StatUtils;

import net.sf.json.JSONObject;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.SortClause;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.PivotField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.RangeFacet;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import java.lang.reflect.Field;

/**
 * @author wuxp
 */
@Controller
@RequestMapping("/talent")
public class TalentController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(TalentController.class);

	public List<Talent> talentList;

	public Talent talent;

	@RequestMapping(value = { "list" })
	public String list(Model model) {

		return "solr/talent/list";
	}

	/**
	 * @Description:查询人才对象
	 * @author 吴小平
	 * @version 创建时间：2015年9月29日 下午3:28:27
	 */
	public Talent getByUid(int uid) throws IOException {

		String sql = "SELECT a.uid,a.shop_id,a.seller_credit,CRC32(a.brand) AS sch_brand,a.brand,\n"
				+ "                a.mobile,a.group_id,task_bid_num,IF(credit_score > 0, credit_score, 0) AS credit_score,\n"
				+ "                a.shop_level,a.w_level,a.w_good_rate,a.integrity,a.integrity_ids,a.user_type,DATE_FORMAT(DATE_ADD(FROM_UNIXTIME(last_login_time), INTERVAL -8 HOUR),'%Y-%m-%dT%TZ') AS last_login_time,\n"
				+ "                a.username,a.is_close,a.province,a.city,a.AREA,a.task_income_cash,a.task_income_credit,a.currency,a.accepted_num,a.auth_realname,\n"
				+ "                a.auth_bank,a.auth_email,a.auth_mobile,a.chief_designer,a.isvip,CRC32(a.city) AS sch_city,CRC32(a.province) AS sch_province,\n"
				+ "                a.vip_start_time,a.vip_end_time,CASE WHEN a.come IS NULL THEN 'WEB' WHEN a.come = '' THEN 'WEB' ELSE come END AS come,\n"
				+ "                a.integrity_points,b.shop_name,b.shop_desc,b.views,c.skill_id,\n"
				+ "                GROUP_CONCAT(DISTINCT(c.skill_id)) AS skill_ids,\n"
				+ "                g.indus_name AS main_skill_name,GROUP_CONCAT(DISTINCT(h.indus_name)) AS second_skill_names,\n"
				+ "                b.min_match_money_ema,b.forbid_match_msg,b.global_match,b.min_match_money_msg,b.forbid_match_email,b.min_match_money,b.forbid_match_sms"
				+ "                 FROM keke_witkey_space a LEFT JOIN keke_witkey_shop b ON a.uid=b.uid LEFT JOIN keke_witkey_skills c ON a.uid=c.uid LEFT JOIN keke_witkey_industry d ON c.skill_id=d.indus_id\n"
				+ "                 LEFT JOIN keke_witkey_skills e ON a.uid=e.uid AND e.skill_type='main_skill' LEFT JOIN keke_witkey_skills f ON a.uid=f.uid AND f.skill_type='second_skill' \n"
				+ "                 LEFT JOIN keke_witkey_industry g ON e.skill_id=g.indus_id LEFT JOIN keke_witkey_industry h ON f.skill_id=h.indus_id\n"
				+ "                WHERE a.uid=?";
		QueryUtils<Talent> queryRunnerUtils = new QueryUtils<Talent>(Talent.class);

		Object params[] = { uid };
		// 搜索结果集
		try {
			talent = queryRunnerUtils.get(sql, params, null);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.info("获取人才对象！！！" + talent);

		return talent;
	}

	/**
	 * @throws SolrServerException
	 * @Description:ajax获取人才列表
	 * @author 吴小平
	 * @version 创建时间：2015年9月29日 下午3:28:27
	 */
	@RequestMapping(value = "get", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	public @ResponseBody String paginationDataTables(HttpServletRequest request)
			throws IOException, SolrServerException {

		// 获取查询关键参数
		String aoData = request.getParameter("aoData");
		logger.info(aoData);
		// 解析查询关键参数
		PageModel<Map<String, Object>> pageModel = parsePageParamFromJson(aoData);

		// uid
		String uid = getParamFromAodata(aoData, "uid");
		// 用户名
		String username = getParamFromAodata(aoData, "username");
		// 商铺名
		String shop_name = getParamFromAodata(aoData, "shop_name");
		// 电话
		String mobile = getParamFromAodata(aoData, "mobile");
		// 商铺等级
		String shop_level = getParamFromAodata(aoData, "shop_level");
		// 能力品级
		String w_level = getParamFromAodata(aoData, "w_level");
		// 已认证信息
		String auth = getParamFromAodata(aoData, "auth");
		// 用户类型
		String user_role = getParamFromAodata(aoData, "user_role");

		// 未认证信息
		String no_auth = getParamFromAodata(aoData, "no_auth");

		String login_time = getParamFromAodata(aoData, "login_time");

		String no_login_time = getParamFromAodata(aoData, "no_login_time");
		// 注册时间
		String reg_start = getParamFromAodata(aoData, "start");
		reg_start = (!"".equals(reg_start)) ? reg_start + "T00:00:00Z" : "*";
		String reg_end = getParamFromAodata(aoData, "end");
		reg_end = (!"".equals(reg_end)) ? reg_end + "T23:59:59Z" : "*";

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

		params.setStart(pageModel.getiDisplayStart());
		params.setRows(pageModel.getiDisplayLength());
		params.setParam("bf", "");

		if (!uid.equals(""))
			params.addFilterQuery("uid:" + uid);

		if (!username.equals(""))
			params.addFilterQuery("username:" + username);

		if (!shop_name.equals(""))
			params.addFilterQuery("shop_name:" + shop_name);

		if (!mobile.equals(""))
			params.addFilterQuery("mobile:" + mobile);

		if (!"全部".equals(login_time) && !"".equals(login_time)) {
			params.addFilterQuery("last_login_time:" + login_time);
			params.addSort(new SortClause("last_login_time", SolrQuery.ORDER.desc));
		}

		if (!user_role.equals("全部"))
			params.addFilterQuery("user_role:" + user_role);

		if (!"全部".equals(no_login_time) && !"".equals(no_login_time)) {
			params.addFilterQuery("last_login_time:" + no_login_time);
			params.addSort(new SortClause("last_login_time", SolrQuery.ORDER.desc));
		}

		if (!"全部".equals(w_level)) {
			params.addFilterQuery("w_level:" + w_level);
		}

		if (shop_level.equals("全部VIP")) {
			params.addFilterQuery("shop_level:{1 TO *}");
			params.addFilterQuery("shop_id:{0 TO *}");
		} else {
			if (!shop_level.equals("全部")) {
				params.addFilterQuery("shop_level:" + shop_level);
				params.addFilterQuery("shop_id:{0 TO *}");
			}
		}

		if (auth != null && auth.contains("auth_bank")) {
			params.addFilterQuery("auth_bank:1");
		}

		if (auth != null && auth.contains("auth_email")) {
			params.addFilterQuery("auth_email:1");
		}

		if (auth != null && auth.contains("auth_realname")) {
			params.addFilterQuery("auth_realname:1");
		}

		if (auth != null && no_auth.contains("auth_mobile")) {
			params.addFilterQuery("auth_mobile:0");
		}

		if (no_auth != null && no_auth.contains("auth_bank")) {
			params.addFilterQuery("auth_bank:0");
		}

		if (no_auth != null && no_auth.contains("auth_email")) {
			params.addFilterQuery("auth_email:0");
		}

		if (no_auth != null && no_auth.contains("auth_realname")) {
			params.addFilterQuery("auth_realname:0");
		}

		if (no_auth != null && no_auth.contains("auth_mobile")) {
			params.addFilterQuery("auth_mobile:0");
		}

		params.addFilterQuery("reg_time_date:[" + reg_start + " TO " + reg_end + "]");

		params.addSort(new SortClause("credit_score", SolrQuery.ORDER.desc));

		QueryResponse response = SolrUtils.query(params, "talent");
		// 获取人才列表
		SolrDocumentList list = response.getResults();
		// 总条数
		long total = response.getResults().getNumFound();

		// 搜索结果数
		pageModel.setiTotalDisplayRecords(total);
		pageModel.setiTotalRecords(total);
		pageModel.setAaData(list);
		JSONObject json = JSONObject.fromObject(pageModel);
		logger.info("获取人才列表！！！" + json);

		return json.toString();
	}

	@RequestMapping(value = "update", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	public @ResponseBody RetModel update(HttpServletRequest request)
			throws IOException, SQLException, IllegalArgumentException, IllegalAccessException {
		// 返回结果对象
		RetModel retModel = new RetModel();
		// 获取主键
		int uid = Integer.parseInt(request.getParameter("uid"));
		// 获取人才对象
		Talent talent = null;
		try {
			talent = getByUid(uid);
		} catch (Exception e) {
			retModel.setFlag(false);
			retModel.setObj(e);
			retModel.setMsg("查询失败，暂时无法更新！");
			e.printStackTrace();
			return retModel;
		}

		if (talent == null) {
			retModel.setFlag(false);
			retModel.setMsg("未找到该记录，暂时无法更新！");
			return retModel;
		}

		Class<? extends Talent> clazz = talent.getClass();
		Field[] fields = clazz.getDeclaredFields();
		// 拼装索引对象
		SolrInputDocument doc = new SolrInputDocument();
		for (int i = 0; i < fields.length; i++) {
			Field f = fields[i];
			f.setAccessible(true);
			System.out.println("属性名:" + f.getName() + " 属性值:" + f.get(talent));
			if ("indus_ids".equals(f.getName()) || "second_skill_names".equals(f.getName())
					|| "main_skill_names".equals(f.getName()) || "skill_ids".equals(f.getName())) {// 处理多值字段
				if (f.get(talent) != null) {
					String[] arr = f.get(talent).toString().split(",");
					for (int j = 0; j < arr.length; j++) {
						doc.addField(f.getName(), arr[j]);
					}
				}
			} else if (!"uid".equals(f.getName()) && !"serialVersionUID".equals(f.getName())) {
				Map<String, Object> oper = new HashMap<String, Object>();
				oper.put("set", f.get(talent));
				doc.addField(f.getName(), oper);
			} else if ("uid".equals(f.getName())) {
				doc.addField(f.getName(), f.get(talent));
			}
		}
		System.out.println(doc.toString());
		try {
			SolrUtils.update(doc, "talent");
			retModel.setMsg("更新成功！");
		} catch (Exception e) {
			retModel.setFlag(false);
			retModel.setObj(e);
			retModel.setMsg("更新失败！");
			e.printStackTrace();
		}

		return retModel;
	}

	@RequestMapping(value = "del", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	public @ResponseBody RetModel del(HttpServletRequest request) throws IOException {
		// 返回结果对象
		RetModel retModel = new RetModel();
		// 获取主键
		String uid = request.getParameter("uid");
		List<String> ids = new ArrayList<String>();
		ids.add(uid);
		try {
			SolrUtils.deleteById(ids, "talent");
			retModel.setMsg("删除成功！");
		} catch (Exception e) {
			retModel.setFlag(false);
			retModel.setObj(e);
			retModel.setMsg("删除失败！");
			e.printStackTrace();
		}

		return retModel;
	}

	/**
	 * @Description:一品用户省份分布统计
	 * @author 吴小平
	 * @version 创建时间：2015年6月10日 下午3:28:27
	 */
	@RequestMapping(value = { "stat/province" })
	public ModelAndView provinceStat() throws SolrServerException, IOException {

		SolrQuery params = new SolrQuery("*:*").setFacet(true).addFacetField("province");
		QueryResponse response = SolrUtils.getSolrServer("talent").query(params);
		SolrDocumentList results = response.getResults();

		// 地区分布统计
		List<FacetField> facetFields = response.getFacetFields();

		// 返回视图
		ModelAndView mv = new ModelAndView("solr/talent/province");
		// 总数
		mv.addObject("total", results.getNumFound());
		// 饼状图数据
		// mv.addObject("pieData", ChartUtils.pieJson(facetFields));
		// 柱状图数据
		mv.addObject("barData", StatUtils.barJson(facetFields));
		logger.info("进入用户分布统计！！！");
		return mv;
	}

	/**
	 * @Description:一品用户注册统计
	 * @author 吴小平
	 * @version 创建时间：2015年6月10日 下午3:28:27
	 */
	@RequestMapping(value = { "stat/register" })
	public ModelAndView registerByDate() throws SolrServerException, IOException {
		// 返回视图
		ModelAndView mv = new ModelAndView("solr/talent/register");
		logger.info("进入用户注册统计！！！");
		return mv;
	}

	/**
	 * @Description:获取注册用户
	 * @author 吴小平
	 * @version 创建时间：2015年6月10日 下午3:28:27
	 */
	@RequestMapping(value = "register/get", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	public @ResponseBody String getRegister(HttpServletRequest request) throws Exception {

		// 获取查询关键参数
		String aoData = request.getParameter("aoData");
		logger.info(aoData);
		// 解析查询关键参数
		PageModel<Map<String, Object>> pageModel = parsePageParamFromJson(aoData);

		// 注册时间
		String reg_start = getParamFromAodata(aoData, "reg_start");
		reg_start = (!"".equals(reg_start)) ? reg_start + "T00:00:00Z" : "*";
		String reg_end = getParamFromAodata(aoData, "reg_end");
		reg_end = (!"".equals(reg_end)) ? reg_end + "T23:59:59Z" : "*";

		SolrQuery params = new SolrQuery("*:*");
		params.addFilterQuery("reg_time_date:[" + reg_start + " TO " + reg_end + "]");
		params.setFacet(true);
		params.addFacetPivotField("reg_date,user_role,come").setFacetLimit(Integer.MAX_VALUE);

		QueryResponse response = SolrUtils.getSolrServer("talent").query(params);
		NamedList<List<PivotField>> namedList = response.getFacetPivot();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = null;

		int allTotal = 0;
		int allUncertain = 0;
		int allWitkey = 0;
		int allEmployer = 0;
		int allBoth = 0;
		int allWeb = 0;
		int allCpm = 0;
		int allZtepwk = 0;
		int allApp = 0;
		int allWap = 0;
		int allMall = 0;
		int allBack = 0;
		int allYun = 0;

		if (namedList != null) {
			List<PivotField> pivotList = null;
			for (int i = 0, len = namedList.size(); i < len; i++) {
				pivotList = namedList.getVal(i);
				if (pivotList != null) {
					for (PivotField pivot : pivotList) {
						int total = 0;
						map = new HashMap<String, Object>();
						map.put("label", pivot.getValue());
						// 处理身份类型
						List<PivotField> fieldList = pivot.getPivot();
						if (fieldList != null) {
							for (PivotField field : fieldList) {
								int count = field.getCount();
								String value = field.getValue().toString();
								System.out.println("field=" + field.getField());
								String tmp = "";
								if ("0".equals(value)) {// 未确定
									tmp = "uncertain";
									allUncertain += count;
								} else if ("1".equals(value)) {// 威客
									tmp = "witkey";
									allWitkey += count;
								} else if ("2".equals(value)) {// 雇主
									tmp = "employer";
									allEmployer += count;
								} else if ("3".equals(value)) {
									tmp = "both";
									allBoth += count;
								}
								map.put(tmp, count);
								total += count;
								// 处理注册渠道
								List<PivotField> fieldList2 = field.getPivot();
								for (PivotField field2 : fieldList2) {
									int count2 = field2.getCount();
									String value2 = field2.getValue().toString();
									int tmp2 = 0;
									if (map.get(value2) != null)
										tmp2 = Integer.parseInt(map.get(value2).toString());
									map.put(value2, tmp2 + count2);

									switch (value2) {
									case "WEB":
										allWeb += count2;
										break;
									case "cpm":
										allCpm += count2;
										break;
									case "ztepwk":
										allZtepwk += count2;
										break;
									case "APP":
										allApp += count2;
										break;
									case "WAP":
										allWap += count2;
										break;
									case "mall":
										allMall += count2;
										break;
									case "background":
										allBack += count2;
										break;
									case "yun":
										allYun += count2;
										break;
									default:
										break;
									}
								}
							}
						}
						allTotal += total;
						System.out.println("map" + map.toString());
						map.put("TOTAL", total);
						// 不存在赋值0
						if (map.get("WEB") == null)
							map.put("WEB", 0);
						if (map.get("cpm") == null)
							map.put("cpm", 0);
						if (map.get("ztepwk") == null)
							map.put("ztepwk", 0);
						if (map.get("APP") == null)
							map.put("APP", 0);
						if (map.get("WAP") == null)
							map.put("WAP", 0);
						if (map.get("mall") == null)
							map.put("mall", 0);
						if (map.get("background") == null)
							map.put("background", 0);
						if (map.get("yun") == null)
							map.put("yun", 0);
						if (map.get("witkey") == null)
							map.put("witkey", 0);
						if (map.get("employer") == null)
							map.put("employer", 0);
						if (map.get("both") == null)
							map.put("both", 0);
						if (map.get("uncertain") == null)
							map.put("uncertain", 0);
						if (map.get("TOTAL") == null)
							map.put("TOTAL", 0);

						list.add(map);
					}
				}
			}
		}

		// 排序(按注册日期升序)
		Collections.sort(list, new Comparator<Map<String, Object>>() {
			public int compare(Map<String, Object> arg0, Map<String, Object> arg1) {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
				Date date1 = null;
				Date date2 = null;
				try {
					date1 = simpleDateFormat.parse(arg0.get("label").toString());
					date2 = simpleDateFormat.parse(arg1.get("label").toString());
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Double timeStemp1 = (double) date1.getTime();
				Double timeStemp2 = (double) date2.getTime();
				return -(timeStemp1.compareTo(timeStemp2));
			}
		});

		// 汇总
		Map<String, Object> map2 = new HashMap<String, Object>();
		map2.put("label", "汇总");
		map2.put("TOTAL", allTotal);
		map2.put("witkey", allWitkey);
		map2.put("employer", allEmployer);
		map2.put("uncertain", allUncertain);
		map2.put("both", allBoth);
		map2.put("WEB", allWeb);
		map2.put("cpm", allCpm);
		map2.put("ztepwk", allZtepwk);
		map2.put("APP", allApp);
		map2.put("WAP", allWap);
		map2.put("mall", allMall);
		map2.put("background", allBack);
		map2.put("yun", allYun);
		list.add(0, map2);

		// 搜索结果数
		pageModel.setiTotalDisplayRecords(list.size());
		pageModel.setiTotalRecords(list.size());
		pageModel.setAaData(list);
		JSONObject json = JSONObject.fromObject(pageModel);
		logger.info("获取用户注册统计列表！！！" + json);

		return json.toString();
	}

	/**
	 * @Description:能力品级X商铺等级
	 * @author 吴小平
	 * @version 创建时间：2017年2月15日 下午3:28:27
	 */
	@RequestMapping(value = { "stat/w_to_shop_level" })
	public ModelAndView wAndShopLevel() throws SolrServerException, IOException {
		// 返回视图
		ModelAndView mv = new ModelAndView("solr/talent/w_to_shop_level");
		return mv;
	}

	/**
	 * @Description:能力品级X商铺等级
	 * @author 吴小平
	 * @version 创建时间：2017年2月15日 下午3:28:27
	 */
	@RequestMapping(value = "wAndShopLevel/get", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	public @ResponseBody String getWAndShopLevel(HttpServletRequest request) throws Exception {

		// 获取查询关键参数
		String aoData = request.getParameter("aoData");
		logger.info(aoData);
		// 解析查询关键参数
		PageModel<Map<String, Object>> pageModel = parsePageParamFromJson(aoData);

		// 注册时间
		String reg_start = getParamFromAodata(aoData, "reg_start");
		reg_start = (!"".equals(reg_start)) ? reg_start + "T00:00:00Z" : "*";
		String reg_end = getParamFromAodata(aoData, "reg_end");
		reg_end = (!"".equals(reg_end)) ? reg_end + "T23:59:59Z" : "*";

		SolrQuery params = new SolrQuery("*:*");
		params.addFilterQuery("reg_time_date:[" + reg_start + " TO " + reg_end + "]");
		params.setFacet(true);
		params.addFacetPivotField("w_level,shop_level").setFacetLimit(Integer.MAX_VALUE);

		QueryResponse response = SolrUtils.getSolrServer("talent").query(params);
		NamedList<List<PivotField>> namedList = response.getFacetPivot();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = null;

		int allTotal = 0;
		int all1 = 0;
		int all2 = 0;
		int all3 = 0;
		int all4 = 0;
		int all5 = 0;
		int all6 = 0;
		int all7 = 0;
		int all8 = 0;

		if (namedList != null) {
			List<PivotField> pivotList = null;
			for (int i = 0, len = namedList.size(); i < len; i++) {
				pivotList = namedList.getVal(i);
				if (pivotList != null) {
					for (PivotField pivot : pivotList) {
						int total = 0;
						map = new HashMap<String, Object>();
						map.put("label", pivot.getValue().toString());
						// 商铺类型
						List<PivotField> fieldList = pivot.getPivot();
						if (fieldList != null) {
							for (PivotField field : fieldList) {
								int count = field.getCount();
								String value = field.getValue().toString();
								System.out.println("field=" + field.getField());
								if ("1".equals(value)) {
									all1 += count;
								} else if ("2".equals(value)) {
									all2 += count;
								} else if ("3".equals(value)) {
									all3 += count;
								} else if ("4".equals(value)) {
									all4 += count;
								} else if ("5".equals(value)) {
									all5 += count;
								} else if ("6".equals(value)) {
									all6 += count;
								} else if ("7".equals(value)) {
									all7 += count;
								} else if ("8".equals(value)) {
									all8 += count;
								}

								map.put(getShopLevelName(value), count);
								total += count;
							}
						}
						allTotal += total;
						System.out.println("map" + map.toString());
						map.put("TOTAL", total);
						// 不存在赋值0
						if (map.get("基础版") == null)
							map.put("基础版", 0);
						if (map.get("VIP拓展") == null)
							map.put("VIP拓展", 0);
						if (map.get("VIP旗舰") == null)
							map.put("VIP旗舰", 0);
						if (map.get("VIP白金") == null)
							map.put("VIP白金", 0);
						if (map.get("VIP钻石") == null)
							map.put("VIP钻石", 0);
						if (map.get("VIP皇冠") == null)
							map.put("VIP皇冠", 0);
						if (map.get("金尊皇冠") == null)
							map.put("金尊皇冠", 0);
						if (map.get("至尊皇冠") == null)
							map.put("至尊皇冠", 0);

						list.add(map);
					}
				}
			}
		}

		// 排序(按能力品级降序)
		Collections.sort(list, new Comparator<Map<String, Object>>() {
			public int compare(Map<String, Object> arg0, Map<String, Object> arg1) {
				return arg0.get("label").toString().compareTo(arg1.get("label").toString());
			}
		});

		// 汇总
		Map<String, Object> map2 = new HashMap<String, Object>();
		map2.put("label", "汇总");
		map2.put("TOTAL", allTotal);
		map2.put("基础版", all1);
		map2.put("VIP拓展", all2);
		map2.put("VIP旗舰", all3);
		map2.put("VIP白金", all4);
		map2.put("VIP钻石", all5);
		map2.put("VIP皇冠", all6);
		map2.put("金尊皇冠", all7);
		map2.put("至尊皇冠", all8);
		list.add(0, map2);

		// 搜索结果数
		pageModel.setiTotalDisplayRecords(list.size());
		pageModel.setiTotalRecords(list.size());
		pageModel.setAaData(list);
		JSONObject json = JSONObject.fromObject(pageModel);

		return json.toString();
	}

	/**
	 * @Description:一品用户注册统计(按时间)
	 * @author 吴小平
	 * @version 创建时间：2015年6月10日 下午3:28:27
	 */
	@RequestMapping(value = { "stat/register/date" })
	public ModelAndView register() throws SolrServerException, IOException {
		// 返回视图
		ModelAndView mv = new ModelAndView("solr/talent/register_date");
		return mv;
	}

	/**
	 * @Description:注册统计列表（按时间）
	 * @author 吴小平
	 * @version 创建时间：2016年6月13日 下午5:29:08
	 */
	@RequestMapping(value = "/register/date/get", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	public @ResponseBody String getRegisterFacetByDate(HttpServletRequest request) throws Exception {

		// 获取查询关键参数
		String aoData = request.getParameter("aoData");
		// 开始时间
		String startString = getParamFromAodata(aoData, "start");
		Date start = DateUtils.parseDate(startString);
		// 结束时间
		String endString = getParamFromAodata(aoData, "end");
		Date end = DateUtils.parseDateTime(endString + " 23:59:59");
		// 用户类型
		String user_role = getParamFromAodata(aoData, "user_role");
		// 注册渠道
		String come = getParamFromAodata(aoData, "come");
		// 统计类型(日、月、年)
		String statType = getParamFromAodata(aoData, "statType");

		SolrQuery parameters = new SolrQuery("*:*").setFacet(true)
				.addDateRangeFacet("reg_time_date", start, end, statType).setFacetLimit(1000);
		if (!come.equals("全部")) {
			if (come.equals("cpm")) {
				parameters.addFilterQuery("come:cpm OR come:ztepwk");
			} else {
				parameters.addFilterQuery("come:" + come);
			}
		}

		if (!user_role.equals("全部"))
			parameters.addFilterQuery("user_role:" + user_role);
		// 解析查询关键参数
		PageModel<Map<String, Object>> pageModel = parsePageParamFromJson(aoData);
		// facet统计列表
		// 日期根据统计类型截取
		int endIndex = 10;
		if (statType.contains("YEAR")) {
			endIndex = 4;
		} else if (statType.contains("MONTH")) {
			endIndex = 7;
		}

		QueryResponse response = SolrUtils.getSolrServer("talent").query(parameters);
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

		// 搜索结果数
		pageModel.setiTotalDisplayRecords(list.size());
		pageModel.setiTotalRecords(list.size());
		pageModel.setAaData(list);
		JSONObject json = JSONObject.fromObject(pageModel);
		logger.info("获取任务统计列表！！！" + json);

		return json.toString();
	}

	/**
	 * @Description:一品用户注册统计(按地区)
	 * @author 吴小平
	 * @version 创建时间：2015年6月10日 下午3:28:27
	 */
	@RequestMapping(value = { "stat/register/area" })
	public ModelAndView registerByCat() throws SolrServerException, IOException {
		// 返回视图
		ModelAndView mv = new ModelAndView("solr/talent/register_area");
		return mv;
	}

	/**
	 * @Description:获取注册用户(按地区)
	 * @author 吴小平
	 * @version 创建时间：2015年6月10日 下午3:28:27
	 */
	@RequestMapping(value = "/register/area/get", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	public @ResponseBody String getRegisterByCat(HttpServletRequest request) throws Exception {

		// 获取查询关键参数
		String aoData = request.getParameter("aoData");
		logger.info(aoData);
		// 解析查询关键参数
		PageModel<Map<String, Object>> pageModel = parsePageParamFromJson(aoData);

		// 注册时间
		String reg_start = getParamFromAodata(aoData, "start");
		reg_start = (!"".equals(reg_start)) ? reg_start + "T00:00:00Z" : "*";
		String reg_end = getParamFromAodata(aoData, "end");
		reg_end = (!"".equals(reg_end)) ? reg_end + "T23:59:59Z" : "*";
		// 注册渠道
		String come = getParamFromAodata(aoData, "come");
		// 分类
		String indus1 = getParamFromAodata(aoData, "indus1");
		String indus2 = getParamFromAodata(aoData, "indus2");
		String indus3 = getParamFromAodata(aoData, "indus3");
		// 店铺等级
		String shop_level = getParamFromAodata(aoData, "shop_level");
		// 地区类型
		String area_type = getParamFromAodata(aoData, "area_type");
		// 身份类型
		String user_role = getParamFromAodata(aoData, "user_role");

		SolrQuery params = new SolrQuery("*:*");
		params.addFilterQuery("reg_time_date:[" + reg_start + " TO " + reg_end + "]");

		if (!come.equals("全部")) {
			if (come.equals("cpm")) {
				params.addFilterQuery("come:cpm OR come:ztepwk");
			} else {
				params.addFilterQuery("come:" + come);
			}
		}
		if (!user_role.equals("全部"))
			params.addFilterQuery("user_role:" + user_role);
		if (!indus1.equals("全部"))
			params.addFilterQuery("indus_ids_str:" + indus1);
		if (indus2 != null && !indus2.equals(""))
			params.addFilterQuery("indus_ids_str:" + indus2);
		if (indus3 != null && !indus3.equals(""))
			params.addFilterQuery("indus_ids_str:" + indus3);
		if (shop_level.equals("全部VIP")) {
			params.addFilterQuery("shop_level:{1 TO *}");
		} else {
			if (!shop_level.equals("全部"))
				params.addFilterQuery("shop_level:" + shop_level);
		}

		params.setFacet(true);

		String pivotField = "province,user_role";
		if ("按城市".equals(area_type))
			pivotField = "city,user_role";
		params.addFacetPivotField(pivotField).setFacetLimit(Integer.MAX_VALUE);

		QueryResponse response = SolrUtils.getSolrServer("talent").query(params);
		NamedList<List<PivotField>> namedList = response.getFacetPivot();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Map<String, Object> map = null;

		int allTotal = 0;
		int allUncertain = 0;
		int allWitkey = 0;
		int allEmployer = 0;
		int allBoth = 0;

		if (namedList != null) {
			List<PivotField> pivotList = null;
			for (int i = 0, len = namedList.size(); i < len; i++) {
				pivotList = namedList.getVal(i);
				if (pivotList != null) {
					for (PivotField pivot : pivotList) {
						int total = 0;
						map = new HashMap<String, Object>();
						map.put("label", pivot.getValue());
						// 处理身份类型
						List<PivotField> fieldList = pivot.getPivot();
						if (fieldList != null) {
							for (PivotField field : fieldList) {
								int count = field.getCount();
								String value = field.getValue().toString();
								System.out.println("field=" + field.getField());
								String tmp = "";
								if ("0".equals(value)) {// 未确定
									tmp = "uncertain";
									allUncertain += count;
								} else if ("1".equals(value)) {// 威客
									tmp = "witkey";
									allWitkey += count;
								} else if ("2".equals(value)) {// 雇主
									tmp = "employer";
									allEmployer += count;
								} else if ("3".equals(value)) {
									tmp = "both";
									allBoth += count;
								}
								map.put(tmp, count);
								total += count;
							}
						}
						allTotal += total;
						System.out.println("map" + map.toString());
						map.put("TOTAL", total);
						// 不存在赋值0
						if (map.get("witkey") == null)
							map.put("witkey", 0);
						if (map.get("employer") == null)
							map.put("employer", 0);
						if (map.get("both") == null)
							map.put("both", 0);
						if (map.get("uncertain") == null)
							map.put("uncertain", 0);
						if (map.get("TOTAL") == null)
							map.put("TOTAL", 0);

						list.add(map);
					}
				}
			}
		}

		// 汇总
		Map<String, Object> map2 = new HashMap<String, Object>();
		map2.put("label", "汇总");
		map2.put("TOTAL", allTotal);
		map2.put("witkey", allWitkey);
		map2.put("employer", allEmployer);
		map2.put("uncertain", allUncertain);
		map2.put("both", allBoth);
		list.add(0, map2);

		// 搜索结果数
		pageModel.setiTotalDisplayRecords(list.size());
		pageModel.setiTotalRecords(list.size());
		pageModel.setAaData(list);
		JSONObject json = JSONObject.fromObject(pageModel);
		logger.info("获取用户注册统计列表！！！" + json);

		return json.toString();
	}

}
