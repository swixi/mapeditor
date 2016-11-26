package tools;

import java.awt.*;
import javax.swing.*;

public class Logger extends JPanel
{
    JTextArea textArea;
    JFrame frame;
    String name;

    public Logger(String frameName, int rows, int columns)
    {
        super(new GridBagLayout());

        name = frameName;

        textArea = new JTextArea(rows, columns);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        
        JScrollPane scrollPane = new JScrollPane(textArea);

        //add components to this panel
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;

        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        add(scrollPane, c);
    }

    //Adds a line to the log's output
    public void addLine(String message)
    {
        textArea.append(message + "\n");
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }

    //Puts the log in the center of the screen
    public void setToCenter()
    {
        frame.setLocationRelativeTo(null);
    }

    //Updates the log's title
    public void updateTitle(String name)
    {
        frame.setTitle(name);
    }

    //Returns the frame's title
    public String getTitle()
    {
        return frame.getTitle();
    }

    //Creates frame, adds the logger into it, and shows it
    public void showLog()
    {
        frame = new JFrame(name);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(this);
        frame.pack();
        frame.setVisible(true);
        frame.setResizable(false);
    }

    //Kills the log, but the log can be brought back by simply executing showLog()
    public void disposeLog()
    {
        frame.dispose();
    }
}