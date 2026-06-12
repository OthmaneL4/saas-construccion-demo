package com.lsototalbouw.tool;

import com.lsototalbouw.common.enums.ToolStatus;
import java.time.LocalDate;
import java.util.List;

public class ToolInventorySummary {

    private final int totalTools;
    private final int availableTools;
    private final int inUseTools;
    private final int maintenanceDueTools;
    private final int unavailableTools;

    private ToolInventorySummary(int totalTools, int availableTools, int inUseTools,
                                 int maintenanceDueTools, int unavailableTools) {
        this.totalTools = totalTools;
        this.availableTools = availableTools;
        this.inUseTools = inUseTools;
        this.maintenanceDueTools = maintenanceDueTools;
        this.unavailableTools = unavailableTools;
    }

    public static ToolInventorySummary from(List<ToolItem> tools, LocalDate today) {
        int availableTools = 0;
        int inUseTools = 0;
        int maintenanceDueTools = 0;
        int unavailableTools = 0;

        for (ToolItem tool : tools) {
            if (tool.getStatus() == ToolStatus.AVAILABLE) {
                availableTools++;
            }
            if (tool.getStatus() == ToolStatus.IN_USE) {
                inUseTools++;
            }
            if (tool.getNextMaintenanceDate() != null && !tool.getNextMaintenanceDate().isAfter(today)) {
                maintenanceDueTools++;
            }
            if (tool.getStatus() == ToolStatus.LOST || tool.getStatus() == ToolStatus.RETIRED) {
                unavailableTools++;
            }
        }

        return new ToolInventorySummary(tools.size(), availableTools, inUseTools,
                maintenanceDueTools, unavailableTools);
    }

    public int getTotalTools() {
        return totalTools;
    }

    public int getAvailableTools() {
        return availableTools;
    }

    public int getInUseTools() {
        return inUseTools;
    }

    public int getMaintenanceDueTools() {
        return maintenanceDueTools;
    }

    public int getUnavailableTools() {
        return unavailableTools;
    }
}
