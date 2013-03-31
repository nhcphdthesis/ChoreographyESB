package org.nhc.component;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;

public class CreateProcess {

	/**
	 * @param args
	 */
	public String work(String s){
		String result = "create: "+s;
		
		try {
			ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
			System.out.println(processEngine.toString());
//			RepositoryService repositoryService = processEngine.getRepositoryService();
			RuntimeService runtimeService = processEngine.getRuntimeService();
//			Deployment deployment = repositoryService.createDeployment()
//			  .addClasspathResource("test.bpmn20.xml")
//			  .deploy();
			ProcessInstance processInstance = processEngine.getRuntimeService().startProcessInstanceByKey("testIntegration");
			System.out.println("ID: "+processInstance.getId());
			result = "created process instance.\nID: "+processInstance.getId();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(result);
		return result;
	}

}
