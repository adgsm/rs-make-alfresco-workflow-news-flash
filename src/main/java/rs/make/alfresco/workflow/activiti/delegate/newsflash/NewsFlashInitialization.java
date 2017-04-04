package rs.make.alfresco.workflow.activiti.delegate.newsflash;

import java.io.Serializable;

import org.activiti.engine.IdentityService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.context.Context;
import org.alfresco.repo.workflow.activiti.BaseJavaDelegate;
import org.apache.log4j.Logger;

import rs.make.alfresco.common.workflow.MakeWorkflowVars;

public class NewsFlashInitialization extends BaseJavaDelegate implements Serializable {
	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger( NewsFlashInitialization.class );

	@Override
	public void execute( DelegateExecution execution ) throws Exception {
		logger.debug( "News flash, initialization." );
		setIdentity( execution );
	}

	private void setIdentity( DelegateExecution execution ){
		MakeWorkflowVars makeWorkflowVars = (MakeWorkflowVars) getBean( "makeWorkflowVars" );
		IdentityService identityService = execution.getEngineServices().getIdentityService();
		try {
			String initiator = (String) makeWorkflowVars.getExecutionLocalVar( execution , "initiatorUserName" );
			identityService.setAuthenticatedUserId( initiator );
			logger.debug( String.format( "Set initiator: \"%s\"." , initiator ) );
		}
		catch( Exception e ){
			logger.debug( e.getMessage() );
			identityService.setAuthenticatedUserId( null );
		}
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
