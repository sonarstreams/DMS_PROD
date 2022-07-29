package com.fino.dms.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fino.dms.repo.CustomNativeQuery;

@Service
public class SrvDocUtiltiy {

	@Autowired
	FinoService finoService;

	@Autowired
	CustomNativeQuery customNativeQuery;
	
	
	Logger logger = LoggerFactory.getLogger(FinoService.class);

	@Value("${dms.downloadUrl}")
	private String dmsGetDocDataUrl;

	@Value("${dms.uploadDocUrl}")
	private String uploadDocUrl;

	@Value("${dms.servoDocumentUrl}")
	private String servoDocumentUrl;
	
	private static int nbsCount = 0;
	private static int imgProcessCount= 0;
	private static int uploadCnt = 0;
	
	@SuppressWarnings("unchecked")
	public Map<String, Object> srvDownloadMultiVersionImage(String requestJson) {
		int stats = 0, downloadImgCount = 0;
		Map<String, Object> map = new HashMap<>();
		Map<String,Integer> uploadPinstIdCnt = new HashMap<>();
		String nbsName = "", docName = "", destPath = "";
		try {

			JSONParser parser = new JSONParser();
			JSONObject requestJSONObject = (JSONObject) parser.parse(requestJson);

			nbsName = requestJSONObject.get("NBS_NAME") + "";
			docName = requestJSONObject.get("DOC_NAME") + "";
			destPath = requestJSONObject.get("DEST_DIRECTORY") + "";

			RestTemplate restTemplate = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			JSONObject requestObject = new JSONObject();
			requestObject.put("NBS_NAME", nbsName);
			requestObject.put("DOC_NAME", docName);

			logger.info("requestObject " + requestObject.toString());
			logger.info("Download URL " + servoDocumentUrl);
			HttpEntity<String> request = new HttpEntity<String>(requestObject.toString(), headers);
			ResponseEntity<String> response1 = restTemplate.exchange(servoDocumentUrl, HttpMethod.POST, request,
					String.class);
			String res = response1.getBody();
			logger.info("Output \n" + res);
			JSONParser parser1 = new JSONParser();
			JSONObject respJsonObj = (JSONObject) parser1.parse(res);

			if (respJsonObj.get("status") != null) {
				stats = Integer.parseInt(respJsonObj.get("status") + "");
				if (stats == 0) {
					JSONArray jsonArray = (JSONArray) respJsonObj.get("data");
					logger.info("[SrvDocUtility]SRV Doc list size " + jsonArray.size());

					for (int i = 0; i < jsonArray.size(); i++) {
						org.json.simple.JSONObject tempJson = (org.json.simple.JSONObject) jsonArray.get(i);

						String extension = tempJson.get("EXTENSION") + "";
						String documentString = tempJson.get("IMAGE") + "";
						String version = tempJson.get("VERSION") + "";

						boolean receivedFlag = saveFile(documentString, destPath, nbsName, docName, extension, version);
						if (receivedFlag) {
							downloadImgCount++;
							receivedFlag = false;
						}

					}

					map.put("status", 0);
					map.put("message", "OK");
					map.put("download_count", downloadImgCount);
				}
			}
		} catch (Exception e) {
			logger.error("[srvDownloadMultiVersionImage] Exception while downloading multiversion images ",
					e.getMessage());
			e.printStackTrace();
			map.put("status", 1);
			map.put("message", "INTERNAL ERROR");
			map.put("download_count", 0);
		} finally {

		}
		return map;
	}

	public boolean saveFile(String documentString, String destPath, String nbsName, String docName, String extension,
			String version) {
		boolean retFlag = false;

		try {

			byte[] imageBytes = javax.xml.bind.DatatypeConverter.parseBase64Binary(documentString);
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
			BufferedImage transperancyImage = ensureOpaque(image);
			String time = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());

			File unmasked_dir = new File(destPath + File.separator + time);

			if (!unmasked_dir.exists()) {
				unmasked_dir.mkdirs();
			}

			if (docName.equalsIgnoreCase("ADDRESS PROOF FRONT")) {
				docName = "ADDRESS_PROOF_FRONT";
			} else if (docName.equalsIgnoreCase("ADDRESS PROOF BACK")) {
				docName = "ADDRESS_PROOF_BACK";
			} else if (docName.equalsIgnoreCase("ID PROOF Front")) {
				docName = "ID_PROOF_FRONT";
			} else if (docName.equalsIgnoreCase("ID PROOF Back")) {
				docName = "ID_PROOF_BACK";
			}
			

			String path = "" + destPath + File.separator + time + File.separator + nbsName + "_" + docName + "_VERSION_"
					+ version + "." + extension;
			logger.error("[DMSImageProcessing] IMAGE WILL BE AT : " + path);

			File outputfile = new File(path);
			retFlag = ImageIO.write(transperancyImage, extension, outputfile);
			logger.error("retFlag  " + retFlag);

		} catch (Exception e) {
			logger.error("Exception while saving the document ", e);
			e.printStackTrace();
		}

