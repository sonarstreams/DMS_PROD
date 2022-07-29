package com.fino.dms.service;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
//Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
public class FinoService {

	@PersistenceContext
	EntityManager entityManager;

	Logger logger = LoggerFactory.getLogger(FinoService.class);
	
	@Value("${dms.downloadUrl}")
	private String dmsGetDocDataUrl;
	
	@Value("${dms.uploadDocUrl}")
	private String uploadDocUrl;
	
	@Value("${dms.removeOldVersionUrl}")
	private String dmsRemoveAadharOlderVersionUrl;

	@SuppressWarnings("unchecked")
	public int downloadAadharPhoto(String requestJson) {

		logger.info("==  Inside downloadAadharPhoto === [" + requestJson + "]");
		//String downloadUrl = "http://10.15.2.134:8080/servows/getDocData";
		
		int status = 0;
		String nbsName = "", documentName = "", docType = "", destPath = "";
		try {
			if (requestJson != null && !"".equalsIgnoreCase(requestJson)) {

				JSONParser parser = new JSONParser();
				JSONObject requestJSONObject = (JSONObject) parser.parse(requestJson);

				nbsName = requestJSONObject.get("nbsName") + "";
				documentName = requestJSONObject.get("documentName") + "";
				docType = requestJSONObject.get("docType") + "";
				destPath = requestJSONObject.get("downloadDirectory") + "";

				RestTemplate restTemplate = new RestTemplate();
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_JSON);

				JSONObject requestObject = new JSONObject();
				requestObject.put("nbsName", nbsName);
				requestObject.put("documentName", documentName);
				requestObject.put("parentFolderID", requestJSONObject.get("parentFolderID"));

				logger.info("requestObject " + requestObject.toString());
				logger.info("Download URL "+dmsGetDocDataUrl);
				HttpEntity<String> request = new HttpEntity<String>(requestObject.toString(), headers);
				ResponseEntity<String> response1 = restTemplate.exchange(dmsGetDocDataUrl, HttpMethod.POST, request,
						String.class);
				String res = response1.getBody();

				JSONParser parser1 = new JSONParser();
				JSONObject parseObject2 = (JSONObject) parser1.parse(res);

				Optional<String> docStr = Optional.ofNullable(parseObject2.get("DocumentString"))
						.map(node -> node.toString());

				if (docStr.isPresent()) {
					String documentString = docStr.get();
					boolean receivedFlag = saveFile(documentString, destPath, nbsName, documentName, docType);
					if (receivedFlag) {
						status = 1;
					} else {
						status = 0;
					}

				} else {
					return 0;
				}

			} else {
				return 0;
			}

		} catch (Exception e) {
			logger.error("Exception while downloading the document" + e.getMessage());
			logger.error("Exception while downloading the document ",e);
			e.printStackTrace();
			return 0;
		}
		return status;

	}

	public boolean saveFile(String documentString, String destPath, String nbsName, String docName, String extension) {
		boolean retFlag = false;

		try {

			byte[] imageBytes = javax.xml.bind.DatatypeConverter.parseBase64Binary(documentString);
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
			BufferedImage transperancyImage= ensureOpaque(image);
			String time = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());

			File unmasked_dir = new File(destPath + File.separator + time);

			if (!unmasked_dir.exists()) {
				unmasked_dir.mkdirs();
			}

			if (docName.equalsIgnoreCase("ADDRESS PROOF FRONT")) {
				docName = "ADDRESS_PROOF_FRONT";
			}
			if (docName.equalsIgnoreCase("ADDRESS PROOF BACK")) {
				docName = "ADDRESS_PROOF_BACK";
			}

			String path = "" + destPath + File.separator + time + File.separator + nbsName + "_" + docName + "."
					+ extension;
			logger.error("[DMSImageProcessing] IMAGE WILL BE AT : " + path);

			File outputfile = new File(path);
			retFlag = ImageIO.write(transperancyImage, extension, outputfile);
			logger.error("retFlag  " + retFlag);

		} catch (Exception e) {
			logger.error("Exception while saving the document ",e);
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

	@SuppressWarnings("unchecked")
	public int uploadAadharPhoto(String requestJson) {
 
		logger.error(" <<< ==============  Inside uploadAadharPhoto ======= >>> [" + requestJson + "]");
		int counter = 0;
		int status = 0;
		Long uniqueIdLength = 0l;
		String encodstring = null;
		String pinstIdList = null;
		String documentTypeID = null;
		String processId = "";
		String afterMaskedImageUrl = "", partiallyMaskedPath = "", unMaskedSourcePath = "", maskedImageUrl = "",
				maskedSourcePath = "",timerServiceType = "";
		try {
			if (requestJson != null && !"".equalsIgnoreCase(requestJson)) {

				JSONParser parser = new JSONParser();
				JSONObject requestJSONObject = (JSONObject) parser.parse(requestJson);

				uniqueIdLength =   (Long) requestJSONObject.get("uniqueIdLength");
				processId = requestJSONObject.get("processId") + "";
				maskedImageUrl = requestJSONObject.get("maskedImageUrl") + "";
				afterMaskedImageUrl = requestJSONObject.get("afterMaskedImageUrl") + "";
				timerServiceType = requestJSONObject.get("timerServiceType") + "";
			    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
				logger.info(" timerServiceType [" + timerServiceType + "]" );
	

				ArrayList<String> results = new ArrayList<String>(10);
				File outputFolder = new File(maskedImageUrl);
				File[] listOfOutputFolders = outputFolder.listFiles();
				if (listOfOutputFolders.length > 0) {
					for (int i = 0; i < listOfOutputFolders.length; i++) {
						
						if( counter == 10) {
							break;
						}
						String time = listOfOutputFolders[i].getName();
						logger.info("[DMS_AEPS_DocMasking]" + "[DMS_AEPS_DocMasking]WORKING ON FOLDER " + time);

						String sourceFile = maskedImageUrl + File.separator + time + File.separator + "masked";
						String historyPath = afterMaskedImageUrl + File.separator + time + File.separator + "masked";
						
						File folder = new File(sourceFile);
						logger.info("[DMS_AEPS_DocMasking] EXTRACTING ALL IMAGES PRESENT IN LOCATION [" + sourceFile + "]");
						File[] listOfFiles = folder.listFiles();
						int status1 = 1;
						// reading filenames
						
						if( listOfFiles != null && listOfFiles.length > 0) {
							logger.info("[DMS_AEPS_DocMasking]  timerServiceType ["+ timerServiceType + "] ");
							if("T1".equalsIgnoreCase(timerServiceType)) {
								Arrays.sort(listOfFiles, Comparator.comparingLong(File::lastModified));
								
							} else if("T2".equalsIgnoreCase(timerServiceType)) {
								Arrays.sort(listOfFiles, Comparator.comparingLong(File::lastModified).reversed());
							}
							
							for(int k=0;k<listOfFiles.length;k++){
								logger.info(" File Name : "+ listOfFiles[k].getName()+" Time : " +sdf.format(listOfFiles[k].lastModified())+" ");
							}
						}

						
						if (listOfFiles != null && listOfFiles.length > 10) {
							
							for (int k = 0; k < 10; k++) {
								if (listOfFiles[k].isFile()) {
									logger.info("[DMS_AEPS_DocMasking] FILES PRESENT ARE [ "+ listOfFiles[k].getName() + " ]");
									results.add(listOfFiles[k].getName());
								}
							}
						} else if (listOfFiles != null && listOfFiles.length > 0) {
							
							for (int j = 0; j < listOfFiles.length; j++) {
								if (listOfFiles[j].isFile()) {
									logger.info("[DMS_AEPS_DocMasking] FILES PRESENT ARE [ "+ listOfFiles[j].getName() + " ]");
									results.add(listOfFiles[j].getName());
								}
							}
						} else if(listOfFiles == null){
							logger.info("Images not found");
							return 0;
						}

						logger.info("[DMS_AEPS_DocMasking]" + "[DMS_AEPS_DocMasking] FILES PRESENT ARE: ["+ listOfFiles.length + " ]");
						if (listOfFiles.length == 0) {
							logger.info("[DMS_AEPS_DocMasking]" + "[DMS_AEPS_DocMasking] FILES PRESENT ARE: ["+ listOfFiles.length + " ]");
							logger.info("[DMS_AEPS_DocMasking]" + "[DMS_AEPS_DocMasking] DELETING MASK FOLDER");
							folder.deleteOnExit();
							if (folder.exists()) {
								folder.delete();
							} else {
								logger.info("[DMS_AEPS_DocMasking]" + "MASKED FOLDER NOT EXIST");
							}

							String sourceFileNonMaskedUrl = maskedImageUrl + File.separator + time + File.separator + "non_masked";
							String destFileNonMaskedUrl = afterMaskedImageUrl + File.separator + time + File.separator + "non_masked";

							File sourceFileNonMasked = new File(sourceFileNonMaskedUrl);
							File destFileNonMasked = new File(destFileNonMaskedUrl);
							if (!destFileNonMasked.exists()) {
								
								destFileNonMasked.mkdir();
							}
							boolean delStat = false;
							if (sourceFileNonMasked.exists()) {
								logger.info("[DMS_AEPS_DocMasking] COPYING NON MASKED FOLDER");
								FileUtils.copyDirectory(sourceFileNonMasked, destFileNonMasked);
								File listOfsourceFileNonMasked[] = sourceFileNonMasked.listFiles();
								for (int j = 0; j < listOfsourceFileNonMasked.length; j++) {
									listOfsourceFileNonMasked[j].delete();

								}
								sourceFileNonMasked.deleteOnExit();
								logger.info("[DMS_AEPS_DocMasking]" + "DELETED FROM "+ sourceFileNonMasked.getAbsolutePath());
								if (sourceFileNonMasked.exists()) {
									delStat = sourceFileNonMasked.delete();
								} else {
									logger.info("[DMS_AEPS_DocMasking]" + "NON MASKED FOLDER NOT EXIST");
								}
								logger.info(" NON MASKED FOLDER DELETED STATUS: " + delStat);

							}

							String sourceFilePartillyMaskedUrl = maskedImageUrl + File.separator + time + File.separator+ "partially_masked";
							String destFilePartillyMaskedUrl = afterMaskedImageUrl + File.separator + time+ File.separator + "partially_masked";

							File sourceFilePartillyMasked = new File(sourceFilePartillyMaskedUrl);
							File destFilePartillyMasked = new File(destFilePartillyMaskedUrl);
							if (!destFilePartillyMasked.exists()) {
								destFilePartillyMasked.mkdir();
							}

							boolean delStatus = false;

							if (sourceFilePartillyMasked.exists()) {
								logger.info("[DMS_AEPS_DocMasking] COPYING PARTIALLY MASKED FOLDER");
								FileUtils.copyDirectory(sourceFilePartillyMasked, destFilePartillyMasked);
								File listOfFilesPartiallyMasked[] = sourceFilePartillyMasked.listFiles();
								for (int j = 0; j < listOfFilesPartiallyMasked.length; j++) {
									listOfFilesPartiallyMasked[j].delete();

								}
								sourceFilePartillyMasked.deleteOnExit();
								logger.info("[DMS_AEPS_DocMasking] DELETED FROM [" + sourceFilePartillyMasked.getAbsolutePath() + "]");
								if (sourceFilePartillyMasked.exists()) {
									delStatus = sourceFilePartillyMasked.delete();
								} else {
									logger.info("[DMS_AEPS_DocMasking]" + "PARTIALLY MASKED FOLDER NOT EXIST");
								}
								logger.info("[DMS_AEPS_DocMasking] PARTIALLY MASKED FOLDER DELETED STATUS: " + delStatus);
							}
							boolean delRootStat = false;
							if (!folder.exists()) {
								String rootFileUrl = maskedImageUrl + File.separator + time;
								File rootFile = new File(rootFileUrl);

								rootFile.deleteOnExit();
								if (rootFile.exists()) {
									delRootStat = rootFile.delete();
								} else {
									logger.info("[DMS_AEPS_DocMasking]" + "NOT EXIST");
								}
								logger.info("[DMS_AEPS_DocMasking] ROOT FILE DELETED STATUS: " + delRootStat);
							}

						}
						logger.info("[DMS_AEPS_DocMasking] FILES ADDED IN RESULT ARRAY_LIST ]" + results);
						String[] str = new String[results.size()];
						for (int l = 0; l < results.size(); l++) {
							str[l] = results.get(l);
						}
						logger.info("[DMS_AEPS_DocMasking] NUMBER OF FILES PRESENT IN MASKED FOLDER " + str.length);

						for (int m = 0; m < str.length; m++) {
							File f = new File(sourceFile + File.separator + str[m]);
							encodstring = encodeFileToBase64Binary(f);
							//System.out.println("[DMS_AEPS_DocMasking] ENCODING STRING " + encodstring);
							pinstIdList = str[m].substring(0, uniqueIdLength.intValue());
							logger.error("[DMS_AEPS_DocMasking] PINSTID/CUSTOMER EXTRACTED FROM LIST: " + pinstIdList);
							if (str[m].contains("ADDRESS_PROOF_FRONT")) {
								logger.info("[DMS_AEPS_DocMasking] CONTAINS ADHAR CARD FRONT IMAGE ["+ str[m].contains("FRONT") + "]");
								documentTypeID = "2";
								status1 = uploadDocAPI(pinstIdList, encodstring, documentTypeID, processId);
								logger.info("[DMS_AEPS_DocMasking] IMAGE UPLOADING SATUS AFTER API CALL[" + status1 + "]");
								if (status1 == 1) {

									int aadharVersionDeleteStatus = removeOlderVersionAadharImage(pinstIdList,documentTypeID,"R");
									//int aadharVersionDeleteStatus = 1;
									logger.info("[DMS_AEPS_DocMasking] ALL PRREVIOUS VERSIONS DELETED STATUS ["+ aadharVersionDeleteStatus + "]");
									if (aadharVersionDeleteStatus == 1) {
										String path = historyPath;

										File folder1 = new File(path);

										if (!folder1.exists()) {
											folder1.mkdir();
										}
										logger.info("[DMS_AEPS_DocMasking] ===== NOW MOVING THE IMAGE TO AFTER MASKED IMAGE FOLDER==== ");
										moveFileToHistory(historyPath, sourceFile, str[m]);
										int imageMovedStatus = removeOlderVersionAadharImage(pinstIdList,documentTypeID,"M");
										logger.info("[DMS_AEPS_DocMasking] IMAGE MOVED STATUS UPDATE IN AUDIT TABLE IS "+ imageMovedStatus);
									}
								}

							} else if (str[m].contains("ADDRESS_PROOF_BACK")) {
								logger.info("[DMS_AEPS_DocMasking] CONTAINS ADHAR CARD BACK IMAGE ["+ str[m].contains("BACK") + "]");
								documentTypeID = "3";
								status = uploadDocAPI(pinstIdList, encodstring, documentTypeID, processId);
								logger.info("[DMS_AEPS_DocMasking] IMAGE UPLOADING SATUS AFTER API CALL [" + status + "]");
								if (status == 1) {
									int aadharVersionDeleteStatus  = removeOlderVersionAadharImage(pinstIdList,documentTypeID,"R");
									//int aadharVersionDeleteStatus  = 1;
									logger.info(" [DMS_AEPS_DocMasking] ALL PRREVIOUS VERSIONS DELETED STATUS ["+ aadharVersionDeleteStatus + "]");
									if (aadharVersionDeleteStatus == 1) {
										String path = afterMaskedImageUrl;

										File folder1 = new File(path);

										if (!folder1.exists()) {
											folder1.mkdir();
										}
										logger.info(" [DMS_AEPS_DocMasking] ===== NOW MOVING THE IMAGE TO AFTER MASKED IMAGE FOLDER==== ");
										moveFileToHistory(historyPath, sourceFile, str[m]);
										int imageMovedStatus = removeOlderVersionAadharImage(pinstIdList,documentTypeID,"M");
										logger.error("[DMS_AEPS_DocMasking] IMAGE MOVED STATUS UPDATE IN AUDIT TABLE IS ["+ imageMovedStatus + "]");
									}
								}
							}
							status = 1;
						}
						counter++;
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Uploading the document" + e.getMessage());
			logger.error("Exception in uploadAadharPhoto ", e);
			e.printStackTrace();
			return 0;
		}
		return status;

	}

	public String encodeFileToBase64Binary(File file) throws Exception {
		try (FileInputStream fileInputStreamReader = new FileInputStream(file)) {
			byte[] bytes = new byte[(int) file.length()];
			fileInputStreamReader.read(bytes);
			return new String(Base64.encodeBase64(bytes), "UTF-8");
		}
	}

	
	@SuppressWarnings("unchecked")
	public int uploadDocAPI(String pinstId, String encodstring, String documentTypeID, String processId) {
		int status = 0;
		try {
			logger.info("[uploadDocAPI] ============= uploadDocAPI Called ==================");
			JSONObject reqJSON = new JSONObject();
			JSONObject documentDetailsJson = new JSONObject();
			JSONArray array = new JSONArray();
			documentDetailsJson.put("documentTemplate", "" + encodstring);
			documentDetailsJson.put("documentTypeID", "" + documentTypeID);
			array.add(documentDetailsJson);
			reqJSON.put("documentDetails", array);
			reqJSON.put("accountNumber", "");
			reqJSON.put("customerNumber", "" + pinstId);
			reqJSON.put("processId", processId);
			reqJSON.put("pinstId", "" + pinstId);
			reqJSON.put("mobile", "");
			logger.info("[DMS_AEPS_DocMasking][uploadDocAPI] REQUEST: " + reqJSON.toString());
			int restAPIStatus = restAPI(reqJSON  + "");
			status = restAPIStatus;
		} catch (Exception ex) {
			logger.error("[DMS_AEPS_DocMasking]" + "[DMS_AEPS_DocMasking] Exception " + ex.getMessage());
			logger.error("Exception in uploadDocAPI ", ex);
			ex.printStackTrace();
		}
		return status;
	}

	public int restAPI(String request) {
		String response = "";
		JSONObject emptyJSON = new JSONObject();
		int status = 0;
		StringBuilder str = new StringBuilder();
		HttpURLConnection connection = null;
		OutputStreamWriter out = null;
		BufferedReader in = null;
		try {

			JSONParser parser = new JSONParser();
			JSONObject requestJSONObject = (JSONObject) parser.parse(request);
			//logger.info("[DMS_AEPS_DocMasking]" + "[DMS_AEPS_DocMasking] jsonObject" + requestJSONObject);
			URL url = new URL(uploadDocUrl);
			//logger.info("[DMS_AEPS_DocMasking]" + "[DMS_AEPS_DocMasking] uploadDoc API URL :: " + url);
			connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/json");

			out = new OutputStreamWriter(connection.getOutputStream());
			out.write(requestJSONObject.toString());
			out.close();
			in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			while ((response = in.readLine()) != null) {
				str = str.append(response);
			}

			if (str.length() > 0) {
				String respStr = str.toString();
				if (!"{}".equalsIgnoreCase(respStr) && !"".equalsIgnoreCase(respStr) && respStr != null) {
					logger.info("[DMS_AEPS_DocMasking] ==== RESPONSE FOUND ==== " + respStr);
					status = 1;
				} else {
					logger.info("[DMS_AEPS_DocMasking] Empty Response" + respStr);
				}
			}

			in.close();
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("[DMS_AEPS_DocMasking]" + "[DMS_AEPS_DocMasking] Exception while calling restAPI" + e);
			logger.error("Exception in REST API",e);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					logger.error("Exception in REST API 1 ",e);
				}
			}

			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					logger.error("Exception in REST API 2 ",e);
				}
			}

			if (connection != null) {
				connection.disconnect();
			}
		}
		return status;
	}

	@Transactional
	public void updatePersonDetails(String address) {
		logger.info("Updating Person table data" + address);
		try {
			Query createQuery = entityManager.createQuery("UPDATE Person SET Address = ?1 WHERE PersonID = ?2");
			createQuery.setParameter(1, address);
			createQuery.setParameter(2, "1");
			int executeUpdate = createQuery.executeUpdate();
			logger.info("Update Personal Address Details " + executeUpdate);
		} catch (Exception e) {
			logger.error("Error in Updating Address details", e);
			e.printStackTrace();
		}
	}

	public void moveFileToHistory(String historyPath, String sourceFile, String fileName) {

		try {

			logger.info("[moveFileToHistory] SRC_PATH[" + sourceFile + "] DEST_PATH [" + historyPath+ "]  FILE_NAME[" + fileName + "]");
			File source = new File(sourceFile + File.separator + fileName);
			File dest = new File(historyPath);
			if (!dest.exists()) {
				dest.mkdirs();
			}
			if (source.exists()) {
				FileUtils.copyFileToDirectory(source, dest);
				logger.info("moveFileToHistory FILE SUCCESSFULLY MOVED TO [" + dest + "] FOLDER");
				logger.info("[moveFileToHistory] FILE EXIST IN SOURCE FOLDER [" + source.exists() + "]");
				if (source.exists()) {
					System.gc();// Added this part
					Thread.sleep(2000);// This part gives the Bufferedreaders and the InputStreams time to close Completely
					source.delete();
					logger.info("[moveFileToHistory] STILL FILE EXISTS IN SOURCE FOLDER ["+ source.exists()+"]");
				}
			}

		} catch (Exception e) {
			logger.error(e.getMessage());
			logger.error("Error While moving file to history folder ", e);
			e.printStackTrace();
		}

	}
	
	
	
	@SuppressWarnings("unchecked")
	public int removeOlderVersionAadharImage(String pinstIdList,String documentTypeID,String callId) {
		int returnVal = 0;
		try {
			
			//String url = "http://10.15.2.134:8080/servows/RemoveOlderVersionOfAadhar";
			RestTemplate restTemplate = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			JSONObject requestObject = new JSONObject();
			requestObject.put("pinstIdList", pinstIdList);
			requestObject.put("documentTypeID", documentTypeID);
			requestObject.put("callId", callId);

			logger.info("removeOlderVersionAadharImage requestObject " + requestObject.toString());
			
			HttpEntity<String> request = new HttpEntity<String>(requestObject.toString(), headers);
			ResponseEntity<String> response1 = restTemplate.exchange(dmsRemoveAadharOlderVersionUrl, HttpMethod.POST, request,
					String.class);
			String res = response1.getBody();
			
			logger.info("removeOlderVersionAadharImage ["+ res + "]");
			JSONParser parser1 = new JSONParser();
			JSONObject parseObject2 = (JSONObject) parser1.parse(res);

			Long returnCode = (Long) parseObject2.get("returnCode");
			logger.info("returnCode["+returnCode+"]");
			returnVal = returnCode.intValue();
			
		} catch (Exception e) {
			logger.info("Error while removing older version of aadhar image ", e);
			logger.error("Error While removing ", e);
		}
		
		return returnVal;
	}
	
	
}