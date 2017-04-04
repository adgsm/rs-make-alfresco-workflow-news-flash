package rs.make.alfresco.workflow.activiti.delegate.newsflash;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.activiti.engine.IdentityService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.context.Context;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.workflow.activiti.ActivitiScriptNode;
import org.alfresco.repo.workflow.activiti.BaseJavaDelegate;
import org.apache.log4j.Logger;

import rs.make.alfresco.common.workflow.MakeWorkflowVars;

public class NewsFlash extends BaseJavaDelegate implements Serializable{
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger( NewsFlash.class );

	private final String NEWS_FLASH_VARIABLE_PREFFIX = "makenfwf_";
	private DelegateExecution EXECUTION = null;

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		logger.debug( "News Flash workflow initiated." );
		String authenticatedUserName = AuthenticationUtil.getFullyAuthenticatedUser();
		AuthenticationUtil.setRunAsUserSystem();

		EXECUTION = execution;

		setIdentity( execution );
		setStartTimer( execution );
		setEndTimer( execution );

		AuthenticationUtil.setRunAsUser( authenticatedUserName );
	}

	@SuppressWarnings("unchecked")
	public Collection<ActivitiScriptNode> subProcessInstances(){
		if( EXECUTION == null ){
			String errorMessage = "Workflow execution delegate could not be reached.";
			logger.error( errorMessage );
			throw new BpmnError( "newsFlashError" , errorMessage );
		}
		Collection<ActivitiScriptNode> newsFlashCollection = new ArrayList<ActivitiScriptNode>();
		MakeWorkflowVars makeWorkflowVars = (MakeWorkflowVars) getBean( "makeWorkflowVars" );
		try{
			newsFlashCollection = (Collection<ActivitiScriptNode>) makeWorkflowVars.getExecutionLocalVar( EXECUTION , "item" , NEWS_FLASH_VARIABLE_PREFFIX );
			logger.debug( String.format( "Number of items in a collection is: %d" , newsFlashCollection.size() ) );
		}
		catch ( Exception e ) {
			String errorMessage = String.format( "Error occured whilst calculating number of items in collection. %s" , e.getMessage() );
			logger.error( errorMessage );
			throw new BpmnError( "newsFlashError" , errorMessage );
		}
			return newsFlashCollection;
	}

	private void setIdentity( DelegateExecution execution ){
		IdentityService identityService = execution.getEngineServices().getIdentityService();
		MakeWorkflowVars makeWorkflowVars = (MakeWorkflowVars) getBean( "makeWorkflowVars" );
		try {
			String initiator = (String) makeWorkflowVars.getExecutionLocalVar( execution , "initiator" , NEWS_FLASH_VARIABLE_PREFFIX );
			identityService.setAuthenticatedUserId( initiator );
			logger.debug( String.format( "Set initiator: \"%s\"." , initiator ) );
		}
		catch( Exception e ){
			logger.debug( e.getMessage() );
			identityService.setAuthenticatedUserId( null );
		}
	}

	private void setStartTimer( DelegateExecution execution ){
		MakeWorkflowVars makeWorkflowVars = (MakeWorkflowVars) getBean( "makeWorkflowVars" );
		makeWorkflowVars.setExecutionLocalVar( execution , "newsFlashTimerStart" , "PT0S" );
	}

	private void setEndTimer( DelegateExecution execution ){
		MakeWorkflowVars makeWorkflowVars = (MakeWorkflowVars) getBean( "makeWorkflowVars" );
		makeWorkflowVars.setExecutionLocalVar( execution , "newsFlashTimerEnd" , "PT0S" );
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
