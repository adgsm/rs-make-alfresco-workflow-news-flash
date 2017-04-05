package rs.make.alfresco.workflow.activiti.delegate.newsflash;

import java.io.Serializable;

import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.context.Context;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.workflow.WorkflowModel;
import org.alfresco.repo.workflow.activiti.ActivitiConstants;
import org.alfresco.repo.workflow.activiti.ActivitiScriptNode;
import org.alfresco.repo.workflow.activiti.BaseJavaDelegate;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.workflow.WorkflowInstance;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.apache.log4j.Logger;

import rs.make.alfresco.common.workflow.MakeWorkflowVars;

public class NewsFlashStart extends BaseJavaDelegate implements Serializable{
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger( NewsFlashStart.class );

	private static String ACTIVITI_PREFIX = "activiti$";
	private final String NEWS_FLASH_VARIABLE_PREFFIX = "makenfwf_";

	@Override
	public void execute( DelegateExecution execution ) throws Exception {
		logger.debug( "News flash, start." );
		String authenticatedUserName = AuthenticationUtil.getFullyAuthenticatedUser();

		AuthenticationUtil.setRunAsUserSystem();
		resetWorkflow( execution );

		AuthenticationUtil.setRunAsUser( authenticatedUserName );
}

	private void resetWorkflow( DelegateExecution execution ){
		boolean resetTasks = getResetTask( execution );
		if( !resetTasks ) return;

		NodeService nodeService = getNodeService();
		WorkflowService workflowService = getWorkflowService();

		NodeRef newsFlash = getNewsFlash( execution );
		try{
			String workflowInstanceId = getWorkflowInstanceId( newsFlash , execution , null );
			if( workflowInstanceId != null ){
				WorkflowInstance workflowInstance = workflowService.getWorkflowById( ACTIVITI_PREFIX + workflowInstanceId );
				logger.debug( "workflowInstance: " + workflowInstance );
				if( workflowInstance != null && workflowInstance.isActive() ){
					logger.debug( "Cancelling workflowInstance: " + workflowInstance.getId() );
					workflowService.cancelWorkflow( ACTIVITI_PREFIX + workflowInstanceId );
				}
				nodeService.setProperty( newsFlash , WorkflowModel.PROP_WORKFLOW_INSTANCE_ID , null );

				String mainWorkflowInstanceId = getWorkflowInstanceId( newsFlash , execution , new Boolean( true ) );
				if( mainWorkflowInstanceId != null ) {
					WorkflowInstance parentWorkflowInstance = workflowService.getWorkflowById( ACTIVITI_PREFIX + mainWorkflowInstanceId );
					logger.debug( "parentWorkflowInstance: " + parentWorkflowInstance );
					if( parentWorkflowInstance != null && parentWorkflowInstance.isActive() ) {
						logger.debug( "Cancelling parentWorkflowInstance: " + parentWorkflowInstance.getId() );
						workflowService.cancelWorkflow( ACTIVITI_PREFIX + mainWorkflowInstanceId );
					}
				}
			}
		}
		catch ( Exception e ) {
			String errorMessage = "Error occured whilst reseting workflow instance(s). " + e.getMessage();
			logger.error( errorMessage );
			throw new BpmnError( "newsFlashError" , errorMessage );
		}
	}

	private boolean getResetTask( DelegateExecution execution ){
		Boolean resetTasks = new Boolean( false );
		try{
			MakeWorkflowVars makeWorkflowVars = (MakeWorkflowVars) getBean( "makeWorkflowVars" );
			resetTasks = (Boolean) makeWorkflowVars.getExecutionLocalVar( execution , "resetTasks" , NEWS_FLASH_VARIABLE_PREFFIX );
		}
		catch ( Exception e ) {
			String errorMessage = "Error occured whilst trying to retrieve reset tasks. " + e.getMessage();
			logger.error( errorMessage );
			throw new BpmnError( "newsFlashError" , errorMessage );
		}
		return resetTasks.booleanValue();
	}

	private NodeRef getNewsFlash( DelegateExecution execution ){
		NodeRef newsFlash = null;
		try{
			MakeWorkflowVars makeWorkflowVars = (MakeWorkflowVars) getBean( "makeWorkflowVars" );
			ActivitiScriptNode newsFlashActivitiScriptNode = (ActivitiScriptNode) makeWorkflowVars.getExecutionLocalVar( execution , "flash" );
			newsFlash = newsFlashActivitiScriptNode.getNodeRef();
		}
		catch ( Exception e ) {
			String errorMessage = "Error occured whilst trying to retrieve news-flash. " + e.getMessage();
			logger.error( errorMessage );
			throw new BpmnError( "newsFlashError" , errorMessage );
		}
		return newsFlash;
	}

	private String getWorkflowInstanceId( NodeRef newsFlash , DelegateExecution execution , Boolean mainProcessInstance ){
		NodeService nodeService = getNodeService();
		String workflowInstanceId = null;
		try{
			workflowInstanceId = (String) nodeService.getProperty( newsFlash , ( ( mainProcessInstance == null || !mainProcessInstance.booleanValue() ) ? WorkflowModel.PROP_WORKFLOW_INSTANCE_ID : WorkflowModel.PROP_WORKFLOW_DESCRIPTION ) );
		}
		catch ( Exception e ) {
			String errorMessage = "Error occured whilst trying to check is news-flash sent in this workflow instance. " + e.getMessage();
			logger.error( errorMessage );
			throw new BpmnError( "newsFlashError" , errorMessage );
		}
		return workflowInstanceId;
	}

	private WorkflowService getWorkflowService(){
		WorkflowService workflowService = getServiceRegistryFromConfig().getWorkflowService();
		if( workflowService == null ){
			String errorMessage = "Workflow service could not be instanciated.";
			logger.error( errorMessage );
			throw new BpmnError( "newsFlashError" , errorMessage );
		}
		return workflowService;
	}

	private NodeService getNodeService(){
		NodeService nodeService = getServiceRegistryFromConfig().getNodeService();
		if( nodeService == null ){
			String errorMessage = "Node service could not be instanciated.";
			logger.error( errorMessage );
			throw new BpmnError( "newsFlashError" , errorMessage );
		}
		return nodeService;
	}

	private ServiceRegistry getServiceRegistryFromConfig(){
		return (ServiceRegistry) getBean( ActivitiConstants.SERVICE_REGISTRY_BEAN_KEY );
	}

	private Object getBean( String beanId ){
		Object bean = null;
		ProcessEngineConfigurationImpl config = Context.getProcessEngineConfiguration();
		bean = config.getBeans().get( beanId );
		if( bean == null ){
			String errorMessage = "Error occured whilst trying to invoke bean \"" + beanId + "\". ";
			logger.error( errorMessage );
			throw new BpmnError( "screeningConclusionError" , errorMessage );
		}
		logger.debug( "Bean \"" + beanId + "\" invoked." );
		return bean;
	}
}
