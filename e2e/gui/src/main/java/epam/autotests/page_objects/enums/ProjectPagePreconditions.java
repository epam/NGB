package epam.autotests.page_objects.enums;

import com.epam.commons.linqinterfaces.JAction;
import com.epam.jdi.uitests.web.selenium.preconditions.WebPreconditions;

import java.util.function.Supplier;

import static epam.autotests.page_objects.enums.Views.*;
import static epam.autotests.page_objects.site.NGB_Site.*;

public enum ProjectPagePreconditions implements WebPreconditions {

    OPEN_DATASETS_PANEL(() -> projectPage.isPanelActive(DATASETS), () -> projectPage.openPanel(DATASETS)),
    OPEN_VARIANTS_PANEL(() -> projectPage.isPanelActive(VARIANTS), () -> projectPage.openPanel(VARIANTS)),
    //OPEN_FILTER_PANEL(() -> !projectPage.isPanelActive(VARIANTS), () -> projectPage.openFilter()),
    OPEN_SESSIONS_PANEL(() -> projectPage.isPanelActive(SESSIONS), () -> projectPage.openPanel(SESSIONS)),
    OPEN_BROWSER_PANEL(() -> projectPage.isPanelActive(BROWSER), () -> projectPage.openPanel(BROWSER)),
    CHROMOSOME_CHOSEN(ProjectPagePreconditions::isChromosomeChosen, () -> header.chooseChromosome("2")),
    CHROMOSOME_ISNT_CHOSEN(ProjectPagePreconditions::isntChromosomeChosen, () -> header.resetChSelection()),
    OPEN_SETTING(() -> settingWindow.isDisplayed(), () -> header.openSetting());


    public Supplier<Boolean> checkAction;
    public JAction moveToAction;


    ProjectPagePreconditions(Supplier<Boolean> checkAction, JAction moveToAction) {
        this.checkAction = checkAction;
        this.moveToAction = moveToAction;
    }

    @Override
    public Boolean checkAction() {
        return checkAction.get();
    }

    @Override
    public void moveToAction() {
        moveToAction.invoke();
    }

    private static boolean isChromosomeChosen() {
        return header.isChromosomeSelected();
    }

    private static boolean isntChromosomeChosen() {
        return header.isChromosomeSelected();
    }
}
