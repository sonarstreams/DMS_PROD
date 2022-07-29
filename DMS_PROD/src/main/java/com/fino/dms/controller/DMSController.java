package com.fino.dms.controller;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fino.dms.repo.CustomNativeQuery;
import com.fino.dms.service.FinoService;
import com.fino.dms.service.SrvDocUtiltiy;


@CrossOrigin
@RestController
public class DMSController {

	@Autowired
	FinoService finoService;
	
	@Autowired
	SrvDocUtiltiy srvDocUtiltiy;
	
	@Autowired
	CustomNativeQuery customNativeQuery;
	
	Logger logger = LoggerFactory.getLogger(DMSController.class);
	
	@GetMapping("/getWelcomeTxt")
	public String getWelcomeMsg() {
		logger.info("Inside DMS welcome method");
		
		return "Welcome to fino payment bank";
	}
	
	@PostMapping(value = "/downLoadImage", consumes = "application/json", produces = "application/json")
	public ResponseEntity<Map<String,String>> downloadAadharImage(@RequestBody String payload) {
		Map<String,String> response = new HashMap<>();
		try {
			
			int returnCode = finoService.downloadAadharPhoto(payload);
			response.put("status", returnCode + "");
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error in downLoadImage", e);
		}
        
		return  ResponseEntity.ok(response);
	}
	
	
	@PostMapping(value = "/upLoadImage", consumes = "application/json", produces = "application/json")
	public ResponseEntity<Map<String,String>> uploadAadharMaskImage(@RequestBody String payload) {
		Map<String,String> response = new HashMap<>();
		try {
			
			int returnCode = finoService.uploadAadharPhoto(payload);
			response.put("status", returnCode + "");
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error in uploadAadharMaskImage", e);
	
		}
        
		return  ResponseEntity.ok(response);
	}
	
	//Added By Vikas Lagad [To download multiple Version of Image]
	@PostMapping(value = "/ServoMultiImgVersion", consumes = "application/json", produces = "application/json")
	public ResponseEntity<Map<String,Object>> servoMultiVersionImg(@RequestBody String payload) {
		System.out.println("Inside servoMultiVersionImg");
		Map<String,Object> response = null;
		try {
			
			response = srvDocUtiltiy.srvDownloadMultiVersionImage(payload);
			logger.info("Multiple Image Download Response " + response);
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error in downLoadImage", e);
		}
        
		return  ResponseEntity.ok(response);
	}
	
	
	
	@PostMapping(value = "/uploadMultipleMaskedImage", consumes = "application/json", produces = "application/json")
	public ResponseEntity<Map<String,Object>> uploadMultipleMaskedImag(@RequestBody String payload) {
		Map<String,Object> response = new HashMap<>();
		try {
			
			response = srvDocUtiltiy.servoUploadMultiVersionImg(payload);
			
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error in uploadAadharMaskImage", e);
	
		}
        
		return  ResponseEntity.ok(response);
	}
	
	@PostMapping(value = "/testException", consumes = "application/json", produces = "application/json")
	public ResponseEntity<Map<String,Object>> testException(@RequestBody String payload) {
		System.out.println("Inside servoMultiVersionImg");
		Map<String,Object> response = new HashMap<String, Object>();
		try {
			JSONParser parser = new JSONParser();
			JSONObject requestJSONObject = (JSONObject) parser.parse(payload);
			//int doSomeQuery = customNativeQuery.doSomeQuery( requestJSONObject.get("query") +"");
			
			String nbsName = requestJSONObject.get("nbsName") + "";
			String docType = requestJSONObject.get("docType") + "";
			
			String oldVersionRemovedStatus = customNativeQuery.getOldVersionRemovedStatus(nbsName, docType);
			
			
			response.put("COUNT", oldVersionRemovedStatus );
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error in downLoadImage", e);
		}
        
		return  ResponseEntity.ok(response);
	}
	
	
	
	
	
	
	
	
}


