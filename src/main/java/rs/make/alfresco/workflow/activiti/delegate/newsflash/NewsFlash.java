package rs.make.alfresco.workflow.activiti.delegate.newsflash;

import java.io.Serializable;
import java.util.Collection;

import org.activiti.engine.IdentityService;
import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.alfresco.repo.workflow.activiti.BaseJavaDelegate;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;

import rs.make.alfresco.common.workflow.MakeWorkflowVars;

public class NewsFlash extends BaseJavaDelegate implements Serializable{
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger( NewsFlash.class );

	private final String NEWS_FLASH_VARIABLE_PREFFIX = "makenfwf_";
	private DelegateExecution EXECUTION = null;

	protected transient MakeWorkflowVars makeWorkflowVars;
	public MakeWorkflowVars getMakeWorkflowVars() {
		return makeWorkflowVars;
	}
	public void setMakeWorkflowVars( MakeWorkflowVars makeWorkflowVars ) {
		this.makeWorkflowVars = makeWorkflowVars;
	}

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		logger.debug( "News Flash workflow initiated." );
		EXECUTION = execution;
		setIdentity( execution );
		setStartTimer( execution );
		setEndTimer( execution );
	}

	@SuppressWarnings("unchecked")
	public Collection<NodeRef> subProcessInstances(){
		if( EXECUTION == null ){
			String errorMessage = "Workflow execution delegate could not be reached.";
			logger.error( errorMessage );
			throw new BpmnError( "newsFlashError" , errorMessage );
		}
		Collection<NodeRef> newsFlashCollection = (Collection<NodeRef>) makeWorkflowVars.getExecutionLocalVar( EXECUTION , "newsFlash" , NEWS_FLASH_VARIABLE_PREFFIX );

		logger.debug( String.format( "Number of items in a collection is: %d" , newsFlashCollection.size() ) );
		return newsFlashCollection;
	}

	private void setIdentity( DelegateExecution execution ){
		IdentityService identityService = execution.getEngineServices().getIdentityService();
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
		makeWorkflowVars.setExecutionLocalVar( execution , "newsFlashTimerStart" , "PT0S" );
	}

	private void setEndTimer( DelegateExecution execution ){
		makeWorkflowVars.setExecutionLocalVar( execution , "newsFlashTimerEnd" , "PT0S" );
	}

}
