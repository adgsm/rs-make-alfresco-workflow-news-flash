package rs.make.alfresco.workflow.newsflash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.workflow.WorkflowModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.workflow.WorkflowInstance;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;

import rs.make.alfresco.common.locale.MakeLocale;
import rs.make.alfresco.common.message.MakeMessage;
import rs.make.alfresco.common.status.MakeStatus;

public class CancelNewsFlashWorkflow extends DeclarativeWebScript{
	private static Logger logger = Logger.getLogger( CancelNewsFlashWorkflow.class );

	private final String QUERY_NEWS_FLASH_KEY = "news-flash";
	private static String ACTIVITI_PREFIX = "activiti$";

	private final String NEWS_FLASH_NAMESPACE_URI = "http://www.make.rs/model/newsflash/1.0";
	private final QName NEWS_FLASH_DATE_QNAME = QName.createQName( NEWS_FLASH_NAMESPACE_URI , "date" );

	protected MakeLocale makeLocale;
	public MakeLocale getMakeLocale() {
		return makeLocale;
	}
	public void setMakeLocale( MakeLocale makeLocale ) {
		this.makeLocale = makeLocale;
	}

	protected MakeStatus makeStatus;
	public MakeStatus getMakeStatus() {
		return makeStatus;
	}
	public void setMakeStatus( MakeStatus makeStatus ) {
		this.makeStatus = makeStatus;
	}

	protected NodeService nodeService;
	public NodeService getNodeService() {
		return nodeService;
	}
	public void setNodeService( NodeService nodeService ) {
		this.nodeService = nodeService;
	}

	protected WorkflowService workflowService;
	public WorkflowService getWorkflowService() {
		return workflowService;
	}
	public void setWorkflowService( WorkflowService workflowService ) {
		this.workflowService = workflowService;
	}

	private boolean statusThrown = false;

	@SuppressWarnings("unchecked")
	@Override
	protected Map<String, Object> executeImpl( WebScriptRequest req , Status status , Cache cache ) {
		Map<String, Object> model = new HashMap<String, Object>();
		try{
			WebScript webscript = req.getServiceMatch().getWebScript();
			MakeMessage message = new MakeMessage( webscript );

			String authenticatedUserName = AuthenticationUtil.getFullyAuthenticatedUser();

			AuthenticationUtil.setRunAsUserSystem();
			List<NodeRef> newsFlashList = parseNewsFlashParameter( req , status , message );
			List<String> cancelled =  cancelWorkflow( newsFlashList );
			setDateToNull( newsFlashList );

			AuthenticationUtil.setRunAsUser( authenticatedUserName );

			JSONObject response = new JSONObject();
			response.put( "cancelled" , cancelled );
			String parsedMessage = message.get( "success.text" , null );
			model.put( "response", response.toJSONString() );
			model.put( "success", ( parsedMessage != null ) ? parsedMessage : "" );
		}
		catch( Exception e ) {
			if( !statusThrown ) makeStatus.throwStatus( e.getMessage() , status , Status.STATUS_INTERNAL_SERVER_ERROR );
			return null;
		}
		return model;
	}

	private List<NodeRef> parseNewsFlashParameter( WebScriptRequest req , Status status , MakeMessage message ) throws Exception{
		String parameter = req.getParameter( QUERY_NEWS_FLASH_KEY );
		if( parameter == null ){
			String errorMessage = message.get( "error.noNewsFlashParameterProvided" , null );
			makeStatus.throwStatus( errorMessage , status , Status.STATUS_BAD_REQUEST );
			statusThrown = true;
			throw new Exception( errorMessage );
		}
		return NodeRef.getNodeRefs( parameter );
	}

	private String getWorkflowInstanceId( NodeRef newsFlash , Boolean mainProcessInstance ){
		String workflowInstanceId = null;
		try{
			workflowInstanceId = (String) nodeService.getProperty( newsFlash , ( ( mainProcessInstance == null || !mainProcessInstance.booleanValue() ) ? WorkflowModel.PROP_WORKFLOW_INSTANCE_ID : WorkflowModel.PROP_WORKFLOW_DESCRIPTION ) );
		}
		catch ( Exception e ) {
			String errorMessage = "Error occured whilst trying to get workflow instance id. " + e.getMessage();
			logger.error( errorMessage );
			throw new Error( errorMessage );
		}
		return workflowInstanceId;
	}

	private void setDateToNull( List<NodeRef> newsFlashList ){
		try{
			for( NodeRef newsFlash : newsFlashList ) {
				nodeService.setProperty( newsFlash , NEWS_FLASH_DATE_QNAME , null );
			}
		}
		catch ( Exception e ) {
			String errorMessage = "Error occured whilst trying to set news flash node date to NULL. " + e.getMessage();
			logger.error( errorMessage );
			throw new Error( errorMessage );
		}
	}

	private List<String> cancelWorkflow( List<NodeRef> newsFlashList ){
		List<String> cancelled = new ArrayList<String>();
		try{
			for( NodeRef newsFlash : newsFlashList ) {
				String workflowInstanceId = getWorkflowInstanceId( newsFlash , null );
				if( workflowInstanceId != null ){
					WorkflowInstance workflowInstance = workflowService.getWorkflowById( ACTIVITI_PREFIX + workflowInstanceId );
					logger.debug( "workflowInstance: " + workflowInstance );
					if( workflowInstance != null && workflowInstance.isActive() ){
						logger.debug( "Cancelling workflowInstance: " + workflowInstance.getId() );
						workflowService.cancelWorkflow( ACTIVITI_PREFIX + workflowInstanceId );
					}
					nodeService.setProperty( newsFlash , WorkflowModel.PROP_WORKFLOW_INSTANCE_ID , null );
	
					String mainWorkflowInstanceId = getWorkflowInstanceId( newsFlash , new Boolean( true ) );
					if( mainWorkflowInstanceId != null ) {
						WorkflowInstance parentWorkflowInstance = workflowService.getWorkflowById( ACTIVITI_PREFIX + mainWorkflowInstanceId );
						logger.debug( "parentWorkflowInstance: " + parentWorkflowInstance );
						if( parentWorkflowInstance != null && parentWorkflowInstance.isActive() ) {
							logger.debug( "Cancelling parentWorkflowInstance: " + parentWorkflowInstance.getId() );
							workflowService.cancelWorkflow( ACTIVITI_PREFIX + mainWorkflowInstanceId );
						}
					}
				}
				cancelled.add( workflowInstanceId );
			}
		}
		catch ( Exception e ) {
			String errorMessage = "Error occured whilst canceling workflow instance(s). " + e.getMessage();
			logger.error( errorMessage );
			throw new Error( errorMessage );
		}
		return cancelled;
	}
}
