
package com.google.refine.commands;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.operations.OperationRegistry;

public class GetAllOperationsCommand extends Command {

    private static class OperationEntry {

        @JsonProperty("name")
        public String name;

        @JsonProperty("icon")
        @JsonInclude(Include.NON_NULL)
        public String icon;
    }

    private static class Result {

        @JsonProperty("operations")
        public List<OperationEntry> operations;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Map<String, String> operationIcons = OperationRegistry.getOperationIcons();

        Result result = new Result();
        result.operations = OperationRegistry.getOperationNames().stream()
                .map(name -> {
                    OperationEntry entry = new OperationEntry();
                    entry.name = name;
                    entry.icon = operationIcons.get(name);
                    return entry;
                })
                .collect(Collectors.toList());

        respondJSON(response, result);
    }

}
