package rs.make.alfresco.workflow.newsflash;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.workflow.WorkflowDefinition;
import org.alfresco.service.cmr.workflow.WorkflowInstance;
import org.alfresco.service.cmr.workflow.WorkflowPath;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.namespace.NamespaceService;
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

public class Start extends DeclarativeWebScript{
	private static Logger logger = Logger.getLogger( Start.class );

	private final String WORKFLOW_DEFINITION_NAME = "activiti$newsFlash";
	private final String QUERY_RESET_KEY = "reset";
	private final String QUERY_NEWS_FLASH_KEY = "news-flash";

	private final Date WORKFLOW_DUE_DATE = new Date( ( new Date() ).getTime() + 365 * 24 * 60 * 60 * 1000 );
	private final int WORKFLOW_PRIORITY = 2;

	private final String NEWS_FLASH_NAMESPACE_URI = "http://www.make.rs/model/workflow/news-flash/1.0";
	private final QName LOCALE_QNAME = QName.createQName( NEWS_FLASH_NAMESPACE_URI , "locale" );
	private final QName INITIATOR_USER_NAME_QNAME = QName.createQName( NEWS_FLASH_NAMESPACE_URI , "initiator" );
	private final QName RESET_TASKS_QNAME = QName.createQName( NEWS_FLASH_NAMESPACE_URI , "resetTasks" );
	private final QName NEWS_FLASH_LIST_QNAME = QName.createQName( NEWS_FLASH_NAMESPACE_URI , "newsFlash" );
	private final String BPM_NAMESPACE_URI = NamespaceService.BPM_MODEL_1_0_URI;
	private final QName WORKFLOW_DESCRIPTION_QNAME = QName.createQName( BPM_NAMESPACE_URI , "workflowDescription" );
	private final QName WORKFLOW_DUEDATE_QNAME = QName.createQName( BPM_NAMESPACE_URI , "workflowDueDate" );
	private final QName WORKFLOW_PRIORITY_QNAME = QName.createQName( BPM_NAMESPACE_URI , "workflowPriority" );

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

			Locale locale = makeLocale.get( req );

			WorkflowDefinition workflowDefinition = workflowService.getDefinitionByName( WORKFLOW_DEFINITION_NAME );
			String workflowDefinitionId = workflowDefinition.getId();

			boolean resetTasks = parseResetTasksParameter( req );
			List<NodeRef> newsFlashList = parseNewsFlashParameter( req , status , message );

			Map<QName,Serializable> workflowParameters = getWorkflowParameters( locale , resetTasks , newsFlashList );
			WorkflowPath initialWorkflowPath = startWorkflowInstance( workflowDefinitionId , workflowParameters );
			WorkflowInstance workflowInstance = initialWorkflowPath.getInstance();
			String workflowInstanceId = workflowInstance.getId();
			logger.debug( String.format( "Started %s workflow instance with id %s", WORKFLOW_DEFINITION_NAME , workflowInstanceId ) );

			JSONObject response = new JSONObject();
			response.put( "workflowInstanceId" , workflowInstanceId );
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

	private boolean parseResetTasksParameter( WebScriptRequest req ){
		String reset = req.getParameter( QUERY_RESET_KEY );
		boolean resetTasks = reset != null && reset.toLowerCase().equals( "true" );
		logger.debug( String.format( "Reset tasks flag set to \"%s\"." , resetTasks ) );
		return resetTasks;
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

	private Map<QName,Serializable> getWorkflowParameters( Locale locale , boolean resetTasks , List<NodeRef> newsFlashList ) {
		String initiator = AuthenticationUtil.getFullyAuthenticatedUser();
		Map<QName,Serializable> parameters = new HashMap<QName,Serializable>();
		parameters.put( LOCALE_QNAME , locale );
		parameters.put( INITIATOR_USER_NAME_QNAME , initiator );
		parameters.put( RESET_TASKS_QNAME , resetTasks );
		parameters.put( NEWS_FLASH_LIST_QNAME , (Serializable) newsFlashList );
		parameters.put( WORKFLOW_DESCRIPTION_QNAME , WORKFLOW_DEFINITION_NAME );
		parameters.put( WORKFLOW_DUEDATE_QNAME , WORKFLOW_DUE_DATE );
		parameters.put( WORKFLOW_PRIORITY_QNAME , WORKFLOW_PRIORITY );
		return parameters;
	}

	private WorkflowPath startWorkflowInstance( String workflowDefinitionId , Map<QName,Serializable> workflowParameters ){
		return workflowService.startWorkflow( workflowDefinitionId , workflowParameters );
	}
}
