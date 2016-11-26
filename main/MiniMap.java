package main;

import datastructures.BackgroundEntity;

import java.awt.Graphics2D;
import java.util.LinkedList;
import java.util.Map;

public class MiniMap
{
    int overlayHeight;
    int overlayWidth;
    int overlayX;
    int overlayY;

    public MiniMap(int x, int y, int width, int height)
    {
        overlayHeight = height;
        overlayWidth = width;
        overlayX = x;
        overlayY = y;
    }

    //This method will set the size of the rectangle within the miniMap that shows what the user is currently viewing on their screen
    //With the editor, this can change depending on currentZoomAmount
    public void setViewArea()
    {

    }

    //This method is sent the graphics to draw on, along with a list of the objects (images) to draw,
    //and a map of every image in the database (to get the actual images from)
    //This will probably be changed, having the map being submitted earlier in the program with another method?
    public void update(Graphics2D g2, LinkedList objectList, Map objectMap)
    {
        g2.drawRect(overlayX, overlayY, overlayWidth, overlayHeight);
    }
}