package datastructures;

import java.util.*;

//If 2 objects are at the same X and Y location, it should run through the LL at that location and find the one with the greatest height/width (depending on what
//you're looking for)
public class LocationDatabase
{
    TreeMap<Integer, TreeMap<Integer, LinkedList<String>>> xMap;
    TreeMap<Integer, LinkedList<String>> yMap;
    LinkedList<String> nameList;
    HashMap<String, BackgroundEntity> imageMap;

    public static void main(String[] args)
    {
        LocationDatabase base = new LocationDatabase();

        //image name, x, y, width, height
        base.add(new BackgroundEntity("Image 1", 0, 0, 50, 50));
        base.add(new BackgroundEntity("Image 2", 0, -200, 50, 50));
        base.add(new BackgroundEntity("Image 3", -300, 0, 50, 50));
        base.add(new BackgroundEntity("Image 4", 300, 200, 50, 50));
        base.add(new BackgroundEntity("Image 5", 0, -100, 50, 50));

        base.getMap();
    }

    public LocationDatabase()
    {
        //define properties of the primary data structure, xMap
        xMap = new TreeMap<Integer, TreeMap<Integer, LinkedList<String>>>();

        imageMap = new HashMap<String, BackgroundEntity>();
    }

    public void add(BackgroundEntity mo)
    {
        //gather attributes of BackgroundEntity
        Integer x = mo.getX();
        Integer y = mo.getY();
        String imageName = mo.getImageName();

        imageMap.put(imageName, mo);

        if(xMap.containsKey(x))
        {
            yMap = (TreeMap)xMap.get(x);
            if(yMap.containsKey(y))
            {
                nameList = (LinkedList)yMap.get(y);
                nameList.add(imageName);
            }
            else
            {
                nameList = new LinkedList<String>();
                nameList.add(imageName);
                yMap.put(y, nameList);
            }
        }
        else
        {
            yMap = new TreeMap<Integer, LinkedList<String>>();
            nameList = new LinkedList<String>();
            nameList.add(imageName);

            yMap.put(y, nameList);
            xMap.put(x, yMap);
        }
    }

    public int getHorizontalDistance()
    {
        int rightmostX = xMap.lastKey();
        int leftmostX = xMap.firstKey();

        //must find out the farthest right object by going through all y values related to the x (and all linkedlists therein) and find the object with
        //the greatest width
        //if()

        //return Math.abs(rightmost - );
        return 0;
    }

    private TreeMap getMap()
    {
        return xMap;
    }
}