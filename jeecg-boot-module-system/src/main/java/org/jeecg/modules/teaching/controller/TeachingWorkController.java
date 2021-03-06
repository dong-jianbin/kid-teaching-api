package org.jeecg.modules.teaching.controller;

import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jeecg.common.aspect.annotation.PermissionData;
import org.jeecg.modules.common.controller.BaseController;
import org.jeecg.modules.teaching.model.StudentWorkModel;
import org.jeecg.modules.teaching.vo.StudentWorkSendVO;
import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.jeecg.common.system.vo.LoginUser;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.teaching.entity.TeachingWorkCorrect;
import org.jeecg.modules.teaching.entity.TeachingWorkComment;
import org.jeecg.modules.teaching.entity.TeachingWork;
import org.jeecg.modules.teaching.vo.TeachingWorkPage;
import org.jeecg.modules.teaching.service.ITeachingWorkService;
import org.jeecg.modules.teaching.service.ITeachingWorkCorrectService;
import org.jeecg.modules.teaching.service.ITeachingWorkCommentService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jeecg.common.aspect.annotation.AutoLog;

 /**
 * @Description: ????????????
 * @Author: jeecg-boot
 * @Date:   2020-04-12
 * @Version: V1.0
 */
@Api(tags="????????????")
@RestController
@RequestMapping("/teaching/teachingWork")
@Slf4j
public class TeachingWorkController extends BaseController {
	@Autowired
	private ITeachingWorkService teachingWorkService;
	@Autowired
	private ITeachingWorkCorrectService teachingWorkCorrectService;
	@Autowired
	private ITeachingWorkCommentService teachingWorkCommentService;

	 /**
	  * ??????????????????????????????
	  *
	  * @param teachingWork
	  * @param pageNo
	  * @param pageSize
	  * @param req
	  * @return
	  */
	 @AutoLog(value = "????????????-??????????????????")
	 @ApiOperation(value = "????????????-??????????????????", notes = "????????????-??????????????????")
	 @GetMapping(value = "/mine")
	 public Result<?> mine(StudentWorkModel teachingWork,
												 @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
												 @RequestParam(name = "pageSize", defaultValue = "999") Integer pageSize,
												 HttpServletRequest req) {
		 teachingWork.setUserId(getCurrentUser().getId());
		 Result<IPage<StudentWorkModel>> result = new Result<IPage<StudentWorkModel>>();
		 QueryWrapper<StudentWorkModel> queryWrapper = QueryGenerator.initQueryWrapper(teachingWork, req.getParameterMap());
//		 Page<TeachingWork> page = new Page<TeachingWork>(pageNo, pageSize);
		 IPage<StudentWorkModel> pageList = teachingWorkService.listWorkModel(new Page<>(pageNo, pageSize), queryWrapper);
		 return Result.ok(pageList);
	 }

	 /**
	  * ????????????
	  * @param teachingWork
	  * @return
	  */
	 @PostMapping(value = "/submit")
	 public Result<TeachingWork> add(@RequestBody TeachingWork teachingWork) {
		 teachingWork.setUserId(getCurrentUser().getId());
		 Result<TeachingWork> result = new Result<TeachingWork>();
		 try {
			 List<TeachingWork> oldWork = teachingWorkService.getBaseMapper().selectByMap(new HashMap<String, Object>(){{
				 put("work_name", teachingWork.getWorkName());
				 put("user_id", teachingWork.getUserId());
				 put("work_type", teachingWork.getWorkType());
			 }});
			 if (oldWork.size() > 0){
				 teachingWork.setId(oldWork.get(0).getId());
				 teachingWork.setCreateTime(new Date());
				 teachingWork.setUpdateTime(new Date());
				 result.setResult(teachingWork);
				 result.success("???????????????");
			 }else{
				 result.setResult(teachingWork);
				 result.success("???????????????");
			 }
			 teachingWorkService.saveOrUpdate(teachingWork);
		 } catch (Exception e) {
			 log.error(e.getMessage(),e);
			 result.error500("??????????????????");
		 }
		 return result;
	 }

	 @AutoLog("???????????????????????????")
	 @ApiOperation(value = "???????????????????????????", notes = "???????????????????????????")
	 @PostMapping("/sendWork")
	 public Result<TeachingWork> sendWork(@RequestBody StudentWorkSendVO studentWorkSendVO){
		 Result<TeachingWork> result = new Result<>();
		 int count = teachingWorkService.sendWork(studentWorkSendVO);
		 result.setSuccess(true);
		 result.setMessage(String.format("??????????????????%d?????????", count));
		 return result;
	 }

