package tools;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class QuickFrame extends JFrame
{
    String label;

    public QuickFrame(String qTitle, String qLabel)
    {
        super(qTitle);
        label = qLabel;

        setup();
    }

    public void setup()
    {
        getContentPane().add(new JLabel(label), BorderLayout.CENTER);
        pack();
        setVisible(true);
        setLocationRelativeTo(null);
    }
}
