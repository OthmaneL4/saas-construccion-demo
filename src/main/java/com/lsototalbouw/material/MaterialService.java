package com.lsototalbouw.material;

import com.lsototalbouw.company.CompanyContextService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaterialService {

    private final MaterialRepository materials;
    private final CompanyContextService companyContext;

    public MaterialService(MaterialRepository materials, CompanyContextService companyContext) {
        this.materials = materials;
        this.companyContext = companyContext;
    }

    @Transactional
    public MaterialItem create(MaterialForm form) {
        MaterialItem material = new MaterialItem(
                companyContext.currentCompany(),
                form.getName().trim(),
                form.getUnit().trim(),
                form.getStockQuantity(),
                form.getMinimumStock(),
                form.getUnitCost()
        );
        return materials.save(material);
    }

    @Transactional(readOnly = true)
    public MaterialItem getCurrentCompanyMaterial(Long id) {
        return materials.findByCompanyAccountIdAndIdAndActiveTrue(companyContext.currentCompanyId(), id)
                .orElseThrow(() -> new IllegalArgumentException("Material no encontrado"));
    }

    @Transactional
    public MaterialItem update(Long id, MaterialForm form) {
        MaterialItem material = getCurrentCompanyMaterial(id);
        material.updateFrom(
                form.getName().trim(),
                form.getUnit().trim(),
                form.getStockQuantity(),
                form.getMinimumStock(),
                form.getUnitCost()
        );
        return material;
    }

    @Transactional
    public void archive(Long id) {
        MaterialItem material = getCurrentCompanyMaterial(id);
        material.setActive(false);
    }
}
