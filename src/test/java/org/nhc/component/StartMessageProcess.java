package org.nhc.component;

import java.util.HashMap;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;

public class StartMessageProcess {

	/**
	 * @param args
	 */
	public String work(String s){
		String result = "received id: "+s;
		System.out.println(result);
		try {
			ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
			System.out.println(processEngine.toString());
//			RepositoryService repositoryService = processEngine.getRepositoryService();
			RuntimeService runtimeService = processEngine.getRuntimeService();
//			Deployment deployment = repositoryService.createDeployment()
//			  .addClasspathResource("test.bpmn20.xml")
//			  .deploy();
			HashMap<String, Object> v = new HashMap<String, Object>();
	         v.put("idA", s);
			ProcessInstance processInstance = processEngine.getRuntimeService().startProcessInstanceByKey("testB",v);
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
