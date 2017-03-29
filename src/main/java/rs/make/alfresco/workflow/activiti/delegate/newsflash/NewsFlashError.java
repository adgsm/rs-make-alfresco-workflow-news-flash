package rs.make.alfresco.workflow.activiti.delegate.newsflash;

import org.activiti.engine.delegate.DelegateExecution;
import org.alfresco.repo.workflow.activiti.BaseJavaDelegate;
import org.apache.log4j.Logger;

public class NewsFlashError extends BaseJavaDelegate{
	private static Logger logger = Logger.getLogger( NewsFlashError.class );

	@Override
	public void execute( DelegateExecution execution ) throws Exception {
		logger.debug( "HANDLING BPMN ERROR" );
	}

}
