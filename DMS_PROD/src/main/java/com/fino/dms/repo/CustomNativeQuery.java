package com.fino.dms.repo;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CustomNativeQuery {

	 @PersistenceContext
	 EntityManager entityManager;
	 
	 Logger logger = LoggerFactory.getLogger(CustomNativeQuery.class);

	 @Transactional
     public int doSomeQuery(String str){
        Query query = entityManager.createNativeQuery(str);
        int executeUpdate = query.executeUpdate();
		return executeUpdate;
     }
	 
	 
	 @Transactional
     public int unMaskVersionDataToHistory(String nbsName,String documentType ){
		 String docType = documentType;
		 logger.info("[CustomNativeQuery] nbsName[" +nbsName + "]  documentType["+ documentType + "]" );
		 int executeUpdate = 0; 
         try {
        	 
        	 if (documentType.equalsIgnoreCase("ADDRESS_PROOF_FRONT")) {
        		 documentType = "ADDRESS PROOF FRONT";
 			} else if (documentType.equalsIgnoreCase("ADDRESS_PROOF_BACK")) {
 				documentType = "ADDRESS PROOF BACK" ;
 			} else if (documentType.equalsIgnoreCase("ID_PROOF_FRONT")) {
 				documentType = "ID PROOF Front" ;
 			} else if (documentType.equalsIgnoreCase("ID_PROOF_BACK")) {
 				documentType = "ID PROOF Back" ;
 			}
 			
 			//Query taking backup of old un mask image
        	 Query query = entityManager.createNativeQuery("INSERT INTO SDM_NODE_UNMASK_DOC_VERSION SELECT * FROM  SDM_NODE_DOCUMENT_VERSION "
             		+ " WHERE NDV_DOCUMENT IN("
             		+ " SELECT NBS_UUID FROM SDM_NODE_DOCUMENT "
             		+ " WHERE NBS_PARENT IN (SELECT NBS_UUID FROM SDM_NODE_FOLDER WHERE NBS_NAME = ?1) "
             		+ " AND NBS_NAME = ?2 "
             		+ " )");
             query.setParameter(1, nbsName);
             query.setParameter(2, documentType);
             executeUpdate = query.executeUpdate();
             logger.info("[CustomNativeQuery]Backup for NBS["+nbsName+"] DocType["+documentType+"]  CNT["+executeUpdate+"]");
             
             if( executeUpdate > 0) {
            	 
            	 //Query to remove old unmask images from primary table
            	 Query query1 = entityManager.createNativeQuery(" DELETE FROM SDM_NODE_DOCUMENT_VERSION "
                  		+ " WHERE NDV_DOCUMENT IN("
                  		+ " SELECT NBS_UUID FROM SDM_NODE_DOCUMENT "
                  		+ " WHERE NBS_PARENT IN (SELECT NBS_UUID FROM SDM_NODE_FOLDER WHERE NBS_NAME = ?1) "
                  		+ " AND NBS_NAME = ?2 "
                  		+ " )");
            	  query1.setParameter(1, nbsName);
                  query1.setParameter(2, documentType);
                  int unMaskRemovedCnt = query1.executeUpdate();
                  logger.info("[CustomNativeQuery]Removed CNT["+unMaskRemovedCnt+"]");
            	 
            	 
                  //Query to update OLD_VERSION_REMOVED to Y  
                  System.out.println(nbsName + "   "+docType );
            	 Query query2 = entityManager.createNativeQuery("UPDATE FINO_FRSLAB_MASK_IMG_STATUS SET OLD_VERSION_REMOVED = ?1 WHERE NBS_NAME = ?2 AND DOC_TYPE = ?3 ");
            	 query2.setParameter(1, "Y");
            	 query2.setParameter(2, nbsName);
            	 query2.setParameter(3, documentType);// 
            	 int statusUpdateCnt = query2.executeUpdate();
            	 logger.info("[CustomNativeQuery] FINO_FRSLAB_MASK_IMG_STATUS UPDATE_CNT "+statusUpdateCnt);
             }
     		return executeUpdate;
		} catch (Exception e) {
			logger.error("[CustomNativeQuery][unMaskVersionDataToHistory] Exception with message [" + e.getMessage() + "]");
			e.printStackTrace();
		}
         return executeUpdate;
         
     }
	 

	 
	
    @Transactional
	public String getOldVersionRemovedStatus(String nbsName,String docType) {
		 String status = "",documentType = "";
		 logger.info("[CustomNativeQuery]nbsName " + nbsName + "    "+docType);
		 
		 try {
			 
			 
			 if (docType.equalsIgnoreCase("ADDRESS_PROOF_FRONT")) {
        		 documentType = "ADDRESS PROOF FRONT";
 			} else if (docType.equalsIgnoreCase("ADDRESS_PROOF_BACK")) {
 				documentType = "ADDRESS PROOF BACK" ;
 			} else if (docType.equalsIgnoreCase("ID_PROOF_FRONT")) {
 				documentType = "ID PROOF Front" ;
 			} else if (docType.equalsIgnoreCase("ID_PROOF_BACK")) {
 				documentType = "ID PROOF Back" ;
 			}
 			
			 
			 
			Query query = entityManager.createNativeQuery("SELECT OLD_VERSION_REMOVED FROM FINO_FRSLAB_MASK_IMG_STATUS WHERE NBS_NAME = ?1 AND DOC_TYPE = ?2");
			query.setParameter(1, nbsName);
			query.setParameter(2, documentType);
			List<Object[]> results = query.getResultList();
			
			logger.info("[CustomNativeQuery]Result Size "+results.size());
			
			if(results.size() > 0) {
				System.out.println(results.get(0)); 
				status = results.get(0) +"";
			}
		
		} catch (Exception e) {
			logger.error("" + e.getMessage());
			e.printStackTrace();
		}
		 return status;
	 }
    
    
    @Transactional
    public void updateUploadStatusCnt(Map<String,Map<String,Integer>> uploadStatusCntMap) {
    	
    	try {
	
    		Set<String> keySet = uploadStatusCntMap.keySet();
    		for(String nbsName : keySet) {
    			//logger.info("[CustomNativeQuery][updateUploadStatusCnt] Updating FINO_FRSLAB_MASK_IMG_STATUS NBS_NAME  [" + nbsName +"]");
    			Map<String, Integer> map = uploadStatusCntMap.get(nbsName);
    			map.forEach((k,v) ->{
    				String docType = "";
    			
    				if (k.equalsIgnoreCase("ADDRESS_PROOF_FRONT")) {
    					 docType = "ADDRESS PROOF FRONT";
    	 			} else if (k.equalsIgnoreCase("ADDRESS_PROOF_BACK")) {
    	 				docType = "ADDRESS PROOF BACK" ;
    	 			} else if (k.equalsIgnoreCase("ID_PROOF_FRONT")) {
    	 				docType = "ID PROOF Front" ;
    	 			} else if (k.equalsIgnoreCase("ID_PROOF_BACK")) {
    	 				docType = "ID PROOF Back" ;
    	 			}
    				 logger.info("[CustomNativeQuery][updateUploadStatusCnt ]NBS_NAME " + nbsName + "] DOC_NAME [" + docType + "]  UPLOAD_CNT[" + v + "]");
    			   
    				Query query = entityManager.createNativeQuery("UPDATE FINO_FRSLAB_MASK_IMG_STATUS SET IMG_UPLOAD_CNT = ?1,UPLOAD_TIME = GETDATE() "
    						+ " WHERE  NBS_NAME = ?2 AND DOC_TYPE = ?3 ");
    						
    				query.setParameter(1, v);
    				query.setParameter(2, nbsName);
    				query.setParameter(3,docType);
    				
    				int updateCnt = query.executeUpdate();
    				logger.info("[CustomNativeQuery]Update Cnt " + updateCnt );
    				
    			});
    		}
    	
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    }
    
    
    
    
    
    
    
    
    
    
    
	 
	 
	 
	 
	 
	 
}
