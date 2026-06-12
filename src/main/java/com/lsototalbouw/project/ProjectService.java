package com.lsototalbouw.project;

import com.lsototalbouw.company.CompanyContextService;
import com.lsototalbouw.customer.Customer;
import com.lsototalbouw.customer.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projects;
    private final CustomerRepository customers;
    private final CompanyContextService companyContext;

    public ProjectService(ProjectRepository projects, CustomerRepository customers, CompanyContextService companyContext) {
        this.projects = projects;
        this.customers = customers;
        this.companyContext = companyContext;
    }

    @Transactional
    public Project create(ProjectForm form) {
        Long companyId = companyContext.currentCompanyId();
        Customer customer = customers.findByCompanyAccountIdAndIdAndActiveTrue(companyId, form.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        Project project = new Project(
                customer.getCompanyAccount(),
                customer,
                form.getName().trim(),
                clean(form.getWorkAddress()),
                form.getStartDate(),
                form.getStatus(),
                form.getBudget()
        );
        return projects.save(project);
    }

    @Transactional(readOnly = true)
    public Project getCurrentCompanyProject(Long id) {
        return projects.findByCompanyAccountIdAndIdAndActiveTrue(companyContext.currentCompanyId(), id)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
    }

    @Transactional
    public Project update(Long id, ProjectForm form) {
        Long companyId = companyContext.currentCompanyId();
        Project project = projects.findByCompanyAccountIdAndIdAndActiveTrue(companyId, id)
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado"));
        Customer customer = customers.findByCompanyAccountIdAndIdAndActiveTrue(companyId, form.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        project.updateFrom(
                customer,
                form.getName().trim(),
                clean(form.getWorkAddress()),
                form.getStartDate(),
                form.getStatus(),
                form.getBudget()
        );
        return project;
    }

    @Transactional
    public void archive(Long id) {
        Project project = getCurrentCompanyProject(id);
        project.setActive(false);
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
