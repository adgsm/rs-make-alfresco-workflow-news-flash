package rs.make.alfresco.workflow.activiti.delegate.newsflash;

import java.io.Serializable;

import org.activiti.engine.delegate.DelegateExecution;
import org.alfresco.repo.workflow.activiti.BaseJavaDelegate;
import org.apache.log4j.Logger;

public class NewsFlashEnd extends BaseJavaDelegate implements Serializable{
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger( NewsFlashEnd.class );

	@Override
	public void execute( DelegateExecution execution ) throws Exception {
		logger.debug( "News flash, end." );
	}

}
