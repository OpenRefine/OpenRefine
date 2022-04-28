package com.google.refine.commands.workspace;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.commands.Command;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

public class GetSystemInfoCommand extends Command {
    private static final SystemInfo systemInfo = new SystemInfo();
    private static final GlobalMemory systemMemory = systemInfo.getHardware().getMemory();


    public static class SysInfo {
        @JsonProperty("os_version")
        protected String os = systemInfo.getOperatingSystem().toString();

        @JsonProperty("available_memory")
        protected Long availableMemory = systemMemory.getAvailable();

        @JsonProperty("total_memory")
        protected Long totalMemory = systemMemory.getTotal();

        @JsonProperty("date")
        protected String date = new Date().toString();

        @JsonProperty("hostname")
        protected String hostname;

        public SysInfo(String hostname) {
            this.hostname = hostname;
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        respondJSON(response, new SysInfo(request.getServerName()));
    }
}
