package rs.make.alfresco.workflow.activiti.delegate.newsflash;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.alfresco.util.Pair;
import org.apache.log4j.Logger;

import rs.make.alfresco.common.workflow.MakeWorkflowVars;
import rs.make.alfresco.mail.MakeMailSend;

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

	protected transient MakeMailSend makeMailSend;
	public MakeMailSend getMakeMailSend() {
		return makeMailSend;
	}
	public void setMakeMailSend( MakeMailSend makeMailSend ) {
		this.makeMailSend = makeMailSend;
	}

	private final String NEWS_FLASH_NAMESPACE_URI = "http://www.make.rs/model/newsflash/1.0";
	private final QName NEWS_FLASH_TO_QNAME = QName.createQName( NEWS_FLASH_NAMESPACE_URI , "to" );
	private final QName NEWS_FLASH_CC_QNAME = QName.createQName( NEWS_FLASH_NAMESPACE_URI , "cc" );
	private final QName NEWS_FLASH_BCC_QNAME = QName.createQName( NEWS_FLASH_NAMESPACE_URI , "bcc" );
	private final QName NEWS_FLASH_SUBJECT_QNAME = QName.createQName( NEWS_FLASH_NAMESPACE_URI , "subject" );
	private final QName NEWS_FLASH_ATTACHMENT_QNAME = QName.createQName( NEWS_FLASH_NAMESPACE_URI , "attachment" );

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
		Map<Pair<String,String>,byte[]> attachments = getEmailAttachments( newsFlash );
		sendEmail( to , cc , bcc , subject , body , attachments );
		markNodeAsSent( newsFlash , authenticatedUserName , to , cc , bcc , subject );

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

	private Map<Pair<String,String>,byte[]> getEmailAttachments( NodeRef newsFlash ){
		Map<Pair<String,String>,byte[]> attachments = new HashMap<Pair<String,String>,byte[]>();
		NodeService nodeService = getNodeService();
		ContentService contentService = getContentService();
		try{
			@SuppressWarnings("unchecked")
			ArrayList<NodeRef> nodeRefs = (ArrayList<NodeRef>) nodeService.getProperty( newsFlash , NEWS_FLASH_ATTACHMENT_QNAME );
			if( nodeRefs != null ){
				for( NodeRef nodeRef : nodeRefs ){
					String filename = (String) nodeService.getProperty( nodeRef , ContentModel.PROP_NAME );
					ContentReader contentReader = contentService.getReader( newsFlash , ContentModel.PROP_CONTENT );
					String mimetype = contentReader.getMimetype();
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					contentReader.getContent( outputStream );
					Pair<String,String> pair = new Pair<String,String>( filename , mimetype );
					attachments.put( pair , outputStream.toByteArray() );
				}
			}
			return ( attachments.size() > 0 ) ? attachments : null;
		}
		catch ( Exception e ) {
			String errorMessage = "Error occured whilst retrieving news-flash attachments. " + e.getMessage();
			logger.error( errorMessage );
			throw new BpmnError( "newsFlashError" , errorMessage );
		}
	}

	private void sendEmail( List<String> to , List<String> cc , List<String> bcc , String subject , String body, Map<Pair<String,String>,byte[]> attachments ){
		try {
			makeMailSend.init( ( ( to != null ) ? String.join( "," , to ) : null ) , ( ( cc != null ) ? String.join( "," , cc ) : null ) , ( ( bcc != null ) ? String.join( "," , bcc ) : null ) , subject , body , attachments , true );
		} catch ( Exception e ) {
			String errorMessage = "Error occured whilst sending news-flash. " + e.getMessage();
			logger.error( errorMessage );
			throw new BpmnError( "newsFlashError" , errorMessage );
		}
	}

	private void markNodeAsSent( NodeRef nodeRef , String authenticatedUserName , List<String> to , List<String> cc , List<String> bcc , String subject ){
		NodeService nodeService = getServiceRegistry().getNodeService();
		try {
			Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
			properties.put( ContentModel.PROP_ORIGINATOR , authenticatedUserName );
			properties.put( ContentModel.PROP_ADDRESSEE , String.format( "%s,%s,%s" , ( ( to != null ) ? String.join( "," , to ) : "" ) ,  ( ( cc != null ) ? String.join( "," , cc ) : "" ) ,  ( ( bcc != null ) ? String.join( "," , bcc ) : "" ) ) );
			properties.put( ContentModel.PROP_SUBJECT , subject );
			properties.put( ContentModel.PROP_SENTDATE , new Date() );
			nodeService.addAspect( nodeRef , ContentModel.ASPECT_EMAILED, properties );
		} catch ( Exception e ) {
			String errorMessage = "Error occured whilst marking news-flash as sent. " + e.getMessage();
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
