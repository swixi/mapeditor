package objectbrowser;

import datastructures.Location;
import datastructures.MapEntity;
import datastructures.BackgroundEntity;
import datastructures.CollidableEntity;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class ObjectBrowserComponent extends JScrollPane implements MouseInputListener
{
    ObjectBrowser browser;
    LinkedList<ObjectBrowserImage> images;
    final int ICON_SIDE = 100;
    final int X_PADDING = 12;
    final int Y_PADDING = 25;
    final int MAX_IMAGES = 7;
    final int FONT_SIZE = 12;
    private int tabNumber;

    public ObjectBrowserComponent(ObjectBrowser browser, int num)
    {
        tabNumber = num;
        this.browser = browser;
        images = new LinkedList<ObjectBrowserImage>();
        setBackground(Color.WHITE);
        //setForeground(Color.WHITE);
        addMouseListener(this);
        addMouseMotionListener(this);
    }
    
    @Override
    @SuppressWarnings("empty-statement")
    public void paint(Graphics g)
    {
        //Graphics g2 = g;
        Graphics2D g2 = (Graphics2D)g;
        g2.setFont(new Font(Font.DIALOG, Font.BOLD, FONT_SIZE));
        int imagesInRow = 0;
        int totalImages = 0;

        g2.drawString("Dimensions are listed as [WIDTH x HEIGHT].", (Y_PADDING/2), X_PADDING);
        
        try
        {
            for(ObjectBrowserImage tempImage : images)
            {
                int row = totalImages/MAX_IMAGES + 1;

                if(imagesInRow % MAX_IMAGES == 0)
                    imagesInRow = 0;

                int x = (imagesInRow * ICON_SIDE) + (X_PADDING * (imagesInRow + 1));
                int y = (row - 1) * ICON_SIDE + (Y_PADDING * row);
                tempImage.set(new Location(x,y), ICON_SIDE, ICON_SIDE);

                //long time1 = System.nanoTime();
                
                if(tempImage.getImage().getWidth() == 100 && tempImage.getImage().getHeight() == 100)
                {
                    //g2.drawImage
                    while(!g2.drawImage(tempImage.getImage(), x, y, null));
                }
                else if(tempImage.getImage().getWidth() <= tempImage.getImage().getHeight())
                {
                    double scale = ((double)tempImage.getImage().getHeight())/((double)ICON_SIDE);
                    while(!g2.drawImage(tempImage.getImage(), x, y, (int)(tempImage.getImage().getWidth()/scale), ICON_SIDE, null));
                }
                else
                {
                    double scale = ((double)tempImage.getImage().getWidth())/((double)ICON_SIDE);
                    while(!g2.drawImage(tempImage.getImage(), x, y, ICON_SIDE, (int)(tempImage.getImage().getHeight()/scale), null));
                }
                
                //long time2 = System.nanoTime();
                //System.err.println("Time drawing image " + totalImages + ": " + (time2-time1));

                //draw image name, dimensions, and box around it
                g2.drawString(tempImage.getName(), x, y + ICON_SIDE + FONT_SIZE);
                g2.drawString(tempImage.getImage().getWidth() + " x " + tempImage.getImage().getHeight(), x, y + ICON_SIDE + (FONT_SIZE*2));
                g2.drawRect(x, y, 100, 100);
                
                imagesInRow++;
                totalImages++;
            }    
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public void addImage(ObjectBrowserImage image)
    {
        images.add(image);
    }

    //Returns the image in the browser that is at the clicked mouseX and mouseY
    public ObjectBrowserImage getIcon(int mouseX, int mouseY)
    {
        Iterator iter = images.iterator();
        while(iter.hasNext())
        {
            ObjectBrowserImage temp = (ObjectBrowserImage)iter.next();
            if(temp.getHitBox().contains(mouseX, mouseY))
                return temp;
        }
        return null;
    }

    public void mouseClicked(MouseEvent e)
    {
        //this.e.print("Woop!");
        ObjectBrowserImage image = getIcon(e.getX(), e.getY());
        if(image != null)
        {
            browser.e.minimizeBrowser();
            String tabName = browser.getObjectBrowser().getTitleAt(tabNumber);
            MapEntity entity = null;
            if(tabName.equals("Background"))
                entity = new BackgroundEntity(image.getName(), image.getX(), image.getY(), image.getImage().getWidth(), image.getImage().getHeight());
            else if(tabName.equals("Collidable"))
                entity = new CollidableEntity(image.getName(), image.getX(), image.getY(), image.getImage().getWidth(), image.getImage().getHeight());

            browser.e.setOnCursor(entity);
        }
    }

    public void mousePressed(MouseEvent e) {
        
    }

    public void mouseReleased(MouseEvent e) {
        
    }

    public void mouseEntered(MouseEvent e) {
        
    }

    public void mouseExited(MouseEvent e) {
        
    }

    public void mouseDragged(MouseEvent e) {

    }

    public void mouseMoved(MouseEvent e) {
        //this.e.d.mouseMoved(e);
    }
}