	/**
	 * ??????????????????
	 *
	 * @param studentWorkModel
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	@AutoLog(value = "????????????-??????????????????")
	@ApiOperation(value="????????????-??????????????????", notes="????????????-??????????????????")
	@GetMapping(value = "/list")
	@PermissionData(pageComponent = "teaching/TeachingWorkList")
	public Result<?> queryPageList(StudentWorkModel studentWorkModel,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
//		QueryWrapper<TeachingWork> queryWrapper = QueryGenerator.initQueryWrapper(teachingWork, req.getParameterMap());
		Page<TeachingWork> page = new Page<TeachingWork>(pageNo, pageSize);
		QueryWrapper<StudentWorkModel> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq(null != studentWorkModel.getUsername(), "teaching_work.create_by", studentWorkModel.getUsername())
				.like(null != studentWorkModel.getWorkName(), "work_name", studentWorkModel.getWorkName())
				.like(null != studentWorkModel.getRealname(), "realname", studentWorkModel.getRealname());
		QueryGenerator.installMplus(queryWrapper, studentWorkModel, req.getParameterMap());
		//??????????????????
		Map<String, String[]> param = req.getParameterMap();
		String updateTime_begin =  param.containsKey("teaching_work.updateTime_begin")?param.get("teaching_work.updateTime_begin")[0]:null;
		String updateTime_end = param.containsKey("teaching_work.updateTime_end")?param.get("teaching_work.updateTime_end")[0]:null;
		queryWrapper.ge(null != updateTime_begin, "teaching_work.create_time", updateTime_begin).
				le(null != updateTime_end, "teaching_work.create_time",updateTime_end);
		IPage<StudentWorkModel> pageList = teachingWorkService.listWorkModel(new Page<>(pageNo, pageSize), queryWrapper);
		return Result.ok(pageList);
	}

	 @AutoLog(value = "??????????????????")
	 @ApiOperation(value = "")
	 @GetMapping("/studentWorkInfo")
	 public Result<StudentWorkModel> studentWorkInfo(@RequestParam(name = "workId") String workId){
		 Result<StudentWorkModel> result = new Result<StudentWorkModel>();
		 StudentWorkModel teachingWork = teachingWorkService.studentWorkInfo(workId);
		 if (teachingWork == null) {
			 result.error500("??????????????????");
		 } else {
			 result.setResult(teachingWork);
			 result.setSuccess(true);
		 }
		 return result;
	 }
	
	/**
	 *   ??????
	 *
	 * @param teachingWorkPage
	 * @return
	 */
	@AutoLog(value = "????????????-??????")
	@ApiOperation(value="????????????-??????", notes="????????????-??????")
	@PostMapping(value = "/add")
	public Result<?> add(@RequestBody TeachingWorkPage teachingWorkPage) {
		TeachingWork teachingWork = new TeachingWork();
		BeanUtils.copyProperties(teachingWorkPage, teachingWork);
		teachingWorkService.saveMain(teachingWork, teachingWorkPage.getTeachingWorkCorrectList(),teachingWorkPage.getTeachingWorkCommentList());
		return Result.ok(teachingWork);
	}
	
	/**
	 *  ??????
	 *
	 * @param teachingWorkPage
	 * @return
	 */
	@AutoLog(value = "????????????-??????")
	@ApiOperation(value="????????????-??????", notes="????????????-??????")
	@PutMapping(value = "/edit")
	public Result<?> edit(@RequestBody TeachingWorkPage teachingWorkPage) {
		TeachingWork teachingWork = new TeachingWork();
		BeanUtils.copyProperties(teachingWorkPage, teachingWork);
		TeachingWork teachingWorkEntity = teachingWorkService.getById(teachingWork.getId());
		if(teachingWorkEntity==null) {
			return Result.error("?????????????????????");
		}
		teachingWorkService.updateMain(teachingWork, teachingWorkPage.getTeachingWorkCorrectList(),teachingWorkPage.getTeachingWorkCommentList());
		return Result.ok("????????????!");
	}
	
