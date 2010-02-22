package com.metaweb.gridworks.commands.recon;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import com.metaweb.gridworks.commands.EngineDependentCommand;
import com.metaweb.gridworks.model.AbstractOperation;
import com.metaweb.gridworks.model.Recon;
import com.metaweb.gridworks.model.ReconCandidate;
import com.metaweb.gridworks.model.Recon.Judgment;
import com.metaweb.gridworks.operations.ReconJudgeSimilarCellsOperation;

public class ReconJudgeSimilarCellsCommand extends EngineDependentCommand {

	@Override
	protected AbstractOperation createOperation(
			HttpServletRequest request, JSONObject engineConfig) throws Exception {
		
		String columnName = request.getParameter("columnName");
		String similarValue = request.getParameter("similarValue");
		Judgment judgment = Recon.stringToJudgment(request.getParameter("judgment"));
		
		ReconCandidate match = null;
		String topicID = request.getParameter("topicID");
		if (topicID != null) {
			String scoreString = request.getParameter("score");
			
			match = new ReconCandidate(
				topicID,
				request.getParameter("topicGUID"),
				request.getParameter("topicName"),
				request.getParameter("types").split(","),
				scoreString != null ? Double.parseDouble(scoreString) : 100
			);
		}
		
		String shareNewTopics = request.getParameter("shareNewTopics");
		
		return new ReconJudgeSimilarCellsOperation(
			engineConfig, 
			columnName,
			similarValue,
			judgment,
			match,
			"true".equals(shareNewTopics)
		);
	}
}
