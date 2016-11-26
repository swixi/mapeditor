package objectbrowser;

import main.Editor;
import java.awt.event.*;
import javax.swing.*;
import java.awt.image.BufferedImage;

public class ObjectBrowser extends JComponent
{
    JTabbedPane objectBrowser;
    Editor e;
    
    public ObjectBrowser(Editor ed, int numPanels)
    {
        e = ed;
        init();
        JComponent panel;

        for(int i = 0; i < numPanels; i++)
        {
            panel = createPanel("Panel # " + (i+1));
            objectBrowser.addTab("Tab " + (i+1), panel);
            objectBrowser.setMnemonicAt(i, KeyEvent.VK_1);
        }
    }

    public ObjectBrowser(Editor ed)
    {
        e = ed;
        init(); 
    }

    private void init()
    {
        objectBrowser = new JTabbedPane();
        //objectBrowser.setForeground(Color.BLACK);
        //objectBrowser.setBackground(Color.LIGHT_GRAY);
    }

    private JComponent createPanel(String label)
    {
        //JPanel panel = new JPanel(false);
        ObjectBrowserComponent panel = new ObjectBrowserComponent(this, objectBrowser.getTabCount());
        //panel.setLayout(new GridLayout(1, 1));

        //The panel number is numTabs + 1 because the tab is actually created after this panel is returned to addTab()
        panel.setName("Panel " + (objectBrowser.getTabCount() + 1));
        
        //JScrollPane scroller = new JScrollPane(panel);
        //scroller.setPreferredSize(new Dimension(200,200));
        //add(panel, BorderLayout.PAGE_START);
        //add(scroller, BorderLayout.CENTER);

        return panel;
    }

    //Used by Editor to name the labels once it loads each category from the FTP
    public void setPanelLabel(int panelNumber, String label)
    {
        objectBrowser.setTitleAt(panelNumber, label);
    }

    //Maybe use this method from the Editor to dynamically add images to the browser as it loads them from ***REMOVED***
    public void addImage(int panelNumber, BufferedImage image, String imageName)
    {
        //objectBrowser.getComponent(0).repaint();
        //e.print(objectBrowser.getComponent(0).getName());
        ObjectBrowserImage obImage = new ObjectBrowserImage(image, imageName, e);
        ((ObjectBrowserComponent)objectBrowser.getComponent(panelNumber)).addImage(obImage);
    }

    //adds a tab at the specified location
    public void addTab(int position, String label)
    {
        objectBrowser.insertTab(label, null, createPanel("New panel at position " + position), label, position);
    }

    //adds a tab to the end
    public void addTab(String label)
    {
        objectBrowser.addTab(label, createPanel("New panel at end"));
    }

    public JTabbedPane getObjectBrowser()
    {
        return objectBrowser;
    }

    public void paint()
    {
        repaint();
    }

    public void paintTab(int panelNumber)
    {
        ((ObjectBrowserComponent)objectBrowser.getComponent(panelNumber)).repaint();
    }
}
