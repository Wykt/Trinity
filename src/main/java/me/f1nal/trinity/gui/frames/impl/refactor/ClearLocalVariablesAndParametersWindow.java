package me.f1nal.trinity.gui.frames.impl.refactor;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.events.EventRefreshDecompilerText;
import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.gui.frames.StaticWindow;
import me.f1nal.trinity.refactor.remover.ClearLocalVariablesAndParametersRemover;

public final class ClearLocalVariablesAndParametersWindow extends StaticWindow {
    public ClearLocalVariablesAndParametersWindow(final Trinity trinity) {
        super("Clear local variables & parameters", 360, 0, trinity);
        this.windowFlags |= ImGuiWindowFlags.NoResize;
    }

    private final ClearLocalVariablesAndParametersRemover remover = new ClearLocalVariablesAndParametersRemover();

    @Override
    protected void renderFrame() {
        remover.drawInputs();
        ImGui.separator();

        if (ImGui.button("Start Refactor")) {
            runRefactor(trinity.getExecution());
        }

        ImGui.sameLine();
        ImGui.progressBar(0.0f);
    }

    private void runRefactor(final Execution execution) {
        remover.runRefactor(execution);
    }
}
