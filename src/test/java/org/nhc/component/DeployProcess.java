package org.nhc.component;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.repository.Deployment;


public class DeployProcess {
	public String deploy(String s){
		String result = "";
		ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
		System.out.println(processEngine.toString());
		RepositoryService repositoryService = processEngine.getRepositoryService();
		RuntimeService runtimeService = processEngine.getRuntimeService();
		Deployment deployment = repositoryService.createDeployment()
		  .addClasspathResource("MediationProcess.bpmn20.xml")
		  .deploy();
		result = "deployed MediationProcess.bpmn20: "+deployment.getId();
		return result;
	}
}
