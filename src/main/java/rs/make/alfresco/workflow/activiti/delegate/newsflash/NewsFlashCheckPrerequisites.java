package rs.make.alfresco.workflow.activiti.delegate.newsflash;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.workflow.activiti.ActivitiScriptNode;
import org.alfresco.repo.workflow.activiti.BaseJavaDelegate;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;

import rs.make.alfresco.common.workflow.MakeWorkflowVars;

public class NewsFlashCheckPrerequisites extends BaseJavaDelegate implements Serializable{
	private static final long serialVersionUID = 1L;
	
	private static Logger logger = Logger.getLogger( NewsFlashCheckPrerequisites.class );

	protected transient MakeWorkflowVars makeWorkflowVars;
	public MakeWorkflowVars getMakeWorkflowVars() {
		return makeWorkflowVars;
	}
	public void setMakeWorkflowVars( MakeWorkflowVars makeWorkflowVars ) {
		this.makeWorkflowVars = makeWorkflowVars;
	}

	private final String NEWS_FLASH_NAMESPACE_URI = "http://www.make.rs/model/newsflash/1.0";
	private final QName NEWS_FLASH_TO_QNAME = QName.createQName( NEWS_FLASH_NAMESPACE_URI , "to" );
	private final QName NEWS_FLASH_CC_QNAME = QName.createQName( NEWS_FLASH_NAMESPACE_URI , "cc" );
	private final QName NEWS_FLASH_BCC_QNAME = QName.createQName( NEWS_FLASH_NAMESPACE_URI , "bcc" );
	private final QName NEWS_FLASH_SUBJECT_QNAME = QName.createQName( NEWS_FLASH_NAMESPACE_URI , "subject" );
	private final QName NEWS_FLASH_DATE_QNAME = QName.createQName( NEWS_FLASH_NAMESPACE_URI , "date" );

	private final String COMMON_EMAIL_SCRIPT_TASK_VARIABLE_PREFFIX = "makecestwf_";

	@Override
	public void execute( DelegateExecution execution ) throws Exception, BpmnError {
		logger.debug( "News flash, Check prerequisites." );
		String authenticatedUserName = AuthenticationUtil.getFullyAuthenticatedUser();
		AuthenticationUtil.setRunAsUserSystem();

		NodeRef newsFlash = getNewsFlash( execution );
		if( newsFlash == null ){
			String errorMessage = "Could not access news-flash. News-flash nodeRef is null.";
			logger.error( errorMessage );
			throw new BpmnError( "newsFlashError" , errorMessage );
		}
		setVars( execution , newsFlash );

		AuthenticationUtil.setRunAsUser( authenticatedUserName );
	}

	private NodeRef getNewsFlash( DelegateExecution execution ){
		NodeRef newsFlash = null;
		try{
			ActivitiScriptNode newsFlashActivitiScriptNode = (ActivitiScriptNode) makeWorkflowVars.getExecutionLocalVar( execution , "newsFlash" );
			newsFlash = newsFlashActivitiScriptNode.getNodeRef();
		}
		catch ( Exception e ) {
			String errorMessage = "Error occured whilst trying to retrieve news-flash. " + e.getMessage();
			logger.error( errorMessage );
			throw new BpmnError( "newsFlashError" , errorMessage );
		}
		return newsFlash;
	}

	private void setVars( DelegateExecution execution , NodeRef newsFlash ){
		NodeService nodeService = getNodeService();
		Map<String,Object> workflowCommonEmailScriptVars = new HashMap<String,Object>();
		try{
			// check recipients
			@SuppressWarnings("unchecked")
			ArrayList<String> to = (ArrayList<String>) nodeService.getProperty( newsFlash , NEWS_FLASH_TO_QNAME );
			@SuppressWarnings("unchecked")
			ArrayList<String> cc = (ArrayList<String>) nodeService.getProperty( newsFlash , NEWS_FLASH_CC_QNAME );
			@SuppressWarnings("unchecked")
			ArrayList<String> bcc = (ArrayList<String>) nodeService.getProperty( newsFlash , NEWS_FLASH_BCC_QNAME );
			if( ( to == null || to.size() == 0 ) && ( cc == null || cc.size() == 0 ) && ( bcc == null || bcc.size() == 0 ) ){
				String errorMessage = "No email recepients specified.";
				logger.error( errorMessage );
				throw new BpmnError( "newsFlashError" , errorMessage );
			}

			// check subject
			String subject = (String) nodeService.getProperty( newsFlash , NEWS_FLASH_SUBJECT_QNAME );
			if( subject == null ){
				String errorMessage = "No email subject specified.";
				logger.error( errorMessage );
				throw new BpmnError( "newsFlashError" , errorMessage );
			}

			// check date
			String dateAsISO = null;
			Date date = (Date) nodeService.getProperty( newsFlash , NEWS_FLASH_DATE_QNAME );
			if( date != null ){
				TimeZone tz = TimeZone.getTimeZone( "UTC" );
				DateFormat df = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm'Z'" ); // Quoted "Z" to indicate UTC, no timezone offset
				df.setTimeZone(tz);
				dateAsISO = df.format( date );
			}
			workflowCommonEmailScriptVars.put( "sendDate" , dateAsISO );

			makeWorkflowVars.setExecutionLocalVars( execution , workflowCommonEmailScriptVars , COMMON_EMAIL_SCRIPT_TASK_VARIABLE_PREFFIX );
		}
		catch ( Exception e ) {
			String errorMessage = "Error occured whilst validating news-flash parameters. " + e.getMessage();
			logger.error( errorMessage );
			throw new BpmnError( "newsFlashError" , errorMessage );
		}
	}

	private NodeService getNodeService(){
		NodeService nodeService = getServiceRegistry().getNodeService();
		if( nodeService == null ){
			String errorMessage = "Node service could not be instanciated.";
			logger.error( errorMessage );
			throw new BpmnError( "newsFlashError" , errorMessage );
		}
		return nodeService;
	}
}
