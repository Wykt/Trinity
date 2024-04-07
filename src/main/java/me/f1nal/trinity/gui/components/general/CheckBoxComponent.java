package me.f1nal.trinity.gui.components.general;

import imgui.ImGui;
import me.f1nal.trinity.util.GuiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CheckBoxComponent {
    private final String name, description;
    private boolean value;

    public CheckBoxComponent(final @NotNull String name, final @Nullable String description, final boolean value) {
        this.name = name;
        this.description = description;
        this.value = value;
    }

    public void draw() {
        if(ImGui.checkbox(name, value)) {
            value = !value;
        }

        if(description != null) {
            GuiUtil.informationTooltip(description);
        }
    }

    public boolean getValue() {
        return value;
    }
}
