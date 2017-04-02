package rs.make.alfresco.workflow.activiti.delegate.newsflash;

import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.delegate.DelegateExecution;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.workflow.activiti.BaseJavaDelegate;
import org.apache.log4j.Logger;

import rs.make.alfresco.common.workflow.MakeWorkflowVars;

public class SetParametersForNewsFlash extends BaseJavaDelegate{
	private static Logger logger = Logger.getLogger( SetParametersForNewsFlash.class );

	protected MakeWorkflowVars makeWorkflowVars;
	public MakeWorkflowVars getMakeWorkflowVars() {
		return makeWorkflowVars;
	}
	public void setMakeWorkflowVars( MakeWorkflowVars makeWorkflowVars ) {
		this.makeWorkflowVars = makeWorkflowVars;
	}

	protected NewsFlashInitialization newsFlashInitialization;
	public NewsFlashInitialization getNewsFlashInitialization() {
		return newsFlashInitialization;
	}
	public void setNewsFlashInitialization( NewsFlashInitialization newsFlashInitialization ) {
		this.newsFlashInitialization = newsFlashInitialization;
	}

	protected NewsFlashStart newsFlashStart;
	public NewsFlashStart getNewsFlashStart() {
		return newsFlashStart;
	}
	public void setNewsFlashStart( NewsFlashStart newsFlashStart ) {
		this.newsFlashStart = newsFlashStart;
	}

	protected NewsFlashCheckPrerequisites newsFlashCheckPrerequisites;
	public NewsFlashCheckPrerequisites getNewsFlashCheckPrerequisites() {
		return newsFlashCheckPrerequisites;
	}
	public void setNewsFlashCheckPrerequisites( NewsFlashCheckPrerequisites newsFlashCheckPrerequisites ) {
		this.newsFlashCheckPrerequisites = newsFlashCheckPrerequisites;
	}

	protected NewsFlashSend newsFlashSend;
	public NewsFlashSend getNewsFlashSend() {
		return newsFlashSend;
	}
	public void setNewsFlashSend( NewsFlashSend newsFlashSend ) {
		this.newsFlashSend = newsFlashSend;
	}

	protected NewsFlashEnd newsFlashEnd;
	public NewsFlashEnd getNewsFlashEnd() {
		return newsFlashEnd;
	}
	public void setNewsFlashEnd( NewsFlashEnd newsFlashEnd ) {
		this.newsFlashEnd = newsFlashEnd;
	}

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		String authenticatedUserName = AuthenticationUtil.getFullyAuthenticatedUser();
		logger.debug( String.format( "Authenticated user: %s, Process instance Id: %s, activity: %s." , authenticatedUserName , execution.getProcessInstanceId() , execution.getCurrentActivityName() ) );
		AuthenticationUtil.setRunAsUserSystem();

		Map<String,Object> workflowVars = new HashMap<String,Object>();
		workflowVars.put( "initiatorUserName" , authenticatedUserName );
		makeWorkflowVars.setExecutionLocalVars( execution , workflowVars );
		setExecutorsVars( execution );

		AuthenticationUtil.setRunAsUser( authenticatedUserName );
	}

	private void setExecutorsVars( DelegateExecution execution ){
		Map<String,Object> executorsVars = new HashMap<String,Object>();
		executorsVars.put( "commonEmailScriptTaskInitializationVar" , newsFlashInitialization );
		executorsVars.put( "commonEmailScriptTaskStartVar" , newsFlashStart );
		executorsVars.put( "commonEmailScriptTaskCheckPrerequisitesVar" , newsFlashCheckPrerequisites );
		executorsVars.put( "commonEmailScriptTaskSendVar" , newsFlashSend );
		executorsVars.put( "commonEmailScriptTaskEndVar" , newsFlashEnd );

		makeWorkflowVars.setExecutionLocalVars( execution , executorsVars );
	}

}
