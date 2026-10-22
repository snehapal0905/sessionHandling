package com.spendly.spendly.dto;

import com.spendly.spendly.domain.Category;
import com.spendly.spendly.domain.CategoryKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryForm {

    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 60)
    private String name;

    @NotNull
    private CategoryKind kind = CategoryKind.EXPENSE;

    @NotBlank
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Use a colour like #b5562a")
    private String color = "#b5562a";

    private boolean essential;

    private boolean active = true;

    public boolean isNew() {
        return id == null;
    }

    public static CategoryForm of(Category c) {
        CategoryForm f = new CategoryForm();
        f.id = c.getId();
        f.name = c.getName();
        f.kind = c.getKind();
        f.color = c.getColor();
        f.essential = c.isEssential();
        f.active = c.isActive();
        return f;
    }
}
