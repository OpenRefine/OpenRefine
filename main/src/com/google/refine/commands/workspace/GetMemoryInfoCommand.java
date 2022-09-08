
package com.google.refine.commands.workspace;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.commands.Command;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class GetMemoryInfoCommand extends Command {

    public static class MemoryInfo {

        @JsonProperty("available_memory")
        protected Long availableMemory = Runtime.getRuntime().freeMemory();

        @JsonProperty("total_memory")
        protected Long totalMemory = Runtime.getRuntime().totalMemory();

    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        respondJSON(response, new MemoryInfo());
    }

    @Override
    public boolean logRequests() {
        return false;
    }
}
