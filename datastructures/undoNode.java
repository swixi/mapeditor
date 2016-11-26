package datastructures;

import javax.swing.*;

public class undoNode
{
    public static final int ADD  = 0;
    public static final int DELETE  = 1;
    public static final int MOVE  = 2;

    private MapEntity mapObject;
    private Location location;
    private int type;
    private int index;
    private MapEntity newMapObject;

    /*
     * if the action is an add or delete, there is no old and new x, just the old x, so use Location class for the new x and y.
     * if the action is a move, there is an old x and y and a new x and y. use the DynamicLocation class.
     *
     * when checking, the code (static final) should tell what it is, but use if(instanceof DynamicLocation) for move, and instanceof Location for the others, if needed.
     *
     * */

    public undoNode(MapEntity mO, Location loc, int code, int objectIndex)
    {
        if((loc instanceof DynamicLocation) && !(code == MOVE))
            JOptionPane.showMessageDialog(null, "Wrong format for undoNode");

        mapObject = mO;
        location = loc;
        type = code;
        index = objectIndex;
    }

    public undoNode(MapEntity mO, MapEntity nMO, Location loc, int code, int objectIndex)
    {
        if((loc instanceof DynamicLocation) && !(code == MOVE))
            JOptionPane.showMessageDialog(null, "Wrong format for undoNode");

        mapObject = mO;
        newMapObject = nMO;
        location = loc;
        type = code;
        index = objectIndex;
    }

    public int getType()
    {
        return type;
    }

    public String getTypeString()
    {
        if(type == 0)
            return "ADD";
        else if(type == 1)
            return "DELETE";
        else if(type == 2)
            return "MOVE";
        else
            return null;
    }

    public MapEntity getObject()
    {
        return mapObject;
    }

    public MapEntity getNewObject()
    {
        return newMapObject;
    }

    public Location getLocation()
    {
        return location;
    }

    public int getIndex()
    {
        return index;
    }
}