	/**
	 *   ??????id??????
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "????????????-??????id??????")
	@ApiOperation(value="????????????-??????id??????", notes="????????????-??????id??????")
	@DeleteMapping(value = "/delete")
	public Result<?> delete(@RequestParam(name="id",required=true) String id) {
		teachingWorkService.delMain(id);
		return Result.ok("????????????!");
	}
	
	/**
	 *  ????????????
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "????????????-????????????")
	@ApiOperation(value="????????????-????????????", notes="????????????-????????????")
	@DeleteMapping(value = "/deleteBatch")
	public Result<?> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.teachingWorkService.delBatchMain(Arrays.asList(ids.split(",")));
		return Result.ok("?????????????????????");
	}
	
	/**
	 * ??????id??????
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "????????????-??????id??????")
	@ApiOperation(value="????????????-??????id??????", notes="????????????-??????id??????")
	@GetMapping(value = "/queryById")
	public Result<?> queryById(@RequestParam(name="id",required=true) String id) {
		TeachingWork teachingWork = teachingWorkService.getById(id);
		if(teachingWork==null) {
			return Result.error("?????????????????????");
		}
		return Result.ok(teachingWork);

	}
	
	/**
	 * ??????id??????
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "??????????????????-??????id??????")
	@ApiOperation(value="??????????????????-??????id??????", notes="????????????-??????id??????")
	@GetMapping(value = "/queryTeachingWorkCorrectByMainId")
	public Result<?> queryTeachingWorkCorrectListByMainId(@RequestParam(name="id",required=true) String id) {
		List<TeachingWorkCorrect> teachingWorkCorrectList = teachingWorkCorrectService.selectByMainId(id);
		return Result.ok(teachingWorkCorrectList);
	}
	/**
	 * ??????id??????
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "??????????????????-??????id??????")
	@ApiOperation(value="??????????????????-??????id??????", notes="????????????-??????id??????")
	@GetMapping(value = "/queryTeachingWorkCommentByMainId")
	public Result<?> queryTeachingWorkCommentListByMainId(@RequestParam(name="id",required=true) String id) {
		List<TeachingWorkComment> teachingWorkCommentList = teachingWorkCommentService.selectByMainId(id);
		return Result.ok(teachingWorkCommentList);
	}

    /**
    * ??????excel
    *
    * @param request
    * @param teachingWork
    */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, TeachingWork teachingWork) {
      // Step.1 ??????????????????????????????
      QueryWrapper<TeachingWork> queryWrapper = QueryGenerator.initQueryWrapper(teachingWork, request.getParameterMap());
      LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();

      //Step.2 ??????????????????
      List<TeachingWork> queryList = teachingWorkService.list(queryWrapper);
      // ??????????????????
      String selections = request.getParameter("selections");
      List<TeachingWork> teachingWorkList = new ArrayList<TeachingWork>();
      if(oConvertUtils.isEmpty(selections)) {
          teachingWorkList = queryList;
      }else {
          List<String> selectionList = Arrays.asList(selections.split(","));
          teachingWorkList = queryList.stream().filter(item -> selectionList.contains(item.getId())).collect(Collectors.toList());
      }

      // Step.3 ??????pageList
      List<TeachingWorkPage> pageList = new ArrayList<TeachingWorkPage>();
      for (TeachingWork main : teachingWorkList) {
          TeachingWorkPage vo = new TeachingWorkPage();
          BeanUtils.copyProperties(main, vo);
          List<TeachingWorkCorrect> teachingWorkCorrectList = teachingWorkCorrectService.selectByMainId(main.getId());
          vo.setTeachingWorkCorrectList(teachingWorkCorrectList);
          List<TeachingWorkComment> teachingWorkCommentList = teachingWorkCommentService.selectByMainId(main.getId());
          vo.setTeachingWorkCommentList(teachingWorkCommentList);
          pageList.add(vo);
      }

      // Step.4 AutoPoi ??????Excel
      ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
      mv.addObject(NormalExcelConstants.FILE_NAME, "??????????????????");
      mv.addObject(NormalExcelConstants.CLASS, TeachingWorkPage.class);
      mv.addObject(NormalExcelConstants.PARAMS, new ExportParams("??????????????????", "?????????:"+sysUser.getRealname(), "????????????"));
      mv.addObject(NormalExcelConstants.DATA_LIST, pageList);
      return mv;
    }

    /**
    * ??????excel????????????
    *
    * @param request
    * @param response
    * @return
    */
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
      MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
      Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
      for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
          MultipartFile file = entity.getValue();// ????????????????????????
          ImportParams params = new ImportParams();
          params.setTitleRows(2);
          params.setHeadRows(1);
          params.setNeedSave(true);
          try {
              List<TeachingWorkPage> list = ExcelImportUtil.importExcel(file.getInputStream(), TeachingWorkPage.class, params);
              for (TeachingWorkPage page : list) {
                  TeachingWork po = new TeachingWork();
                  BeanUtils.copyProperties(page, po);
                  teachingWorkService.saveMain(po, page.getTeachingWorkCorrectList(),page.getTeachingWorkCommentList());
              }
              return Result.ok("?????????????????????????????????:" + list.size());
          } catch (Exception e) {
              log.error(e.getMessage(),e);
              return Result.error("??????????????????:"+e.getMessage());
          } finally {
              try {
                  file.getInputStream().close();
              } catch (IOException e) {
                  e.printStackTrace();
              }
          }
      }
      return Result.ok("?????????????????????");
    }

}
