package com.epweike.controller;

import com.epweike.model.RetModel;
import com.epweike.model.Sysconfig;
import com.epweike.model.PageModel;
import com.epweike.service.SysconfigService;
import com.epweike.util.SysconfigUtils;

import net.sf.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

/**
 * @author wuxp
 */
@Controller
@RequestMapping("/sysconfig")
public class SysconfigController extends BaseController {

	private static final Logger logger = LoggerFactory
			.getLogger(SysconfigController.class);

	@Autowired
	private SysconfigService sysconfigService;

	public List<Sysconfig> sysconfigList;

	/**
	 * @Description:清空系统参数
	 * 
	 * @author 吴小平
	 * @version 创建时间：2015年6月11日 上午9:32:13
	 */
	@RequestMapping(value = "clear", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	public @ResponseBody RetModel clear(HttpServletRequest request)
			throws IOException {
		// 返回结果对象
		RetModel retModel = new RetModel();
		SysconfigUtils.sysconfigMap = null;
		retModel.setMsg("刷新成功！");
		return retModel;
	}

	@RequestMapping(value = { "list" })
	public String list(Model model) {
		return "sysconfig/list";
	}

	@RequestMapping(value = "add", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	public @ResponseBody RetModel add(HttpServletRequest request)
			throws IOException {
		// 返回结果对象
		RetModel retModel = new RetModel();
		// 获取请求参数
		String varName = request.getParameter("varName");
		String varValue = request.getParameter("varValue");
		String varGroup = request.getParameter("varGroup");
		String description = request.getParameter("description");

		// 数据校验
		if ("".equals(varName) || varName == null) {
			retModel.setFlag(false);
			retModel.setMsg("参数名不能为空！");
			return retModel;
		} else {// 校验参数名是否存在
			Sysconfig sysconfig = new Sysconfig(varName);
			sysconfig = sysconfigService.selectOne(sysconfig);
			if (sysconfig != null) {
				retModel.setFlag(false);
				retModel.setMsg("参数名:'" + varName + "'已存在！");
				return retModel;
			}
		}

		Sysconfig sysconfig = new Sysconfig();
		sysconfig.setVarName(varName);
		sysconfig.setVarValue(varValue);
		sysconfig.setVarGroup(varGroup);
		sysconfig.setDescription(description);
		sysconfig.setOnTime(new Date());

		try {
			// 添加到数据库
			sysconfigService.insert(sysconfig);
			retModel.setInsertSucceed();
		} catch (AccessDeniedException e) {
			e.printStackTrace();
			retModel.setAccessDenied(e);
			return retModel;
		} catch (Exception e) {
			e.printStackTrace();
			retModel.setInsertFail(e);
			return retModel;
		}

		return retModel;

	}

	@RequestMapping(value = "del", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	public @ResponseBody RetModel del(HttpServletRequest request)
			throws IOException {
		// 返回结果对象
		RetModel retModel = new RetModel();
		// 获取主键
		int id = Integer.parseInt(request.getParameter("id"));

		try {
			sysconfigService.deleteByPrimaryKey(id);
			// 清空系统参数
			SysconfigUtils.sysconfigMap = null;
			retModel.setDelSucceed();
		} catch (AccessDeniedException e) {
			e.printStackTrace();
			retModel.setAccessDenied(e);
			return retModel;
		} catch (Exception e) {
			retModel.setDelFail(e);
			e.printStackTrace();
		}

		return retModel;
	}

	@RequestMapping(value = "update", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	public @ResponseBody RetModel update(HttpServletRequest request)
			throws IOException {
		// 返回结果对象
		RetModel retModel = new RetModel();
		// 获取请求参数
		int id = Integer.parseInt(request.getParameter("id"));
		String varName = request.getParameter("varName");
		String varValue = request.getParameter("varValue");
		String varGroup = request.getParameter("varGroup");
		String description = request.getParameter("description");

		// 数据校验
		if ("".equals(varName) || varName == null) {
			retModel.setFlag(false);
			retModel.setMsg("参数名不能为空！");
			return retModel;
		} else {// 校验参数名是否存在
			Sysconfig sysconfig = new Sysconfig(varName);
			sysconfig = sysconfigService.selectOne(sysconfig);
			if (sysconfig != null && sysconfig.getId() != id) {
				retModel.setFlag(false);
				retModel.setMsg("参数名:'" + varName + "'已存在！");
				return retModel;
			}
		}

		Sysconfig sysconfig = new Sysconfig();
		sysconfig.setId(id);
		sysconfig.setVarName(varName);
		sysconfig.setVarValue(varValue);
		sysconfig.setVarGroup(varGroup);
		sysconfig.setDescription(description);

		try {
			// 更新数据库和计划任务
			sysconfigService.update(sysconfig);
			// 清空系统参数
			SysconfigUtils.sysconfigMap = null;
			retModel.setUpdateSucceed();
		} catch (AccessDeniedException e) {
			e.printStackTrace();
			retModel.setAccessDenied(e);
			return retModel;
		} catch (Exception e) {
			e.printStackTrace();
			retModel.setUpdateFail(e);
			return retModel;
		}

		return retModel;
	}

	@RequestMapping(value = "get", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
	public @ResponseBody String get(HttpServletRequest request)
			throws IOException {

		// 获取查询关键参数
		String aoData = request.getParameter("aoData");
		// 解析查询关键参数
		PageModel<Sysconfig> pageModel = parsePageParamFromJson(aoData);
		// 搜索条件
		String sSearch = pageModel.getsSearch();
		// 总条数(无过滤)
		int total = sysconfigService.count(new Sysconfig());

		// 搜索结果集
		sysconfigList = sysconfigService.selectPage(new Sysconfig(sSearch),
				pageModel);
		// 搜索结果数
		pageModel.setiTotalDisplayRecords(total);
		pageModel.setiTotalRecords(total);
		pageModel.setAaData(sysconfigList);

		JSONObject json = JSONObject.fromObject(pageModel);
		logger.info("获取计划任务列表！！！" + json);

		return json.toString();
	}

}
