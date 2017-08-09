package epam.autotests.page_objects.panels;

import com.epam.jdi.uitests.web.selenium.elements.common.CheckBox;
import com.epam.jdi.uitests.web.selenium.elements.common.TextField;
import epam.autotests.page_objects.general.Node;
import epam.autotests.page_objects.general.Panel;
import epam.autotests.page_objects.general.Tree;
import org.openqa.selenium.Keys;
import org.openqa.selenium.support.FindBy;

import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * Created by Vsevolod_Adrianov on 16-Jan-17.
 */
public class DatasetsPanel extends Panel {
    @FindBy(xpath = ".//md-input-container/input")
    private TextField searchTextField;

    @FindBy(xpath = ".//md-virtual-repeat-container")
    private Tree dsTree;

    @FindBy(xpath = ".//md-virtual-repeat-container//input")
    private CheckBox checkBox;

    public boolean isBoxSelected() {
        return dsTree.isSelected();
    }


    public void searchProject(String nameProj) {
        searchTextField.clickCenter();
        searchTextField.sendKeys(nameProj + Keys.ENTER);
        searchTextField.sendKeys(nameProj);
    }

    public void select(String DataName, boolean select) {
        Path p = Paths.get(DataName);
        String file = p.getFileName().toString();
        String pp = p.toString();
        System.out.println("path> <" + pp + ">");
        Node node = null;
        int i = 0;
        for (String retval : DataName.split("/")) {
            if (file.equalsIgnoreCase(retval) && node != null) {
                // Check file
                node = dsTree.getBy(retval);
                if (select)
                    node.Check();
                else
                    node.unCheck();
            } else {
                if (!retval.isEmpty()) {
                    if (i < 2) {
                        node = dsTree.getBy(retval);
                    } else {
                        node = dsTree.getBy(retval);
                    }
                    if (DataName.split("/").length==2)
                        node.Check();
                    else
                        node.OpenToggle();
                }
            }
            System.out.println("[" + i + "] " + retval);
            i++;
        }
    }
}