		return retFlag;
	}

	public static BufferedImage ensureOpaque(BufferedImage bi) {
		if (bi.getTransparency() == BufferedImage.OPAQUE)
			return bi;
		int w = bi.getWidth();
		int h = bi.getHeight();
		int[] pixels = new int[w * h];
		bi.getRGB(0, 0, w, h, pixels, 0, w);
		BufferedImage bi2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		bi2.setRGB(0, 0, w, h, pixels, 0, w);
		return bi2;
	}

	public Map<String,Object> servoUploadMultiVersionImg(String requestJson){
		
		Map<String,Map<String,List<String>>> map = new TreeMap<>();
		Map<String,String> docTypeMap =  new HashMap<>();
	    Map<String,Object> response = new  HashMap<String,Object>();
		logger.error(" <<< ==============  Uploading Multiple Version Of Document ======= >>> [" + requestJson + "]");
		int counter = 0;
		int status = 0;
		Long uniqueLength = 0l;
		String encodstring = null;
		String pinstIdList = null;
		String documentTypeID = null;
		
		String  partiallyMaskedPath = "", unMaskedSourcePath = "",
		maskedSourcePath = "",timerServiceType = "";
		JSONArray jsonArr =null;
		JSONObject jsonObj = null;
		try {
			if (requestJson != null && !"".equalsIgnoreCase(requestJson)) {

				JSONParser parser = new JSONParser();
				JSONObject requestJSONObject = (JSONObject) parser.parse(requestJson);

				uniqueLength =  Long.parseLong(requestJSONObject.get("uniqueIdLength") + "");
				final String processId = requestJSONObject.get("processId") + "";
				final String maskedImageUrl = requestJSONObject.get("maskedImageUrl") + "";
				final String afterMaskedImageUrl = requestJSONObject.get("afterMaskedImageUrl") + "";
				timerServiceType = requestJSONObject.get("timerServiceType") + "";
			    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
				logger.info(" timerServiceType [" + timerServiceType + "]" );
				
				
				File directoryPath = new File(maskedImageUrl);
				
				String contents[] = directoryPath.list();
				File[] listFiles = directoryPath.listFiles();
				Arrays.sort(listFiles);
				logger.info("[SrvDocUtility]File size "+ listFiles.length);
				
				for (int i = 0; i < listFiles.length; i++) {
					String dateFolder = contents[i];
					logger.info("[SrvDocUtility]Folder Name ::"+dateFolder);
					
					String historyPath = afterMaskedImageUrl ;;
					response = processToUploadDocument(historyPath,maskedImageUrl, uniqueLength,dateFolder,processId);
				}
				
			
			}
		} catch (Exception e) {
			logger.error(" [servoUploadMultiVersionImg] Exception while uploading multiple version of image ",e);
			e.printStackTrace();
			response.put("status", 1);
			response.put("message", "SYSTEM ERROR" );
			
		} 
		 return response;
	}
	
	public Set<String> listFilesUsingJavaIO(String dir) {
	    return Stream.of(new File(dir).listFiles())
	      .filter(file -> !file.isDirectory())
	      .map(File::getName)
	      .collect(Collectors.toSet());
	}
	
	
	
	public Map<String,Object> processToUploadDocument(String historyPath,String maskedImageUrl,Long uniqueLength,String dateFolder,String processId){
		Map<String,Map<String,List<String>>> mapDocumentGroupByPinstId = new TreeMap<>();
		Map<String,Map<String,Integer>> uploadCntStatus = new TreeMap<>();
		Map<String,Object> response = new HashMap<>();
		
		try {
			
			File directoryPath = new File(maskedImageUrl + File.separator +  dateFolder + File.separator + "masked");
			
			if( directoryPath.exists()) {
				String contents[] = directoryPath.list();
				File[] listFiles = directoryPath.listFiles();
				Arrays.sort(listFiles);
				logger.info("[SrvDocUtility]File size "+ listFiles.length);
				if( listFiles.length == 0) {
					removeFolderToArchieve(maskedImageUrl,historyPath,dateFolder);
				}
				
				for (int i = 0; i < listFiles.length; i++) {
					
					String file = contents[i];
					if (uniqueLength == 20 && file.contains("FINO-") && file.contains("ADDRESS_PROOF_BACK")) {
						String pinstId = file.substring(0, 20);
						if(mapDocumentGroupByPinstId.containsKey(pinstId)) {
							if(mapDocumentGroupByPinstId.get(pinstId).containsKey("ADDRESS_PROOF_BACK")) {
								List<String> list = mapDocumentGroupByPinstId.get(pinstId).get("ADDRESS_PROOF_BACK");
								list.add(file);
								mapDocumentGroupByPinstId.get(pinstId).put("ADDRESS_PROOF_BACK", list);
							} else {
								ArrayList<String> list1 = new ArrayList<>();
								list1.add(file);
								mapDocumentGroupByPinstId.get(pinstId).put("ADDRESS_PROOF_BACK", list1);
							}
							
						} else {
							ArrayList<String> list = new ArrayList<>();
							list.add(file);
							Map<String,List<String>> map2 = new TreeMap<>();
							map2.put("ADDRESS_PROOF_BACK", list);
							mapDocumentGroupByPinstId.put(pinstId, map2);
						}
						
					} else  if (uniqueLength == 20 && file.contains("FINO-") &&  file.contains("ADDRESS_PROOF_FRONT")) {
						String pinstId = file.substring(0, 20);
						if(mapDocumentGroupByPinstId.containsKey(pinstId)) {
							
							if(mapDocumentGroupByPinstId.get(pinstId).containsKey("ADDRESS_PROOF_FRONT")) {
								List<String> list = mapDocumentGroupByPinstId.get(pinstId).get("ADDRESS_PROOF_FRONT");
								list.add(file);
								mapDocumentGroupByPinstId.get(pinstId).put("ADDRESS_PROOF_FRONT", list);
							} else {
								ArrayList<String> list1 = new ArrayList<>();
								list1.add(file);
								mapDocumentGroupByPinstId.get(pinstId).put("ADDRESS_PROOF_FRONT", list1);
							}
						} else {
							ArrayList<String> list = new ArrayList<>();
							list.add(file);
							Map<String,List<String>> map2 = new TreeMap<>();
							map2.put("ADDRESS_PROOF_FRONT", list);
							mapDocumentGroupByPinstId.put(pinstId, map2);
						}
					} else if (uniqueLength == 9 && !file.contains("FINO-") && file.contains("ADDRESS_PROOF_BACK")) {
						String pinstId = file.substring(0, 9);
						if(mapDocumentGroupByPinstId.containsKey(pinstId)) {
							if(mapDocumentGroupByPinstId.get(pinstId).containsKey("ADDRESS_PROOF_BACK")) {
								List<String> list = mapDocumentGroupByPinstId.get(pinstId).get("ADDRESS_PROOF_BACK");
								list.add(file);
								mapDocumentGroupByPinstId.get(pinstId).put("ADDRESS_PROOF_BACK", list);
							} else {
								ArrayList<String> list1 = new ArrayList<>();
								list1.add(file);
								mapDocumentGroupByPinstId.get(pinstId).put("ADDRESS_PROOF_BACK", list1);
							}
							
						} else {
							ArrayList<String> list = new ArrayList<>();
							list.add(file);
							Map<String,List<String>> map2 = new TreeMap<>();
							map2.put("ADDRESS_PROOF_BACK", list);
							mapDocumentGroupByPinstId.put(pinstId, map2);
						}
						
					} else  if (uniqueLength == 9 && !file.contains("FINO-") &&  file.contains("ADDRESS_PROOF_FRONT")) {
						String pinstId = file.substring(0, 9);
						if(mapDocumentGroupByPinstId.containsKey(pinstId)) {
							
							if(mapDocumentGroupByPinstId.get(pinstId).containsKey("ADDRESS_PROOF_FRONT")) {
								List<String> list = mapDocumentGroupByPinstId.get(pinstId).get("ADDRESS_PROOF_FRONT");
								list.add(file);
								mapDocumentGroupByPinstId.get(pinstId).put("ADDRESS_PROOF_FRONT", list);
							} else {
								ArrayList<String> list1 = new ArrayList<>();
								list1.add(file);
								mapDocumentGroupByPinstId.get(pinstId).put("ADDRESS_PROOF_FRONT", list1);
							}
						} else {
							ArrayList<String> list = new ArrayList<>();
							list.add(file);
							Map<String,List<String>> map2 = new TreeMap<>();
							map2.put("ADDRESS_PROOF_FRONT", list);
							mapDocumentGroupByPinstId.put(pinstId, map2);
						}
					} else if (uniqueLength == 9 && !file.contains("FINO-") && file.contains("ID_PROOF_FRONT")) {
						String pinstId = file.substring(0, 9);
						if(mapDocumentGroupByPinstId.containsKey(pinstId)) {
							if(mapDocumentGroupByPinstId.get(pinstId).containsKey("ID_PROOF_FRONT")) {
								List<String> list = mapDocumentGroupByPinstId.get(pinstId).get("ID_PROOF_FRONT");
								list.add(file);
								mapDocumentGroupByPinstId.get(pinstId).put("ID_PROOF_FRONT", list);
							} else {
								ArrayList<String> list1 = new ArrayList<>();
								list1.add(file);
								mapDocumentGroupByPinstId.get(pinstId).put("ID_PROOF_FRONT", list1);
							}
							
						} else {
							ArrayList<String> list = new ArrayList<>();
							list.add(file);
							Map<String,List<String>> map2 = new TreeMap<>();
							map2.put("ID_PROOF_FRONT", list);
							mapDocumentGroupByPinstId.put(pinstId, map2);
						}
						
					} else  if (uniqueLength == 9 && !file.contains("FINO-") &&  file.contains("ID_PROOF_BACK")) {
						String pinstId = file.substring(0, 9);
						if(mapDocumentGroupByPinstId.containsKey(pinstId)) {
							
							if(mapDocumentGroupByPinstId.get(pinstId).containsKey("ID_PROOF_BACK")) {
								List<String> list = mapDocumentGroupByPinstId.get(pinstId).get("ID_PROOF_BACK");
								list.add(file);
								mapDocumentGroupByPinstId.get(pinstId).put("ID_PROOF_BACK", list);
							} else {
								ArrayList<String> list1 = new ArrayList<>();
								list1.add(file);
								mapDocumentGroupByPinstId.get(pinstId).put("ID_PROOF_BACK", list1);
							}
						} else {
							ArrayList<String> list = new ArrayList<>();
							list.add(file);
							Map<String,List<String>> map2 = new TreeMap<>();
							map2.put("ID_PROOF_BACK", list);
							mapDocumentGroupByPinstId.put(pinstId, map2);
						}
					}
				}
				
				int pinstIdCount = mapDocumentGroupByPinstId.keySet().size();
				
				logger.info("[SrvDocUtility]Total pinstid count  "+ pinstIdCount);
		
				
				
				
				mapDocumentGroupByPinstId.keySet().forEach(PINST_ID -> {
					
					logger.info("[SrvDocUtility]COUNT " + SrvDocUtiltiy.nbsCount );
					if(nbsCount <= 9 ) {
						nbsCount = nbsCount  + 1;
							
						mapDocumentGroupByPinstId.get(PINST_ID).keySet().forEach(DOCUMENT_TYPE -> {
							//logger.info("[SrvDocUtility]>>>> PINSTID [" + PINST_ID  + "]   Document Type["  + DOCUMENT_TYPE  + "]");
							
							
							//check OLDER UNMASKED VERSION REMOVED OR NOT
							logger.info("[SrvDocUtility]CHECK UNMASKED VERSION REMOVED OR NOT");
							String oldVersionRemovedStatus = customNativeQuery.getOldVersionRemovedStatus(PINST_ID, DOCUMENT_TYPE);
							logger.info("[SrvDocUtility]older version removed status "+oldVersionRemovedStatus);
							
							if(oldVersionRemovedStatus!=null && !"".equalsIgnoreCase(oldVersionRemovedStatus) && "N".equalsIgnoreCase(oldVersionRemovedStatus)) {
								logger.info("[SrvDocUtility]Removing Older version of images to history table");
								int unMaskVersionDataToHistory = customNativeQuery.unMaskVersionDataToHistory(PINST_ID, DOCUMENT_TYPE);
								logger.info("[SrvDocUtility]MOVE TO HISTORY COUNT "+unMaskVersionDataToHistory);
							}
							
							//IF N THEN MOVE TO HISTORY TABLE
							
							
							mapDocumentGroupByPinstId.get(PINST_ID).get(DOCUMENT_TYPE).forEach( fileName -> {
								 System.out.println( "Document Name [" + fileName + "]");
								 imgProcessCount += 1 ;
								 uploadCnt +=1;
								 
								 
								 File file = new File(maskedImageUrl + File.separator +  dateFolder + File.separator + "masked" + File.separator + fileName);
								 if(file.exists()) {
									try {
										 String  docId="";
										
										 if(fileName.contains("ID_PROOF_FRONT")) {
											 docId = "4";
										 } else  if(fileName.contains("ID_PROOF_BACK")) {
											 docId = "5";
										 } else  if(fileName.contains("ADDRESS_PROOF_FRONT")) {
											 docId = "2";
										 } else  if(fileName.contains("ADDRESS_PROOF_BACK")) {
											 docId = "3";
										 } 
										 
										 
									     if(docId !=null && !"".equalsIgnoreCase(docId)) {
									    	 
									    	 String encodedString = finoService.encodeFileToBase64Binary(file);
											 
											 if(encodedString != null && !"".equalsIgnoreCase(encodedString) ) {
												
												 int uploadDocAPI = finoService.uploadDocAPI(PINST_ID, encodedString, docId , processId);
												 
												 logger.info("[SrvDocUtility]File upload status ::" + uploadDocAPI);
											
												 if (uploadDocAPI == 1) {
													
													File folder1 = new File(historyPath + File.separator +  dateFolder + File.separator + "masked");
													if (!folder1.exists()) {
														folder1.mkdir();
													}
													
													logger.info(" [DMS_AEPS_DocMasking] ===== NOW MOVING THE IMAGE TO AFTER MASKED IMAGE FOLDER==== ");
													finoService.moveFileToHistory(historyPath + File.separator +  dateFolder + File.separator + "masked", 
															maskedImageUrl + File.separator +  dateFolder + File.separator + "masked", fileName);
												
												}

											 } else {
												 logger.info("[SrvDocUtility]Document not found");
											 }
									     } 
										 
									} catch(Exception e){
										e.printStackTrace();
									}
								 }
							 });
							logger.info("[SrvDocUtility]PINST_ID ["+ PINST_ID   + "]  DOC_TYPE["+   DOCUMENT_TYPE + "]  Uploaded Count ["+uploadCnt+"]");
							
							if( uploadCntStatus.containsKey(PINST_ID)) {
								Map<String, Integer> map = uploadCntStatus.get(PINST_ID);
								map.put(DOCUMENT_TYPE, uploadCnt);
								uploadCntStatus.put(PINST_ID, map);
							} else {
								Map<String,Integer> map = new HashMap<>();
								map.put(DOCUMENT_TYPE, uploadCnt);
								uploadCntStatus.put(PINST_ID, map);
							}
							uploadCnt = 0;
						});
					}
				});
				
				logger.info("[SrvDocUtility]Updating status .. ");
				customNativeQuery.updateUploadStatusCnt(uploadCntStatus);
				logger.info("[SrvDocUtility]# #"+uploadCntStatus.toString());
				response.put("status", 0);
				response.put("message", "ok");
				response.put("nbs_cnt", nbsCount );
				response.put("upload_img_cnt", imgProcessCount);
				imgProcessCount = 0;
				nbsCount=0;
				SrvDocUtiltiy.nbsCount = 0;
			} else {
				logger.info("[SrvDocUtility]FILE DOES NOT EXIST");
				response.put("status", 0);
				response.put("message", "FILE DOES NOT EXIST");
				response.put("nbs_cnt", 0 );
				response.put("upload_img_cnt", 0);
				imgProcessCount = 0;
				nbsCount=0;
				SrvDocUtiltiy.nbsCount = 0;
			}
			
		} catch (Exception e) {
			logger.info("[SrvDocUtility]Exception ");
			e.printStackTrace();
			response.put("status", 1);
			response.put("message", "SYSTEM ERROR");
			imgProcessCount = 0;
			nbsCount=0;
		}
		return response;
	}

	
	public void removeFolderToArchieve(String sourcePath, String archivalPath, String dateFolder) {
	
		logger.info("[SrvDocUtility]Removing to Archival");
		try {
			boolean delStatus = false;
			logger.info("[SrvDocUtility]SourcePath [" + sourcePath + "]   archivalPath[" + archivalPath + "]  dateFolder["
					+ dateFolder + "]");

			String partiallyMsk = sourcePath + File.separator + dateFolder + File.separator + "partially_masked";
			String nonMasked = sourcePath + File.separator + dateFolder + File.separator + "non_masked";
			logger.info("[SrvDocUtility]partiallyMskFld[" + partiallyMsk + "]   \n nonMasked[" + nonMasked + "]");

			File maskedFld = new File(sourcePath + File.separator + dateFolder + File.separator + "masked");
			logger.info("[SrvDocUtility]maskedUrl: "+sourcePath + File.separator + dateFolder + File.separator + "masked");
			maskedFld.deleteOnExit();
			if(maskedFld.exists()) {
			maskedFld.delete();
			}
			// MASKED FOLDER REMOVE

			File partialMaskedFile = new File(partiallyMsk);
			File nonMaskedFld = new File(nonMasked);

			File archivalPartiallyMaskedFld = new File(
					archivalPath + File.separator + dateFolder + File.separator + "partially_masked");
			File archivalNonMaskedFld = new File(
					archivalPath + File.separator + dateFolder + File.separator + "non_masked");

			if (partialMaskedFile.exists()) {
				logger.info("[DMS_AEPS_DocMasking] COPYING PARTIALLY MASKED FOLDER");
				FileUtils.copyDirectory(partialMaskedFile, archivalPartiallyMaskedFld);
				File listOfFilesPartiallyMasked[] = partialMaskedFile.listFiles();
				for (int j = 0; j < listOfFilesPartiallyMasked.length; j++) {
					listOfFilesPartiallyMasked[j].delete();

				}
				partialMaskedFile.deleteOnExit();
				logger.info("[DMS_AEPS_DocMasking] DELETED FROM [" + partialMaskedFile.getAbsolutePath() + "]");
				if (partialMaskedFile.exists()) {
					delStatus = partialMaskedFile.delete();
				} else {
					logger.info("[DMS_AEPS_DocMasking]" + "PARTIALLY MASKED FOLDER NOT EXIST");
				}
				logger.info("[DMS_AEPS_DocMasking] PARTIALLY MASKED FOLDER DELETED STATUS: " + delStatus);
			} else {
				logger.info("[SrvDocUtility]PARTIALLY MASKED FOLDER NOT FOUND");
			}

			if (nonMaskedFld.exists()) {
				logger.info("MOVING NON MASKED TO ARCHIVAL");
				FileUtils.copyDirectory(nonMaskedFld, archivalNonMaskedFld);
				File listOfFilesNonMasked[] = nonMaskedFld.listFiles();
				for (int j = 0; j < listOfFilesNonMasked.length; j++) {
					listOfFilesNonMasked[j].delete();

				}
				nonMaskedFld.deleteOnExit();
				logger.info(" DELETED FROM [" + nonMaskedFld.getAbsolutePath() + "]");
				if (nonMaskedFld.exists()) {
					delStatus = nonMaskedFld.delete();
				} else {
					logger.info(" nonMaskedFld PARTIALLY MASKED FOLDER NOT EXIST");
				}
				logger.info("[DMS_AEPS_DocMasking] PARTIALLY MASKED FOLDER DELETED STATUS: " + delStatus);
			} else {
				logger.info("[SrvDocUtility]NON MASKED FOLDER NOT FOUND");
			}
			
			boolean delRootStat = false;
			if (!maskedFld.exists()) {
				logger.info("[SrvDocUtility]Going to delete SRC path");
				String rootFileUrl = sourcePath + File.separator + dateFolder;
				File rootFile = new File(rootFileUrl);

				rootFile.deleteOnExit();
				if (rootFile.exists()) {
					delRootStat = rootFile.delete();
				} else {
					logger.info("[DMS_AEPS_DocMasking]" + "NOT EXIST");
				}
				logger.info("[DMS_AEPS_DocMasking] ROOT FILE DELETED STATUS: " + delRootStat);
			} else {
				logger.info("[SrvDocUtility]SRC  ROOT EXIST" + maskedFld.exists());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
}
