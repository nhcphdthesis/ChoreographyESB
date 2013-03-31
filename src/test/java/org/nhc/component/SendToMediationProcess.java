package org.nhc.component;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.runtime.Execution;

public class SendToMediationProcess {
	public String send(String s){
		String result = "";
		String id = s.substring(10);//length of "/receiveB/"
		result = "receiving id:"+id+" in SendToMediationProcess";
		System.out.println(result);
		ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
		System.out.println(processEngine.toString());
//		RepositoryService repositoryService = processEngine.getRepositoryService();
		RuntimeService runtimeService = processEngine.getRuntimeService();
		Execution execution = processEngine.getRuntimeService().createExecutionQuery()
		  .processInstanceId(id)
		  .activityId("sendToB")
		  .singleResult();
		
		if(execution == null){
			System.out.println("sendToB task is null");
		}else{
			System.out.println("sendToB task: "+execution.getId());
		}
		
		execution = processEngine.getRuntimeService().createExecutionQuery()
		  .processInstanceId(id)
		  .activityId("reveiveFromB")
		  .singleResult();
//		
		if(execution == null){
			System.out.println("reveiveFromB task is null");
		}else
		{
			System.out.println("reveiveFromB execution ID: "+execution.getId());
			result += "execution: " + execution.toString();
			runtimeService.setVariable(execution.getId(), "input", "modify: "+s);
			runtimeService.signal(id);
		}
		return result;
	}
}
