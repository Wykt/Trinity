package me.f1nal.trinity.refactor.remover;

import me.f1nal.trinity.execution.Execution;
import me.f1nal.trinity.gui.components.general.CheckBoxComponent;

public final class ClearLocalVariablesAndParametersRemover {
    private final CheckBoxComponent clearLocalVariables = new CheckBoxComponent("Clear local variables", "Clear method local variables names", false);
    private final CheckBoxComponent clearParameters = new CheckBoxComponent("Clear parameters", "Clear method parameters names", false);

    public void drawInputs() {
        clearLocalVariables.draw();
        clearParameters.draw();
    }

    public void runRefactor(final Execution execution) {
        for (final var classInput : execution.getClassList()) {
            for (final var methodInput : classInput.getMethodList().values()) {
                final var mn = methodInput.getMethodNode();

                if(clearLocalVariables.getValue()) {
                    mn.localVariables = null;
                }

                if(clearParameters.getValue()) {
                    mn.parameters = null;
                }
            }
        }
    }
}
