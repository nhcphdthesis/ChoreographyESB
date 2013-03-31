package org.nhc.component;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.Execution;

public class SendToActiviti2 {
	public String send(String s){
		String result;
		String id = s;//.substring(10);//length of "/activiti/"
		result = "receiving id:"+id;
		String Aid = id.substring(5);
		id = id.substring(0, 3);
		System.out.println("Aid: "+Aid+", ID: " +id);
		System.out.println(result);
		ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
		System.out.println(processEngine.toString());
//		RepositoryService repositoryService = processEngine.getRepositoryService();
		RuntimeService runtimeService = processEngine.getRuntimeService();
//		sleep(5000);
		Execution execution = processEngine.getRuntimeService().createExecutionQuery()
		  .processInstanceId(Aid)
		  .activityId("receivetask1")
		  .singleResult();
		if(execution == null){
			System.out.println("receive task is null");
			return result;
		}
		result += "execution: " + execution.toString();
		System.out.println("execution ID: "+execution.getId());
		runtimeService.signal(execution.getId());
		return result;
	}
}
