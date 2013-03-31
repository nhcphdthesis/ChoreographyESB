package org.nhc.component;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.Execution;

public class SendToActiviti {
	public String send(String s){
		String result = "";
		String id = s.substring(10);//length of "/activiti/"
		result = "receiving id:"+id+" in SendToActiviti";
		System.out.println(result);
		ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
		System.out.println(processEngine.toString());
//		RepositoryService repositoryService = processEngine.getRepositoryService();
		RuntimeService runtimeService = processEngine.getRuntimeService();
		Execution execution = processEngine.getRuntimeService().createExecutionQuery()
		  .processInstanceId(id)
		  .activityId("servicetask1")
		  .singleResult();
		
		if(execution == null){
			System.out.println("service task is null");
		}else{
			System.out.println("service task: "+execution.getId());
		}
		
		execution = processEngine.getRuntimeService().createExecutionQuery()
		  .processInstanceId(id)
		  .activityId("receivetask1")
		  .singleResult();
//		
		if(execution == null){
			System.out.println("receive task is null");
		}else
		{
			System.out.println("execution ID: "+execution.getId());
			result += "execution: " + execution.toString();
			runtimeService.signal(id);
		}
		return result;
	}
}
