package objectbrowser;

import main.Editor;
import datastructures.Location;
import java.awt.*;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.geom.*;

//an ObjectBrowserImage is just a BufferedImage with a name attached to it, used to displayed a name in the Object Browser
public class ObjectBrowserImage extends JComponent
{
    BufferedImage image;
    String imageName;
    int width;
    int height;
    Location location;
    Rectangle2D.Double hitBox;
    Editor e;

    public ObjectBrowserImage(BufferedImage img, String name, Editor ed)
    {
        e = ed;
        image = img;
        imageName = name;
    }

    public void set(Location loc, int w, int h)
    {
        location = loc;
        width = w;
        height = h;
        setHitBox();
    }
    
    @Override
    public int getX()
    {
        return location.getX();
    }
    
    @Override
    public int getY()
    {
        return location.getY();
    }

    public Rectangle2D.Double getHitBox()
    {
        return hitBox;
    }

    public void setHitBox()
    {
        hitBox = new Rectangle2D.Double(location.getX(), location.getY(), width, height);
    }

    public BufferedImage getImage()
    {
        return image;
    }

    @Override
    public String getName()
    {
        return imageName;
    }

    @Override
    public void paint(Graphics g)
    {
        super.paint(g);
    }
}
