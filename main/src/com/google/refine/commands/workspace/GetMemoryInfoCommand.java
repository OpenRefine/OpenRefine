
package com.google.refine.commands.workspace;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.commands.Command;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class GetMemoryInfoCommand extends Command {

    private static final GlobalMemory systemMemory = new SystemInfo().getHardware().getMemory();

    public static class MemoryInfo {

        @JsonProperty("available_memory")
        protected Long availableMemory = systemMemory.getAvailable();

        @JsonProperty("total_memory")
        protected Long totalMemory = systemMemory.getTotal();

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
