package rs.make.alfresco.workflow.activiti.delegate.newsflash;

import java.io.Serializable;
import java.util.List;

import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.workflow.activiti.ActivitiScriptNode;
import org.alfresco.repo.workflow.activiti.BaseJavaDelegate;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;

import rs.make.alfresco.common.workflow.MakeWorkflowVars;

public class NewsFlashSend extends BaseJavaDelegate implements Serializable{
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger( NewsFlashSend.class );

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

	@Override
	public void execute( DelegateExecution execution ) throws Exception {
		logger.debug( "News flash, send." );
		String authenticatedUserName = AuthenticationUtil.getFullyAuthenticatedUser();
		AuthenticationUtil.setRunAsUserSystem();

		NodeRef newsFlash = getNewsFlash( execution );
		if( newsFlash == null ){
			String errorMessage = "Could not access news-flash. News-flash nodeRef is null.";
			logger.error( errorMessage );
			throw new BpmnError( "newsFlashError" , errorMessage );
		}

		List<String> to = getEmailTo( newsFlash );
		List<String> cc = getEmailCc( newsFlash );
		List<String> bcc = getEmailBcc( newsFlash );
		String subject = getEmailSubject( newsFlash );
		String body = getEmailBody( newsFlash );
		sendEmail( to , cc , bcc , subject , body );

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

	@SuppressWarnings("unchecked")
	private List<String> getEmailTo( NodeRef newsFlash ){
		NodeService nodeService = getNodeService();
		try{
			// get to recipients
			return (List<String>) nodeService.getProperty( newsFlash , NEWS_FLASH_TO_QNAME );
		}
		catch ( Exception e ) {
			String errorMessage = "Error occured whilst retrieving news-flash to recepients. " + e.getMessage();
			logger.error( errorMessage );
			throw new BpmnError( "newsFlashError" , errorMessage );
		}
	}

	@SuppressWarnings("unchecked")
	private List<String> getEmailCc( NodeRef newsFlash ){
		NodeService nodeService = getNodeService();
		try{
			// get cc recipients
			return (List<String>) nodeService.getProperty( newsFlash , NEWS_FLASH_CC_QNAME );
		}
		catch ( Exception e ) {
			String errorMessage = "Error occured whilst retrieving news-flash cc recepients. " + e.getMessage();
			logger.error( errorMessage );
			throw new BpmnError( "newsFlashError" , errorMessage );
		}
	}
	@SuppressWarnings("unchecked")
	private List<String> getEmailBcc( NodeRef newsFlash ){
		NodeService nodeService = getNodeService();
		try{
			// get bcc recipients
			return (List<String>) nodeService.getProperty( newsFlash , NEWS_FLASH_BCC_QNAME );
		}
		catch ( Exception e ) {
			String errorMessage = "Error occured whilst retrieving news-flash bcc recepients. " + e.getMessage();
			logger.error( errorMessage );
			throw new BpmnError( "newsFlashError" , errorMessage );
		}
	}

	private String getEmailSubject( NodeRef newsFlash ){
		NodeService nodeService = getNodeService();
		try{
			// get subject
			return (String) nodeService.getProperty( newsFlash , NEWS_FLASH_SUBJECT_QNAME );
		}
		catch ( Exception e ) {
			String errorMessage = "Error occured whilst retrieving news-flash subject. " + e.getMessage();
			logger.error( errorMessage );
			throw new BpmnError( "newsFlashError" , errorMessage );
		}
	}

	private String getEmailBody( NodeRef newsFlash ){
		ContentService contentService = getContentService();
		try{
			ContentReader contentReader = contentService.getReader( newsFlash , ContentModel.PROP_CONTENT );
			return contentReader.getContentString();
		}
		catch ( Exception e ) {
			String errorMessage = "Error occured whilst retrieving news-flash message body. " + e.getMessage();
			logger.error( errorMessage );
			throw new BpmnError( "newsFlashError" , errorMessage );
		}
	}

	private void sendEmail( List<String> to , List<String> cc , List<String> bcc , String subject , String body ){
		
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

	private ContentService getContentService(){
		ContentService contentService = getServiceRegistry().getContentService();
		if( contentService == null ){
			String errorMessage = "Content service could not be instanciated.";
			logger.error( errorMessage );
			throw new BpmnError( "newsFlashError" , errorMessage );
		}
		return contentService;
	}
}
