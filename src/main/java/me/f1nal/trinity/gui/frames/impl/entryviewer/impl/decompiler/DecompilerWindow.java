package me.f1nal.trinity.gui.frames.impl.entryviewer.impl.decompiler;

import com.google.common.eventbus.Subscribe;
import imgui.ImColor;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiMouseButton;
import imgui.flag.ImGuiMouseCursor;
import me.f1nal.trinity.Main;
import me.f1nal.trinity.Trinity;
import me.f1nal.trinity.database.IDatabaseSavable;
import me.f1nal.trinity.database.object.DatabaseDecompiler;
import me.f1nal.trinity.decompiler.DecompiledClass;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.events.EventClassModified;
import me.f1nal.trinity.events.EventRefreshDecompilerText;
import me.f1nal.trinity.events.api.IEventListener;
import me.f1nal.trinity.execution.ClassInput;
import me.f1nal.trinity.execution.ClassTarget;
import me.f1nal.trinity.execution.Input;
import me.f1nal.trinity.execution.packages.other.ExtractArchiveEntryRunnable;
import me.f1nal.trinity.gui.components.SearchBar;
import me.f1nal.trinity.gui.components.filter.misc.ShowFilterOption;
import me.f1nal.trinity.gui.components.popup.PopupItemBuilder;
import me.f1nal.trinity.gui.components.popup.PopupMenuBar;
import me.f1nal.trinity.gui.frames.impl.classstructure.ClassStructure;
import me.f1nal.trinity.gui.frames.impl.classstructure.ClassStructureWindow;
import me.f1nal.trinity.gui.frames.impl.entryviewer.ArchiveEntryViewerWindow;
import me.f1nal.trinity.theme.CodeColorScheme;
import me.f1nal.trinity.util.SystemUtil;
import me.f1nal.trinity.util.TimedStopwatch;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class DecompilerWindow extends ArchiveEntryViewerWindow<ClassTarget> implements IEventListener, IDatabaseSavable<DatabaseDecompiler> {
    private ClassInput selectedClass;
    /**
     * Notifies the selected class must be refreshed.
     */
    private TimedStopwatch forceRefresh;
    /**
     * Text component that is currently hovered.
     */
    private DecompilerComponent hoveredComponent;
    private boolean resetLines, shouldSearch;
    private final SearchBar searchBar = new SearchBar();
    private static final ShowFilterOption showFilter = new ShowFilterOption("decompilerWindow");

    public DecompilerWindow(ClassTarget classTarget, Trinity trinity) {
        super(trinity, classTarget);
        trinity.getEventManager().registerListener(this);
        this.setDecompileTarget(Objects.requireNonNull(classTarget.getInput()));
        this.setMenuBar(new PopupMenuBar(PopupItemBuilder.create().
                menu("File", file -> {
                    file
                            .menuItem("Refresh", () -> this.forceRefresh = new TimedStopwatch(0L))
                            .separator()
                            .menuItem("Copy", () -> SystemUtil.copyToClipboard(this.getText()))
                            .menuItem("Save", () -> new ExtractArchiveEntryRunnable(classTarget.getDisplaySimpleName() + ".java", this.getText().getBytes()).run())
                    ;
                }).
                menu("Find", showFilter::menuItem)));
    }

    @Override
    public String getTitle() {
        return getArchiveEntry().getDisplayOrRealName() + ".java";
    }

    public void setDecompileTarget(ClassInput classInput) {
        if (classInput == selectedClass) {
            return;
        }
        selectedClass = classInput;
        if (classInput != null && !trinity.getDatabase().isLoading()) this.save();
        if (this.isFocusGained()) this.updateClassStructure();
    }

    @Override
    protected void onFocusGain() {
        this.updateClassStructure();
    }

    private void updateClassStructure() {
        if (this.selectedClass != null) {
            Main.getDisplayManager().addStaticWindow(ClassStructureWindow.class).setClassStructure(new ClassStructure(this.selectedClass));
        }
    }

    public ClassInput getSelectedClass() {
        return selectedClass;
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    protected void renderFrame() {
        if (selectedClass != null) {
            this.drawDecompileTab();
        } else {
            ImGui.text("No class selected");
        }
    }

    @Subscribe
    public void onClassModified(EventClassModified event) {
        if (event.getClassInput() == this.selectedClass) {
            if (this.forceRefresh == null || event.getPriority().getTime() < this.forceRefresh.getPassed()) {
                this.forceRefresh = new TimedStopwatch(event.getPriority().getTime());
            }
        }
    }

    @Subscribe
    public void onRefreshDecompilerText(EventRefreshDecompilerText event) {
        DecompiledClass decompiledClass = getDecompiledClass();
        if (event.getPredicate().test(decompiledClass)) {
            this.resetLines = true;
        }
    }

    private ClassInput decompilingInput;

    private void drawDecompileTab() {
        this.runControls();

        if (showFilter.getShowFilter().getState()) {
            if (showFilter.isSetFocus()) ImGui.setKeyboardFocusHere();

            if(this.searchBar.draw()) {
                this.shouldSearch = true;
            }
        }

        ImGui.beginChild("DecompilerWindowChild");
        showFilter.runControls(); // needed so even if user focuses the decompiler window, we still can use show filter shortcut

        DecompiledClass decompiledClass = this.getDecompiledClass();
        if (decompiledClass == null) {
            ImGui.textUnformatted("...");
        } else {
            if (this.resetLines) {
                decompiledClass.resetLines();
                this.resetLines = false;
            }
            this.drawDecompiledOutput(decompiledClass);
        }

        ImGui.endChild();
    }

    public void requestResetLines() {
        this.resetLines = true;
    }

    private void runControls() {
        showFilter.runControls();

        boolean refreshDecompiler = (this.forceRefresh != null && this.forceRefresh.hasPassed());
        if (refreshDecompiler) {
            this.forceRefresh = null;
            try {
                trinity.getDecompiler().decompile(selectedClass, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Main.getDisplayManager().getArchiveEntryViewerFacade().drawHistoryButtons(this.selectedClass.getClassTarget());

        if (trinity.getDecompiler().isDecompileFailed(selectedClass)) {
            ImGui.textColored(ImColor.rgb(245, 80, 80), "Decompilation failed");
        }

        if (decompilingInput != selectedClass) {
            if (trinity.getDecompiler().getFromCache(selectedClass) == null) {
                try {
                    trinity.getDecompiler().decompile(selectedClass, decompiledClass -> {
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            decompilingInput = selectedClass;
        }
    }

    private String getText() {
        DecompiledClass decompiled = getDecompiledClass();
        StringBuilder output = new StringBuilder();
        return output.toString();
    }

    private boolean isSearching() {
        return showFilter.getShowFilter().getState() && !searchBar.getText().isEmpty();
    }

    private DecompiledClass getDecompiledClass() {
        return trinity.getDecompiler().getFromCache(selectedClass);
    }

    private void drawDecompiledOutput(DecompiledClass decompiledClass) {
        this.hoveredComponent = null;

        ImVec2 textSize = ImGui.calcTextSize(String.valueOf(decompiledClass.getLines().size() + 1));
        float lineNumberSpacing = 3.F + textSize.x;
        float cursorPosX = ImGui.getCursorPosX();

        for (DecompilerLine line : decompiledClass.getLines()) {
            float mousePosY = ImGui.getMousePosY() + ImGui.getScrollY() - ImGui.getWindowPosY();
            float cursorPosY = ImGui.getCursorPosY();
            boolean hovered = mousePosY >= cursorPosY && mousePosY < cursorPosY + textSize.y + ImGui.getStyle().getItemSpacingY();

            ImGui.textColored(CodeColorScheme.LINE_NUMBER, String.valueOf(line.getLineNumber()));
            ImGui.sameLine();
            ImGui.setCursorPosX(cursorPosX + lineNumberSpacing);

            for (DecompilerLineText text : line.getComponents()) {
                text.render();
                if (this.hoveredComponent == null && ImGui.isItemHovered()) {
                    this.hoveredComponent = text.getComponent();
                }
                ImGui.sameLine(0.F, 0.F);
            }

            ImGui.newLine();
        }

        if (this.hoveredComponent != null) {
            List<ColoredString> tooltip = this.hoveredComponent.createTooltip();

            if (tooltip != null) {
                ImGui.beginTooltip();

                ColoredString.drawText(tooltip);

                ImGui.endTooltip();
            }

            if (this.hoveredComponent.getRenameHandler() != null && this.hoveredComponent.getRenameState() == null) {
                if (ImGui.isMouseClicked(0)) {
                    this.hoveredComponent.beginRenaming();
                }

                ImGui.setMouseCursor(ImGuiMouseCursor.TextInput);
            }

            if (ImGui.isMouseClicked(ImGuiMouseButton.Right)) {
                PopupItemBuilder popup = this.hoveredComponent.createPopup();

                if (!popup.isEmpty()) {
                    Main.getDisplayManager().showPopup(popup);
                }
            }
        }
    }

    private String formatClassName() {
        return null;
    }

    public void setDecompileTarget(Input input) {
//        this.autoscrollTo = input;
        this.setDecompileTarget(input.getOwningClass());
    }

    @Override
    public DatabaseDecompiler createDatabaseObject() {
        return new DatabaseDecompiler(this.selectedClass.getFullName());
    }
}
