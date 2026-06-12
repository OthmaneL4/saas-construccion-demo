package com.lsototalbouw.tool;

import com.lsototalbouw.company.CompanyContextService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ToolService {

    private final ToolRepository tools;
    private final CompanyContextService companyContext;

    public ToolService(ToolRepository tools, CompanyContextService companyContext) {
        this.tools = tools;
        this.companyContext = companyContext;
    }

    @Transactional
    public ToolItem create(ToolForm form) {
        ToolItem tool = new ToolItem(
                companyContext.currentCompany(),
                form.getName().trim(),
                clean(form.getSerialNumber()),
                form.getStatus(),
                form.getNextMaintenanceDate()
        );
        return tools.save(tool);
    }

    @Transactional(readOnly = true)
    public ToolItem getCurrentCompanyTool(Long id) {
        return tools.findByCompanyAccountIdAndIdAndActiveTrue(companyContext.currentCompanyId(), id)
                .orElseThrow(() -> new IllegalArgumentException("Herramienta no encontrada"));
    }

    @Transactional
    public ToolItem update(Long id, ToolForm form) {
        ToolItem tool = getCurrentCompanyTool(id);
        tool.updateFrom(
                form.getName().trim(),
                clean(form.getSerialNumber()),
                form.getStatus(),
                form.getNextMaintenanceDate()
        );
        return tool;
    }

    @Transactional
    public void archive(Long id) {
        ToolItem tool = getCurrentCompanyTool(id);
        tool.setActive(false);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
